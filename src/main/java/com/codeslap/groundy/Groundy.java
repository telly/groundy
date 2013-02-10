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
    public static final String KEY_TASK_NAME = "com.codeslap.groundy.key.task_NAME";

    public static final int STATUS_FINISHED = 200;
    public static final int STATUS_ERROR = 232;
    public static final int STATUS_RUNNING = 224;
    public static final int STATUS_PROGRESS = 225;

    /**
     * Queue a call task to be executed with no params nor result receiver
     *
     * @param context   needed to start the service
     * @param taskClass groundy task implementation
     */
    public static void queue(Context context, Class<? extends GroundyTask> taskClass) {
        queue(context, taskClass, (ResultReceiver) null);
    }

    /**
     * Queue a call task to be executed with no params
     *
     * @param context   needed to start the service
     * @param taskClass groundy task implementation
     * @param receiver  result receiver to report results back
     */
    public static void queue(Context context, Class<? extends GroundyTask> taskClass, ResultReceiver receiver) {
        queue(context, taskClass, receiver, null);
    }

    /**
     * Queue a call task to be executed with no result receiver
     *
     * @param context   needed to start the service
     * @param taskClass groundy task implementation
     * @param extras    the application code
     */
    public static void queue(Context context, Class<? extends GroundyTask> taskClass, Bundle extras) {
        queue(context, taskClass, null, extras);
    }

    /**
     * Queue a call task to be executed
     *
     * @param context   needed to start the service
     * @param taskClass groundy task implementation
     * @param receiver  result receiver to report results back
     * @param extras    the application code
     */
    public static void queue(Context context, Class<? extends GroundyTask> taskClass, ResultReceiver receiver,
                             Bundle extras) {
        startApiService(context, receiver, taskClass, extras, false);
    }

    /**
     * Execute this groundy task asynchronously with no params nor receiver
     *
     * @param context   needed to start the service
     * @param taskClass groundy task implementation
     */
    public static void execute(Context context, Class<? extends GroundyTask> taskClass) {
        execute(context, taskClass, (ResultReceiver) null);
    }

    /**
     * Execute this groundy task asynchronously with no params
     *
     * @param context   needed to start the service
     * @param taskClass groundy task implementation
     * @param receiver  result receiver to report results back
     */
    public static void execute(Context context, Class<? extends GroundyTask> taskClass, ResultReceiver receiver) {
        execute(context, taskClass, receiver, null);
    }

    /**
     * Execute this groundy task asynchronously with no result receiver
     *
     * @param context   needed to start the service
     * @param taskClass groundy task implementation
     * @param extras    the application code
     */
    public static void execute(Context context, Class<? extends GroundyTask> taskClass, Bundle extras) {
        execute(context, taskClass, null, extras);
    }

    /**
     * Execute this groundy task asynchronously
     *
     * @param context   needed to start the service
     * @param taskClass groundy task implementation
     * @param receiver  result receiver to report results back
     * @param extras    the application code
     */
    public static void execute(Context context, Class<? extends GroundyTask> taskClass, ResultReceiver receiver,
                               Bundle extras) {
        startApiService(context, receiver, taskClass, extras, true);
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

    private static void startApiService(Context context, ResultReceiver receiver,
                                        Class<? extends GroundyTask> taskClass, Bundle params, boolean async) {
        Intent intent = new Intent(context, GroundyService.class);
        intent.setAction(async ? GroundyService.ACTION_EXECUTE : GroundyService.ACTION_QUEUE);
        if (params != null) {
            intent.putExtra(KEY_PARAMETERS, params);
        }
        intent.putExtra(KEY_RECEIVER, receiver);
        intent.putExtra(KEY_TASK_NAME, taskClass);
        context.startService(intent);
    }
}
