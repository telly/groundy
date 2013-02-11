/*
 * Copyright 2013 CodeSlap
 *
 *   Authors: Cristian C. <cristian@elhacker.net>
 *            Evelio T.   <eveliotc@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeslap.groundy;

import android.app.Service;
import android.content.Intent;
import android.os.*;

import java.util.*;

public final class GroundyService extends Service {

    static final String ACTION_QUEUE = "com.codeslap.groundy.action.QUEUE";
    static final String ACTION_EXECUTE = "com.codeslap.groundy.action.EXECUTE";
    static final String ACTION_CANCEL_ALL = "com.codeslap.groundy.action.CANCEL_ALL";
    static final String ACTION_CANCEL_TASKS = "com.codeslap.groundy.action.CANCEL_TASKS";
    static final String EXTRA_GROUP_ID = "com.codeslap.groundy.extra.GROUP_ID";
    static final String EXTRA_CANCEL_REASON = "com.codeslap.groundy.extra.CANCEL_REASON";
    private static final String TAG = GroundyService.class.getSimpleName();
    public static final int DEFAULT_GROUP_ID = 0;
    private final WakeLockHelper mWakeLockHelper;
    private Set<GroundyTask> mGroundyTasks = Collections.synchronizedSet(new HashSet<GroundyTask>());
    private volatile List<Looper> mAsyncLoopers;
    private volatile Looper mGroundyLooper;
    private volatile GroundyHandler mGroundyHandler;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public GroundyService() {
        mWakeLockHelper = new WakeLockHelper(this);
        mAsyncLoopers = new ArrayList<Looper>();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("SyncGroundyService");
        thread.start();

        mGroundyLooper = thread.getLooper();
        mGroundyHandler = new GroundyHandler(mGroundyLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand(intent, startId);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGroundyLooper.quit();
        internalQuit(GroundyTask.SERVICE_DESTROYED);
    }

    /**
     * Unless you provide binding for your service, you don't need to implement this
     * method, because the default implementation returns null.
     *
     * @see android.app.Service#onBind
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleCommand(Intent intent, int startId) {
        if (intent == null) {
            return;
        }

        if (ACTION_CANCEL_ALL.equals(intent.getAction())) {
            L.e(TAG, "Aborting all tasks");
            mGroundyHandler.removeMessages(DEFAULT_GROUP_ID);
            internalQuit(GroundyTask.CANCEL_ALL);
            return;
        }

        if (ACTION_CANCEL_TASKS.equals(intent.getAction())) {
            cancelTasks(intent);
            return;
        }

        GroundyHandler groundyHandler;
        if (ACTION_EXECUTE.equals(intent.getAction())) {
            HandlerThread thread = new HandlerThread("AsyncGroundyService");
            thread.start();
            Looper looper = thread.getLooper();
            groundyHandler = new GroundyHandler(looper);
            mAsyncLoopers.add(looper);
        } else {
            groundyHandler = mGroundyHandler;
        }

        Message msg = groundyHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        msg.what = intent.getIntExtra(Groundy.KEY_GROUP_ID, DEFAULT_GROUP_ID);
        groundyHandler.sendMessage(msg);
    }

    private void cancelTasks(Intent intent) {
        int groupId = intent.getIntExtra(EXTRA_GROUP_ID, DEFAULT_GROUP_ID);
        if (groupId == DEFAULT_GROUP_ID) {
            throw new IllegalStateException("Cannot use 0 when cancelling tasks by group id");
        }

        synchronized (mGroundyTasks) {
            // prevent future tasks with this group id from executing
            mGroundyHandler.removeMessages(groupId);

            // stop current tasks
            List<GroundyTask> stoppedTasks = new ArrayList<GroundyTask>();
            for (GroundyTask groundyTask : mGroundyTasks) {
                if (groundyTask.getGroupId() == groupId) {
                    int reason = intent.getIntExtra(EXTRA_CANCEL_REASON, GroundyTask.CANCEL_BY_GROUP);
                    groundyTask.stopTask(reason);
                    stoppedTasks.add(groundyTask);
                }
            }
            mGroundyTasks.removeAll(stoppedTasks);
        }
    }

    private void internalQuit(int quittingReason) {
        synchronized (mAsyncLoopers) {
            for (Looper asyncLooper : mAsyncLoopers) {
                asyncLooper.quit();
            }
        }

        synchronized (mGroundyTasks) {
            for (GroundyTask groundyTask : mGroundyTasks) {
                groundyTask.stopTask(quittingReason);
            }
            mGroundyTasks.clear();
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
                onHandleIntent(intent, msg.what);
            }
            stopSelf(msg.arg1);
        }
    }

    /**
     * This method is invoked on the worker thread with a request to process.
     * Only one Intent is processed at a time, but the processing happens on a
     * worker thread that runs independently from other application logic.
     * So, if this code takes a long time, it will hold up other requests to
     * the same IntentService, but it will not hold up anything else.
     * When all requests have been handled, the IntentService stops itself,
     * so you should not call {@link #stopSelf}.
     *
     * @param intent  The value passed to {@link
     *                android.content.Context#startService(android.content.Intent)}.
     * @param groupId group id identifying the kind of task
     */
    private void onHandleIntent(Intent intent, int groupId) {
        Bundle extras = intent.getExtras();
        extras = (extras == null) ? Bundle.EMPTY : extras;

        ResultReceiver receiver = (ResultReceiver) extras.get(Groundy.KEY_RECEIVER);
        if (receiver != null) {
            receiver.send(Groundy.STATUS_RUNNING, Bundle.EMPTY);
        }

        Class<?> taskName = (Class<?>) extras.getSerializable(Groundy.KEY_TASK);
        //noinspection unchecked
        GroundyTask groundyTask = GroundyTaskFactory.get((Class<? extends GroundyTask>) taskName, this);
        if (groundyTask == null) {
            L.e(TAG, "Groundy task no provided");
            return;
        }

        L.d(TAG, "Executing task: " + groundyTask);
        groundyTask.setReceiver(receiver);
        groundyTask.setGroupId(groupId);
        groundyTask.addParameters(extras.getBundle(Groundy.KEY_PARAMETERS));
        boolean requiresWifi = groundyTask.keepWifiOn();
        if (requiresWifi) {
            mWakeLockHelper.acquire();
        }
        mGroundyTasks.add(groundyTask);
        groundyTask.execute();
        mGroundyTasks.remove(groundyTask);
        if (requiresWifi) {
            mWakeLockHelper.release();
        }

        //Lets try to send back the response
        if (receiver != null) {
            Bundle resultData = groundyTask.getResultData();
            receiver.send(groundyTask.getResultCode(), resultData);
        }
    }
}
