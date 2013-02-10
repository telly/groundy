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
    static final String ACTION_CANCEL_ALL = "com.codeslap.groundy.action.ACTION_CANCEL_ALL";
    static final String ACTION_CANCEL_TASK = "com.codeslap.groundy.action.ACTION_CANCEL_TASK";
    static final String EXTRA_TASK = "com.codeslap.groundy.extra.TASK";
    static final String EXTRA_CANCEL_REASON = "com.codeslap.groundy.extra.CANCEL_REASON";
    private static final String TAG = GroundyService.class.getSimpleName();
    private final WakeLockHelper mWakeLockHelper;
    private Set<GroundyTask> mGroundyTasks = Collections.synchronizedSet(new HashSet<GroundyTask>());
    private volatile List<Looper> mAsyncLoopers;
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;

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

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
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
        mServiceLooper.quit();
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
            mServiceHandler.removeMessages(0);
            internalQuit(GroundyTask.CANCEL_ALL);
            return;
        }

        if (ACTION_CANCEL_TASK.equals(intent.getAction())) {
            cancelTask(intent);
            return;
        }

        ServiceHandler serviceHandler;
        if (ACTION_EXECUTE.equals(intent.getAction())) {
            HandlerThread thread = new HandlerThread("AsyncGroundyService");
            thread.start();
            Looper looper = thread.getLooper();
            serviceHandler = new ServiceHandler(looper);
            mAsyncLoopers.add(looper);
        } else {
            serviceHandler = mServiceHandler;
        }

        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        msg.what = 0;
        serviceHandler.sendMessage(msg);
    }

    private void cancelTask(Intent intent) {
        //noinspection unchecked
        Class<? extends GroundyTask> task = (Class<? extends GroundyTask>) intent.getSerializableExtra(EXTRA_TASK);
        L.e(TAG, "Aborting task " + task);
        synchronized (mGroundyTasks) {
            for (GroundyTask groundyTask : mGroundyTasks) {
                if (groundyTask.getClass() == task) {
                    int reason = intent.getIntExtra(EXTRA_CANCEL_REASON, GroundyTask.CANCEL_INDIVIDUAL);
                    groundyTask.stopTask(reason);
                }
            }
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

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Intent intent = (Intent) msg.obj;
            if (intent != null) {
                onHandleIntent(intent);
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
     * @param intent The value passed to {@link
     *               android.content.Context#startService(Intent)}.
     */
    private void onHandleIntent(Intent intent) {
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
