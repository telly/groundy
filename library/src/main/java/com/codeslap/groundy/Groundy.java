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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

public class Groundy {
    public static final String KEY_PARAMETERS = "com.codeslap.groundy.key.paramaters";
    public static final String KEY_ERROR = "com.codeslap.groundy.key.error";
    public static final String KEY_RECEIVER = "com.codeslap.groundy.key.receiver";
    public static final String KEY_PROGRESS = "com.codeslap.groundy.key.progress";
    public static final String KEY_TASK = "com.codeslap.groundy.key.task_NAME";
    public static final String KEY_TOKEN = "com.codeslap.groundy.key.TOKEN";

    public static final int STATUS_FINISHED = 200;
    public static final int STATUS_ERROR = 232;
    public static final int STATUS_RUNNING = 224;
    public static final int STATUS_PROGRESS = 225;

    private final Context mContext;
    private final Class<? extends GroundyTask> mGroundyTask;
    private ResultReceiver mResultReceiver;
    private Bundle mExtras;
    private int mToken;

    private Groundy(Context context, Class<? extends GroundyTask> groundyTask) {
        mContext = context;
        mGroundyTask = groundyTask;
    }

    /**
     * Cancels all tasks of the specified type
     *
     * @param context   used to interact with the service
     * @param taskClass the class type to cancel
     * @param reason    the reason for cancelling the tasks
     */
    public static void cancelTasks(Context context, Class<? extends GroundyTask> taskClass, int reason) {
        Intent intent = new Intent(context, GroundyService.class);
        intent.setAction(GroundyService.ACTION_CANCEL_TASK);
        intent.putExtra(GroundyService.EXTRA_TASK, taskClass);
        intent.putExtra(GroundyService.EXTRA_CANCEL_REASON, reason);
        context.startService(intent);
    }

    public static void setLogEnabled(boolean enabled) {
        L.logEnabled = enabled;
    }

    private void startApiService(boolean async) {
        Intent intent = new Intent(mContext, GroundyService.class);
        intent.setAction(async ? GroundyService.ACTION_EXECUTE : GroundyService.ACTION_QUEUE);
        if (mExtras != null) {
            intent.putExtra(KEY_PARAMETERS, mExtras);
        }
        intent.putExtra(KEY_RECEIVER, mResultReceiver);
        intent.putExtra(KEY_TASK, mGroundyTask);
        intent.putExtra(KEY_TOKEN, mToken);
        mContext.startService(intent);
    }

    public static Groundy create(Context context, Class<? extends GroundyTask> groundyTask) {
        return new Groundy(context, groundyTask);
    }

    public void queue() {
        boolean async = false;
        startApiService(async);
    }

    public void execute() {
        boolean async = true;
        startApiService(async);
    }

    public Groundy setExtras(Bundle bundle) {
        mExtras = bundle;
        return this;
    }

    public Groundy setReceiver(ResultReceiver resultReceiver) {
        mResultReceiver = resultReceiver;
        return this;
    }

    public Groundy setToken(int token) {
        mToken = token;
        return this;
    }

    /**
     * Cancel all tasks: the ones running and parallel and
     * future tasks.
     *
     * @param context used to interact with the service
     */
    public static void cancelAll(Context context) {
        Intent intent = new Intent(context, GroundyService.class);
        intent.setAction(GroundyService.ACTION_CANCEL_ALL);
        context.startService(intent);
    }

    /**
     * Cancels all tasks of the specified type. The tasks get cancelled with
     * the {@link GroundyTask#CANCEL_INDIVIDUAL} reason.
     *
     * @param context   used to interact with the service
     * @param taskClass the class type to cancel
     */
    public static void cancelTask(Context context, Class<? extends GroundyTask> taskClass) {
        cancelTasks(context, taskClass, GroundyTask.CANCEL_INDIVIDUAL);
    }
}
