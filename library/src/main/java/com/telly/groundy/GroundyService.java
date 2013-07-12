/**
 * Copyright Telly, Inc. and other Groundy contributors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.telly.groundy;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ResultReceiver;
import com.telly.groundy.annotations.OnCancel;
import com.telly.groundy.annotations.OnFailure;
import com.telly.groundy.annotations.OnStart;
import com.telly.groundy.annotations.OnSuccess;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This service executes tasks dispatched to Groundy. By default, it creates a new {@link Thread}
 * per task and executes it sequentially. It can also process tasks in parallel but you most
 * explicitly declare your service using this meta-data tag:
 *
 * <pre> {@code
 * &lt;service android:name="com.groundy.example.AsyncGroundyService"&gt;
 *   &lt;meta-data android:name="groundy:mode" android:value="async" /&gt;
 * &lt;/service&gt;
 * }
 * </pre>
 */
public class GroundyService extends Service {

  static final String ACTION_QUEUE = "com.telly.groundy.action.QUEUE";
  static final String ACTION_EXECUTE = "com.telly.groundy.action.EXECUTE";

  private static final String TAG = GroundyService.class.getSimpleName();

  public static final int DEFAULT_GROUP_ID = 0;
  /**
   * To be returned by cancelTaskById. It means the value was not cancelled at all. It could happen
   * for two reasons: the value had already been completed; or the value didn't even existed.
   */
  public static final int COULD_NOT_CANCEL = 0;
  /**
   * To be returned by cancelTaskById. It means the value was already running and {@link
   * GroundyTask#stopTask(int)} was called on it. It depends on the value implementation to react
   * to
   * in such cases; thus, this does not mean the value was completely stopped.
   */
  public static final int INTERRUPTED = 1;
  /** To be returned by cancelTaskById. It means the value was not even executed, but it was queued. */
  public static final int NOT_EXECUTED = 2;

  private static enum GroundyMode {QUEUE, ASYNC}

  public static final String KEY_MODE = "groundy:mode";
  public static final String KEY_FORCE_QUEUE_COMPLETION = "groundy:force_queue_completion";
  private final GroundyServiceBinder mBinder = new GroundyServiceBinder();

  private volatile Looper mGroundyLooper;
  private volatile GroundyHandler mGroundyHandler;
  private volatile List<Looper> mAsyncLoopers;

  private GroundyMode mMode = GroundyMode.QUEUE;
  private int mStartBehavior = START_NOT_STICKY;
  private final WakeLockHelper mWakeLockHelper;
  private AtomicInteger mLastStartId = new AtomicInteger();

  // this help us keep track of the tasks that are scheduled to be executed
  private volatile SortedMap<Long, TaskInfo> mTasksInfoSet;

  public GroundyService() {
    mWakeLockHelper = new WakeLockHelper(this);
    mTasksInfoSet = Collections.synchronizedSortedMap(new TreeMap<Long, TaskInfo>());
  }

  @Override
  public void onCreate() {
    super.onCreate();
    updateModeFromMetadata();

    // initialize async loopers only if running in async mode
    if (mMode == GroundyMode.ASYNC) {
      mAsyncLoopers = new ArrayList<Looper>();
    }

    HandlerThread thread = new HandlerThread("SyncGroundyService");
    thread.start();

    mGroundyLooper = thread.getLooper();
    mGroundyHandler = new GroundyHandler(mGroundyLooper);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    mLastStartId.set(startId);

    if (intent == null) {
      // we should not have received a null intent... kill the service just in case
      stopSelf(startId);
      return mStartBehavior;
    }

    final String action = intent.getAction();
    if (ACTION_EXECUTE.equals(action)) {
      if (mMode == GroundyMode.QUEUE) {
        // make sure we don't allow to asynchronously execute tasks while we are not in queue mode
        throw new UnsupportedOperationException(
            "Current mode is 'queue'. You cannot use .executeUsing() while"
                + " in this mode. You must enable 'async' mode by adding metadata to the manifest.");
      }
      HandlerThread thread = new HandlerThread("AsyncGroundyService");
      thread.start();
      Looper looper = thread.getLooper();
      GroundyHandler groundyHandler = new GroundyHandler(looper);
      mAsyncLoopers.add(looper);
      scheduleTask(intent, startId, groundyHandler, flags);
    } else if (ACTION_QUEUE.equals(action)) {
      scheduleTask(intent, startId, mGroundyHandler, flags);
    } else {
      throw new UnsupportedOperationException("Wrong intent received: " + intent);
    }

    return mStartBehavior;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mGroundyLooper.quit();
    internalQuit(GroundyTask.SERVICE_DESTROYED);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  private void scheduleTask(Intent intent, int startId, GroundyHandler groundyHandler, int flags) {
    Message msg = groundyHandler.obtainMessage();
    msg.arg1 = startId;
    msg.arg2 = flags;
    msg.obj = intent;
    int groupId = intent.getIntExtra(Groundy.KEY_GROUP_ID, DEFAULT_GROUP_ID);
    msg.what = groupId;
    long taskId = intent.getLongExtra(Groundy.TASK_ID, 0);
    if (taskId == 0) {
      throw new RuntimeException("Task id cannot be 0. What kind of sorcery is this?");
    }

    mTasksInfoSet.put(taskId, new TaskInfo(startId, groupId));
    if (!groundyHandler.sendMessage(msg)) {
      mTasksInfoSet.remove(taskId);
    }
  }

  private void cancelAllTasks() {
    L.e(TAG, "Cancelling all tasks");
    mGroundyHandler.removeMessages(DEFAULT_GROUP_ID);
    internalQuit(GroundyTask.CANCEL_ALL);
    stopSelf();
  }

  /**
   * @param id value to cancel
   * @param reason the reason to cancel this value. can be anything but 0
   * @return either {@link GroundyService#COULD_NOT_CANCEL}, {@link GroundyService#INTERRUPTED} and
   *         {@link GroundyService#NOT_EXECUTED}
   */
  private int cancelTaskById(long id, int reason) {
    if (id == 0) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if (reason == Integer.MIN_VALUE) {
      throw new IllegalArgumentException("reason cannot be Integer.MIN_VALUE");
    }
    TaskInfo taskInfo = mTasksInfoSet.remove(id);
    if (taskInfo == null) {
      return COULD_NOT_CANCEL;
    }

    GroundyTask groundyTask = taskInfo.task;
    if (groundyTask == null) {
      return NOT_EXECUTED;
    }

    groundyTask.stopTask(reason);
    return INTERRUPTED;
  }

  private List<TaskHandler> attachCallbacks(Class<? extends GroundyTask> task,
      CallbacksReceiver resultReceiver) {
    List<TaskHandler> handlers = new ArrayList<TaskHandler>();
    for (TaskInfo taskInfo : mTasksInfoSet.values()) {
      if (taskInfo.task != null && taskInfo.task.getClass() == task) {
        taskInfo.task.appendReceiver(resultReceiver);

        AttachedTaskHandlerImpl taskHandler =
            new AttachedTaskHandlerImpl(taskInfo.task.getId(), GroundyService.this.getClass(),
                resultReceiver, task);
        handlers.add(taskHandler);
      }
    }
    return handlers;
  }

  /**
   * @param groupId group id identifying the kind of value
   * @param reason reason to cancel this group
   * @return number of tasks cancelled
   */
  private CancelGroupResponse cancelTasks(int groupId, int reason) {
    if (groupId == DEFAULT_GROUP_ID) {
      throw new IllegalStateException("Cannot use 0 when cancelling tasks by group id");
    }
    if (reason == 0) {
      throw new IllegalStateException("Reason cannot be 0");
    }

    if (mStartBehavior == START_REDELIVER_INTENT) {
      L.d(TAG, "Cancelling groups of tasks is not secure when using force_queue_completion."
          + "If your service gets killed unpredictable behavior can happen.");
    }

    // prevent current scheduled tasks with this group id from executing
    mGroundyHandler.removeMessages(groupId);

    Set<Long> notExecutedTasks = new HashSet<Long>();
    Set<Long> interruptedTasks = new HashSet<Long>();
    if (mTasksInfoSet.isEmpty()) {
      // there's nothing to do
      return new CancelGroupResponse(interruptedTasks, notExecutedTasks);
    }

    synchronized (mTasksInfoSet) {
      Set<Long> taskIdSet = mTasksInfoSet.keySet();
      for (Long taskId : taskIdSet) {
        TaskInfo taskInfo = mTasksInfoSet.get(taskId);
        if (taskInfo.groupId == groupId) {
          GroundyTask groundyTask = taskInfo.task;
          if (groundyTask == null) { // value didn't even run
            notExecutedTasks.add(taskId);
          } else { // value was already created and executed
            groundyTask.stopTask(reason);
            interruptedTasks.add(taskId);
          }
        }
      }
      taskIdSet.removeAll(notExecutedTasks);
      taskIdSet.removeAll(interruptedTasks);
      notExecutedTasks.removeAll(interruptedTasks);
    }
    return new CancelGroupResponse(interruptedTasks, notExecutedTasks);
  }

  public static class CancelGroupResponse {
    private final Set<Long> mInterruptedTasks;
    private final Set<Long> mNotExecutedTasks;

    public CancelGroupResponse(Set<Long> interruptedTasks, Set<Long> notExecutedTasks) {
      mInterruptedTasks = interruptedTasks;
      mNotExecutedTasks = notExecutedTasks;
    }

    public Set<Long> getInterruptedTasks() {
      return mInterruptedTasks;
    }

    public Set<Long> getNotExecutedTasks() {
      return mNotExecutedTasks;
    }
  }

  private void internalQuit(int quittingReason) {
    if (mAsyncLoopers != null) {
      synchronized (mAsyncLoopers) {
        for (Looper asyncLooper : mAsyncLoopers) {
          asyncLooper.quit();
        }
      }
    }

    synchronized (mTasksInfoSet) {
      Set<Integer> alreadyStopped = new HashSet<Integer>();
      for (Map.Entry<Long, TaskInfo> taskEntry : mTasksInfoSet.entrySet()) {
        TaskInfo taskInfo = taskEntry.getValue();
        GroundyTask task = taskInfo.task;
        if (task != null) {
          task.stopTask(quittingReason);
        }
        if (taskInfo.groupId != DEFAULT_GROUP_ID && !alreadyStopped.contains(taskInfo.groupId)) {
          mGroundyHandler.removeMessages(taskInfo.groupId);
          alreadyStopped.add(taskInfo.groupId);
        }
      }
      mTasksInfoSet.clear();
    }
  }

  /**
   * This method is invoked on the worker thread with a request to process. Only one Intent is
   * processed at a time, but the processing happens on a worker thread that runs independently
   * from
   * other application logic. So, if this code takes a long time, it will hold up other requests to
   * the same IntentService, but it will not hold up anything else.
   *
   * @param intent The value passed to {@link android.content.Context#startService(android.content.Intent)}.
   * @param groupId group id identifying the kind of value
   * @param startId the service start id given when the value was schedule
   * @param redelivery true if this intent was redelivered by the system
   */
  private void onHandleIntent(Intent intent, int groupId, int startId, boolean redelivery) {
    GroundyTask groundyTask = buildGroundyTask(intent, groupId, startId, redelivery);
    if (groundyTask == null) return;
    executeGroundyTask(groundyTask);
  }

  private GroundyTask buildGroundyTask(Intent intent, int groupId, int startId,
      boolean redelivery) {
    Bundle extras = intent.getExtras();
    extras = (extras == null) ? new Bundle() : extras;

    Class<?> taskName = (Class<?>) extras.getSerializable(Groundy.KEY_TASK);
    //noinspection unchecked
    GroundyTask groundyTask = GroundyTaskFactory.get((Class<? extends GroundyTask>) taskName, this);
    if (groundyTask == null) {
      L.e(TAG, "Groundy value no provided");
      return null;
    }
    final long taskId = extras.getLong(Groundy.TASK_ID);
    groundyTask.setId(taskId);

    // set up the result receiver(s)
    ResultReceiver receiver = (ResultReceiver) extras.get(Groundy.KEY_RECEIVER);
    if (receiver != null) {
      groundyTask.setReceiver(receiver);
    }
    groundyTask.send(OnStart.class, new Bundle());

    groundyTask.setStartId(startId);
    groundyTask.setGroupId(groupId);
    groundyTask.setRedelivered(redelivery);
    groundyTask.addArgs(extras.getBundle(Groundy.KEY_ARGUMENTS));
    if (Groundy.devMode) {
      Object[] rawElements = (Object[]) extras.getSerializable(Groundy.STACK_TRACE);
      if (rawElements != null) {
        StackTraceElement[] stackTrace = new StackTraceElement[rawElements.length];
        for (int i = 0; i < rawElements.length; i++) {
          stackTrace[i] = (StackTraceElement) rawElements[i];
        }
        groundyTask.setStackTrace(stackTrace);
      }
    }
    groundyTask.setIntent(intent);
    return groundyTask;
  }

  private void executeGroundyTask(GroundyTask groundyTask) {
    boolean requiresWifi = groundyTask.keepWifiOn();
    if (requiresWifi) {
      mWakeLockHelper.acquire();
    }
    TaskInfo taskInfo = mTasksInfoSet.get(groundyTask.getId());
    if (taskInfo == null) {
      // this can be null if the value is cancelled before this
      return;
    }

    taskInfo.task = groundyTask;
    L.d(TAG, "Executing value: " + groundyTask);
    TaskResult taskResult;

    try {
      taskResult = groundyTask.doInBackground();
    } catch (Exception e) {
      e.printStackTrace();

      taskResult = new Failed();
      taskResult.add(Groundy.CRASH_MESSAGE, String.valueOf(e.getMessage()));
    }

    if (taskResult == null) {
      throw new NullPointerException(
          "Task " + groundyTask + " returned null from the doInBackground method");
    }

    taskInfo.task = null;
    if (requiresWifi) {
      mWakeLockHelper.release();
    }

    //Lets try to send back the response
    Bundle resultData = taskResult.getResultData();
    resultData.putBundle(Groundy.ORIGINAL_PARAMS, groundyTask.getArgs());
    resultData.putSerializable(Groundy.TASK_IMPLEMENTATION, groundyTask.getClass());

    switch (taskResult.getType()) {
      case SUCCESS:
        groundyTask.send(OnSuccess.class, resultData);
        break;
      case FAIL:
        groundyTask.send(OnFailure.class, resultData);
        break;
      case CANCEL:
        resultData.putInt(Groundy.CANCEL_REASON, groundyTask.getQuittingReason());
        groundyTask.send(OnCancel.class, resultData);
        break;
    }
  }

  private void updateModeFromMetadata() {
    ServiceInfo info = null;
    try {
      PackageManager pm = getPackageManager();
      ComponentName component = new ComponentName(this, getClass());
      info = pm.getServiceInfo(component, PackageManager.GET_META_DATA);
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
    if (info == null || info.metaData == null) {
      return;
    }

    // update groundy mode
    if (info.metaData.containsKey(KEY_MODE)) {
      String modeData = info.metaData.getString(KEY_MODE);
      if (GroundyMode.ASYNC.toString().equalsIgnoreCase(modeData)) {
        mMode = GroundyMode.ASYNC;
      } else {
        mMode = GroundyMode.QUEUE;
      }
    }

    // update service behavior
    boolean forceQueueCompletion = info.metaData.getBoolean(KEY_FORCE_QUEUE_COMPLETION, false);
    if (forceQueueCompletion) {
      if (mMode == GroundyMode.ASYNC) {
        throw new UnsupportedOperationException(
            "force_queue_completion can only be used when in 'queue' mode");
      }
      mStartBehavior = START_REDELIVER_INTENT;
    } else {
      mStartBehavior = START_NOT_STICKY;
    }
  }

  private final class GroundyHandler extends Handler {
    public GroundyHandler(Looper looper) {
      super(looper);
    }

    @Override
    public void handleMessage(Message msg) {
      Intent intent = (Intent) msg.obj;
      if (intent != null) {
        long taskId = intent.getLongExtra(Groundy.TASK_ID, 0);
        if (taskId == 0) {
          throw new RuntimeException("Task id cannot be 0. What kind of sorcery is this?");
        }

        if (mTasksInfoSet.containsKey(taskId)) {
          onHandleIntent(intent, msg.what, msg.arg1, msg.arg2 == START_FLAG_REDELIVERY);
          mTasksInfoSet.remove(taskId);
        } else {
          L.d(TAG, "Ignoring value since it was removed: " + taskId);
        }

        if (mMode == GroundyMode.QUEUE) {
          // when in queue mode, we must stop each intent received
          stopSelf(msg.arg1);
        }
      }

      if (mTasksInfoSet.isEmpty()) {
        // stop the service by calling stopSelf with the latest startId
        stopSelf(mLastStartId.get());
      }
    }
  }

  final class GroundyServiceBinder extends Binder {
    void cancelAllTasks() {
      GroundyService.this.cancelAllTasks();
    }

    /**
     * @param groupId group id identifying the kind of value
     * @param reason reason to cancel this group
     * @return number of cancelled tasks (before they were ran)
     */
    CancelGroupResponse cancelTasks(int groupId, int reason) {
      return GroundyService.this.cancelTasks(groupId, reason);
    }

    /**
     * @param id value id
     * @param reason reason to cancel this group
     * @return either {@link GroundyService#COULD_NOT_CANCEL}, {@link GroundyService#INTERRUPTED}
     *         and {@link GroundyService#NOT_EXECUTED}
     */
    int cancelTaskById(long id, int reason) {
      return GroundyService.this.cancelTaskById(id, reason);
    }

    List<TaskHandler> attachCallbacks(Class<? extends GroundyTask> task,
        CallbacksReceiver receiver) {
      return GroundyService.this.attachCallbacks(task, receiver);
    }
  }

  private static class TaskInfo {
    final int serviceId;
    final int groupId;
    GroundyTask task;

    public TaskInfo(int startId, int group) {
      serviceId = startId;
      groupId = group;
    }
  }
}
