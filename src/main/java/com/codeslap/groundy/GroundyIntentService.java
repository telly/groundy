/*
 * Copyright 2012 CodeSlap
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

package com.codeslap.groundy;

import android.app.Service;
import android.content.Intent;
import android.os.*;

import java.util.ArrayList;
import java.util.List;

abstract class GroundyIntentService extends Service {
    static final String EXTRA_ASYNC = "async";
    private volatile List<Looper> mAsyncLoopers;
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private String mName;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent)msg.obj);
            stopSelf(msg.arg1);
        }
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public GroundyIntentService(String name) {
        super();
        mName = name;
        mAsyncLoopers = new ArrayList<Looper>();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        ServiceHandler serviceHandler;
        if (intent.getBooleanExtra(EXTRA_ASYNC, false)) {
            HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
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
        serviceHandler.sendMessage(msg);
    }

    @Override
    public void onDestroy() {
        mServiceLooper.quit();
        for (Looper asyncLooper : mAsyncLoopers) {
            asyncLooper.quit();
        }
    }

    /**
     * Unless you provide binding for your service, you don't need to implement this
     * method, because the default implementation returns null.
     * @see android.app.Service#onBind
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
    protected abstract void onHandleIntent(Intent intent);
}
