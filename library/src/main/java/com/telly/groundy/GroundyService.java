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
import android.util.Log;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GroundyService extends Service {

  static final String ACTION_QUEUE = "com.telly.groundy.action.QUEUE";
  static final String ACTION_EXECUTE = "com.telly.groundy.action.EXECUTE";

  private static final String TAG = GroundyService.class.getSimpleName();

  public static final int DEFAULT_GROUP_ID = 0;
  public static final int COULD_NOT_CANCEL = 0;
  public static final int INTERRUPTED = 1;
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
  private volatile SortedMap<Long, TaskInfo> mPendingTasks;
  private final SortedMap<Long, GroundyTask> mRunningTasks;// TODO can mPendingTasks and mRunningTasks be the same?
  private volatile SortedMap<String, Set<ResultReceiver>> mAttachedReceivers;

  public GroundyService() {
    mWakeLockHelper = new WakeLockHelper(this);
    mAttachedReceivers = Collections.synchronizedSortedMap(new TreeMap<String, Set<ResultReceiver>>());
    mPendingTasks = Collections.synchronizedSortedMap(new TreeMap<Long, TaskInfo>());
    mRunningTasks = Collections.synchronizedSortedMap(new TreeMap<Long, GroundyTask>());
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

    mPendingTasks.put(taskId, new TaskInfo(startId, groupId));
    if (!groundyHandler.sendMessage(msg)) {
      mPendingTasks.remove(taskId);
    }
  }

  private void cancelAllTasks() {
    L.e(TAG, "Cancelling all tasks");
    mGroundyHandler.removeMessages(DEFAULT_GROUP_ID);
    // remove messages of other groups
    synchronized (mRunningTasks) {
      Set<Integer> alreadyStopped = new HashSet<Integer>();
      for (Map.Entry<Long, GroundyTask> groundyTaskEntry : mRunningTasks.entrySet()) {
        final int groupId = groundyTaskEntry.getValue().getGroupId();
        if (groupId == DEFAULT_GROUP_ID || alreadyStopped.contains(groupId)) {
          continue;
        }
        mGroundyHandler.removeMessages(groupId);
        alreadyStopped.add(groupId);
      }
    }
    internalQuit(GroundyTask.CANCEL_ALL);
    mPendingTasks.clear();
  }

  private int cancelTaskById(long id, int reason) {
    if (id == 0) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if (reason == 0) {
      throw new IllegalArgumentException("reason cannot be zero");
    }
    TaskInfo remove = mPendingTasks.remove(id);
    if (remove != null) {
      return NOT_EXECUTED;// not even run
    }

    GroundyTask removedGroundyTask = mRunningTasks.remove(id);
    if (removedGroundyTask != null) {
      removedGroundyTask.stopTask(reason);
      return INTERRUPTED;// interrupted
    }
    return COULD_NOT_CANCEL;// couldn't do anything
  }

  /**
   *
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
      Log.w(TAG,
        "Cancelling groups of tasks is not secure when using force_queue_completion. If your service gets killed unpredictable behavior can happen.");
    }

    // prevent current scheduled tasks with this group id from executing
    mGroundyHandler.removeMessages(groupId);

    Set<Long> interruptedTasks = new HashSet<Long>();
    synchronized (mRunningTasks) {
      if (!mRunningTasks.isEmpty()) {
        // stop current running tasks
        List<Long> tasksToStop = new ArrayList<Long>();
        Set<Long> tasksIds = mRunningTasks.keySet();
        for (Long tasksId : tasksIds) {
          GroundyTask groundyTask = mRunningTasks.get(tasksId);
          if (groundyTask.getGroupId() == groupId) {
            groundyTask.stopTask(reason);
            tasksToStop.add(groundyTask.getId());
            interruptedTasks.add(groundyTask.getId());
          }
        }
        tasksIds.removeAll(tasksToStop);
      }
    }

    Set<Long> notExecutedTasks = new HashSet<Long>();
    synchronized (mPendingTasks) { // remove future tasks
      if (mPendingTasks.size() == 0) {
        return null;
      }
      Set<Long> taskIdSet = mPendingTasks.keySet();
      for (Long taskId : taskIdSet) {
        TaskInfo taskInfo = mPendingTasks.get(taskId);
        if (taskInfo.groupId == groupId) {
          notExecutedTasks.add(taskId);
        }
      }
      taskIdSet.removeAll(notExecutedTasks);
    }

    notExecutedTasks.removeAll(interruptedTasks);
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

    synchronized (mRunningTasks) {
      for (Map.Entry<Long, GroundyTask> groundyTaskEntry : mRunningTasks.entrySet()) {
        groundyTaskEntry.getValue().stopTask(quittingReason);
      }
      mRunningTasks.clear();
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
    Bundle extras = intent.getExtras();
    extras = (extras == null) ? new Bundle() : extras;

    Class<?> taskName = (Class<?>) extras.getSerializable(Groundy.KEY_TASK);
    //noinspection unchecked
    GroundyTask groundyTask = GroundyTaskFactory.get((Class<? extends GroundyTask>) taskName, this);
    if (groundyTask == null) {
      L.e(TAG, "Groundy task no provided");
      return;
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
    boolean requiresWifi = groundyTask.keepWifiOn();
    if (requiresWifi) {
      mWakeLockHelper.acquire();
    }
    mRunningTasks.put(taskId, groundyTask);
    L.d(TAG, "Executing task: " + groundyTask);
    groundyTask.execute();
    mRunningTasks.remove(taskId);
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

        if(mPendingTasks.containsKey(taskId)) {
          mPendingTasks.remove(taskId);
          onHandleIntent(intent, msg.what, msg.arg1, msg.arg2 == START_FLAG_REDELIVERY);
        }

        if (mMode == GroundyMode.QUEUE) {
          // when in queue mode, we must stop each intent received
          stopSelf(msg.arg1);
        }
      }

      if (mPendingTasks.isEmpty()) {
        // stop the service by calling stopSelf with the latest startId
        stopSelf(mLastStartId.get());
        mRunning = false;
      }
    }
  }

  final class GroundyServiceBinder extends Binder {
    void attachReceiver(String token, ResultReceiver resultReceiver) {
      if (!mRunning) {
        return;
      }
      for (Map.Entry<Long, GroundyTask> runningTaskEntry : mRunningTasks.entrySet()) {
        GroundyTask groundyTask = runningTaskEntry.getValue();
        if (token.equals(groundyTask.getToken())) {
          groundyTask.addReceiver(resultReceiver);
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

    void detachReceiver(String token, ResultReceiver resultReceiver) {
      if (!mRunning) {
        return;
      }
      for (Map.Entry<Long, GroundyTask> groundyTaskEntry : mRunningTasks.entrySet()) {
        GroundyTask groundyTask = groundyTaskEntry.getValue();
        if (token.equals(groundyTask.getToken())) {
          groundyTask.removeReceiver(resultReceiver);
        }
      }

      if (mAttachedReceivers.containsKey(token)) {
        Set<ResultReceiver> resultReceivers = mAttachedReceivers.get(token);
        resultReceivers.remove(resultReceiver);
      }
    }

    void cancelAllTasks() {
      GroundyService.this.cancelAllTasks();
      stopSelf();
    }

    /**
     *
     * @param groupId group id identifying the kind of task
     * @param reason  reason to cancel this group
     * @return number of cancelled tasks (before they were ran)
     */
    CancelGroupResponse cancelTasks(int groupId, int reason) {
      return GroundyService.this.cancelTasks(groupId, reason);
    }

    /**
     *
     * @param id     task id
     * @param reason reason to cancel this group
     * @return number of cancelled tasks (before they were ran)
     */
    int cancelTaskById(long id, int reason) {
      return GroundyService.this.cancelTaskById(id, reason);
    }
  }

  private static class TaskInfo {
    final int serviceId;
    final int groupId;

    public TaskInfo(int startId, int groupId) {
      serviceId = startId;
      this.groupId = groupId;
    }
  }
}
