/*
 * Copyright 2013 Telly Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.telly.groundy;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GroundyService extends Service {

  static final String ACTION_QUEUE = "com.telly.groundy.action.QUEUE";
  static final String ACTION_EXECUTE = "com.telly.groundy.action.EXECUTE";

  private static final String TAG = GroundyService.class.getSimpleName();

  public static final int DEFAULT_GROUP_ID = 0;
  /**
   * To be returned by cancelTaskById. It means the task was not cancelled at all. It could happen for two reasons:
   * the task had already been completed; or the task didn't even existed.
   */
  public static final int COULD_NOT_CANCEL = 0;
  /**
   * To be returned by cancelTaskById. It means the task was already running and {@link GroundyTask#stopTask(int)}
   * was called on it. It depends on the task implementation to react to in such cases; thus, this does not
   * mean the task was completely stopped.
   */
  public static final int INTERRUPTED = 1;
  /**
   * To be returned by cancelTaskById. It means the task was not even executed, but it was queued.
   */
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
  private boolean mRunning;
  private AtomicInteger mLastStartId = new AtomicInteger();

  // this help us keep track of the tasks that are scheduled to be executed
  private volatile SortedMap<Long, TaskInfo> mTasksInfoSet;
  private volatile SortedMap<String, Set<ResultReceiver>> mAttachedReceivers;

  public GroundyService() {
    mWakeLockHelper = new WakeLockHelper(this);
    mAttachedReceivers = Collections.synchronizedSortedMap(new TreeMap<String, Set<ResultReceiver>>());
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
      mRunning = false;
      return mStartBehavior;
    }

    final String action = intent.getAction();
    if (ACTION_EXECUTE.equals(action)) {
      if (mMode == GroundyMode.QUEUE) {
        // make sure we don't allow to asynchronously execute tasks while we are not in queue mode
        throw new UnsupportedOperationException(
          "Current mode is 'queue'. You cannot use .execute() while" + " in this mode. You must enable 'async' mode by adding metadata to the manifest.");
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

    mRunning = true;
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
    long taskId = intent.getLongExtra(Groundy.KEY_TASK_ID, 0);
    if (taskId == 0) {
      throw new RuntimeException("Task id cannot be 0. What kind of sorcery is this?");
    }

    mTasksInfoSet.put(taskId, new TaskInfo(startId, groupId));
    if (!groundyHandler.sendMessage(msg)) {
      mTasksInfoSet.remove(taskId);
    }
  }

  private void attachReceiver(String token, ResultReceiver resultReceiver) {
    if (!mRunning) {
      return;
    }
    synchronized (mTasksInfoSet) {
      for (Map.Entry<Long, TaskInfo> taskInfoEntry : mTasksInfoSet.entrySet()) {
        TaskInfo taskInfo = taskInfoEntry.getValue();
        GroundyTask task = taskInfo.task;
        if (task != null && token.equals(task.getToken())) {
          task.addReceiver(resultReceiver);
        }
      }
    }

    Set<ResultReceiver> resultReceivers;
    if (mAttachedReceivers.containsKey(token)) {
      resultReceivers = mAttachedReceivers.get(token);
    } else {
      resultReceivers = new HashSet<ResultReceiver>();
      mAttachedReceivers.put(token, resultReceivers);
    }

    resultReceivers.add(resultReceiver);
  }

  private void detachReceiver(String token, ResultReceiver resultReceiver) {
    if (!mRunning) {
      return;
    }
    for (Map.Entry<Long, TaskInfo> taskInfoEntry : mTasksInfoSet.entrySet()) {
      TaskInfo taskInfo = taskInfoEntry.getValue();
      GroundyTask task = taskInfo.task;
      if (task != null) {
        if (token.equals(task.getToken())) {
          task.removeReceiver(resultReceiver);
        }
      }
    }

    if (mAttachedReceivers.containsKey(token)) {
      Set<ResultReceiver> resultReceivers = mAttachedReceivers.get(token);
      resultReceivers.remove(resultReceiver);
    }
  }

  private void cancelAllTasks() {
    L.e(TAG, "Cancelling all tasks");
    mGroundyHandler.removeMessages(DEFAULT_GROUP_ID);
    internalQuit(GroundyTask.CANCEL_ALL);
    stopSelf();
  }

  /**
   * @param id     task to cancel
   * @param reason the reason to cancel this task. can be anything but 0
   * @return either {@link GroundyService#COULD_NOT_CANCEL}, {@link GroundyService#INTERRUPTED}
   *         and {@link GroundyService#NOT_EXECUTED}
   */
  private int cancelTaskById(long id, int reason) {
    if (id == 0) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if (reason == 0) {
      throw new IllegalArgumentException("reason cannot be zero");
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

  /**
   * @param groupId group id identifying the kind of task
   * @param reason  reason to cancel this group
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
      L.d(TAG,
        "Cancelling groups of tasks is not secure when using force_queue_completion. If your service gets killed unpredictable behavior can happen.");
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
          if (groundyTask == null) { // task didn't even run
            notExecutedTasks.add(taskId);
          } else { // task was already created and executed
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
    private final Set<Long> interruptedTasks;
    private final Set<Long> notExecutedTasks;

    public CancelGroupResponse(Set<Long> interruptedTasks, Set<Long> notExecutedTasks) {
      this.interruptedTasks = interruptedTasks;
      this.notExecutedTasks = notExecutedTasks;
    }

    public Set<Long> getInterruptedTasks() {
      return interruptedTasks;
    }

    public Set<Long> getNotExecutedTasks() {
      return notExecutedTasks;
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
   * processed at a time, but the processing happens on a worker thread that runs independently from
   * other application logic. So, if this code takes a long time, it will hold up other requests to
   * the same IntentService, but it will not hold up anything else.
   *
   * @param intent     The value passed to {@link android.content.Context#startService(android.content.Intent)}.
   * @param groupId    group id identifying the kind of task
   * @param startId    the service start id given when the task was schedule
   * @param redelivery true if this intent was redelivered by the system
   */
  private void onHandleIntent(Intent intent, int groupId, int startId, boolean redelivery) {
    GroundyTask groundyTask = buildGroundyTask(intent, groupId, startId, redelivery);
    if (groundyTask == null) return;
    executeGroundyTask(groundyTask);
  }

  private GroundyTask buildGroundyTask(Intent intent, int groupId, int startId, boolean redelivery) {
    Bundle extras = intent.getExtras();
    extras = (extras == null) ? new Bundle() : extras;

    Class<?> taskName = (Class<?>) extras.getSerializable(Groundy.KEY_TASK);
    //noinspection unchecked
    GroundyTask groundyTask = GroundyTaskFactory.get((Class<? extends GroundyTask>) taskName, this);
    if (groundyTask == null) {
      L.e(TAG, "Groundy task no provided");
      return null;
    }
    final long taskId = extras.getLong(Groundy.KEY_TASK_ID);
    groundyTask.setId(taskId);

    // set up the result receiver(s)
    ResultReceiver receiver = (ResultReceiver) extras.get(Groundy.KEY_RECEIVER);
    if (receiver != null) {
      groundyTask.addReceiver(receiver);
    }
    String token = intent.getStringExtra(Groundy.KEY_TOKEN);
    if (token == null) {
      token = "";
    }
    if (mAttachedReceivers.containsKey(token)) {
      for (ResultReceiver resultReceiver : mAttachedReceivers.get(token)) {
        groundyTask.addReceiver(resultReceiver);
      }
    }
    groundyTask.setToken(token);
    groundyTask.send(Groundy.STATUS_RUNNING, new Bundle());

    groundyTask.setStartId(startId);
    groundyTask.setGroupId(groupId);
    groundyTask.setRedelivered(redelivery);
    groundyTask.addParameters(extras.getBundle(Groundy.KEY_PARAMETERS));
    return groundyTask;
  }

  private void executeGroundyTask(GroundyTask groundyTask) {
    boolean requiresWifi = groundyTask.keepWifiOn();
    if (requiresWifi) {
      mWakeLockHelper.acquire();
    }
    TaskInfo taskInfo = mTasksInfoSet.get(groundyTask.getId());
    if(taskInfo == null) {
      // this can be null if the task is cancelled before this
      return;
    }
    taskInfo.task = groundyTask;
    L.d(TAG, "Executing task: " + groundyTask);
    groundyTask.execute();
    taskInfo.task = null;
    if (requiresWifi) {
      mWakeLockHelper.release();
    }

    //Lets try to send back the response
    Bundle resultData = groundyTask.getResultData();
    if (groundyTask.isQuitting()) {
      resultData.putInt(Groundy.KEY_CANCEL_REASON, groundyTask.getQuittingReason());
    }
    groundyTask.send(groundyTask.getResultCode(), resultData);
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
        long taskId = intent.getLongExtra(Groundy.KEY_TASK_ID, 0);
        if (taskId == 0) {
          throw new RuntimeException("Task id cannot be 0. What kind of sorcery is this?");
        }

        if (mTasksInfoSet.containsKey(taskId)) {
          onHandleIntent(intent, msg.what, msg.arg1, msg.arg2 == START_FLAG_REDELIVERY);
          mTasksInfoSet.remove(taskId);
        } else {
          L.d(TAG, "Ignoring task since it was removed: " + taskId);
        }

        if (mMode == GroundyMode.QUEUE) {
          // when in queue mode, we must stop each intent received
          stopSelf(msg.arg1);
        }
      }

      if (mTasksInfoSet.isEmpty()) {
        // stop the service by calling stopSelf with the latest startId
        stopSelf(mLastStartId.get());
        mRunning = false;
      }
    }
  }

  final class GroundyServiceBinder extends Binder {
    void attachReceiver(String token, ResultReceiver resultReceiver) {
      GroundyService.this.attachReceiver(token, resultReceiver);
    }

    void detachReceiver(String token, ResultReceiver resultReceiver) {
      GroundyService.this.detachReceiver(token, resultReceiver);
    }

    void cancelAllTasks() {
      GroundyService.this.cancelAllTasks();
    }

    /**
     * @param groupId group id identifying the kind of task
     * @param reason  reason to cancel this group
     * @return number of cancelled tasks (before they were ran)
     */
    CancelGroupResponse cancelTasks(int groupId, int reason) {
      return GroundyService.this.cancelTasks(groupId, reason);
    }

    /**
     * @param id     task id
     * @param reason reason to cancel this group
     * @return either {@link GroundyService#COULD_NOT_CANCEL}, {@link GroundyService#INTERRUPTED}
     *         and {@link GroundyService#NOT_EXECUTED}
     */
    int cancelTaskById(long id, int reason) {
      return GroundyService.this.cancelTaskById(id, reason);
    }
  }

  private static class TaskInfo {
    final int serviceId;
    final int groupId;
    GroundyTask task;

    public TaskInfo(int startId, int groupId) {
      serviceId = startId;
      this.groupId = groupId;
    }
  }
}
