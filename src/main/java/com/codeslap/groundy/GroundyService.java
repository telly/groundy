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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

public class GroundyService extends GroundyIntentService {
    /**
     * Log tag
     */
    private static final String TAG = GroundyService.class.getSimpleName();

    private final WakeLockHelper mWakeLockHelper;

    /**
     * Default constructor creates a service named {@link GroundyService#TAG}
     */
    public GroundyService() {
        super(TAG);
        mWakeLockHelper = new WakeLockHelper(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            Bundle extras = intent.getExtras();
            extras = (extras == null) ? Bundle.EMPTY : extras;

            // Pessimistic by default
            int resultCode = Groundy.STATUS_ERROR;
            Bundle resultData = Bundle.EMPTY;

            ResultReceiver receiver = (ResultReceiver) extras.get(Groundy.KEY_RECEIVER);
            if (receiver != null) {
                receiver.send(Groundy.STATUS_RUNNING, Bundle.EMPTY);
            }

            //This should be be the most common action
            GroundyTask groundyTask;
            try {
                groundyTask = GroundyTaskFactory.get((Class<? extends GroundyTask>) Class.forName(action), this);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
            if (groundyTask != null) {
                L.d(TAG, "Executing task: " + groundyTask);
                groundyTask.setReceiver(receiver);
                groundyTask.setParameters(extras.getBundle(Groundy.KEY_PARAMETERS));
                boolean requiresWifi = groundyTask.keepWifiOn();
                if (requiresWifi) {
                    mWakeLockHelper.adquire();
                }
                groundyTask.execute();
                if (requiresWifi) {
                    mWakeLockHelper.release();
                }
                resultCode = groundyTask.getResultCode();
                resultData = groundyTask.getResultData();
            }

            //Lets try to send back the response
            if (receiver != null) {
                receiver.send(resultCode, resultData);
            }
        }
    }

    static class WakeLockHelper {
        private final Context mContext;

        WakeLockHelper(Context context) {
            mContext = context;
        }

        synchronized void adquire() {
            DeviceStatus.keepCpuAwake(mContext, true);
            if (DeviceStatus.isCurrentConnectionWifi(mContext)) {
                DeviceStatus.keepWiFiOn(mContext, true);
            }
        }

        synchronized void release() {
            DeviceStatus.keepWiFiOn(mContext, false);
            DeviceStatus.keepCpuAwake(mContext, false);
        }
    }
}
