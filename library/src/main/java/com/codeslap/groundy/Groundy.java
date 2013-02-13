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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;

public class Groundy {
    public static final String KEY_PARAMETERS = "com.codeslap.groundy.key.PARAMATERS";
    public static final String KEY_ERROR = "com.codeslap.groundy.key.ERROR";
    public static final String KEY_RECEIVER = "com.codeslap.groundy.key.RECEIVER";
    public static final String KEY_PROGRESS = "com.codeslap.groundy.key.PROGRESS";
    public static final String KEY_TASK = "com.codeslap.groundy.key.TASK";
    public static final String KEY_GROUP_ID = "com.codeslap.groundy.key.GROUP_ID";

    public static final int STATUS_FINISHED = 200;
    public static final int STATUS_ERROR = 232;
    public static final int STATUS_RUNNING = 224;
    public static final int STATUS_PROGRESS = 225;

    private final Context mContext;
    private final Class<? extends GroundyTask> mGroundyTask;
    private ResultReceiver mResultReceiver;
    private Bundle mParams;
    private int mGroupId;
    private boolean mAlreadyProcessed = false;
    private Class<? extends GroundyService> mGroundyClass = GroundyService.class;

    private Groundy(Context context, Class<? extends GroundyTask> groundyTask) {
        mContext = context.getApplicationContext();
        mGroundyTask = groundyTask;
    }

    /**
     * Creates a new Groundy instance ready to be queued or executed.
     * You can configure it by adding parameters ({@link #params(android.os.Bundle)}),
     * setting a group id ({@link #group(int)}) or providing a result
     * receiver ({@link #receiver(android.os.ResultReceiver)}).
     * <p/>
     * You must configure the task <b>before</b> queueing or executing it.
     *
     * @param context     used to start the groundy service
     * @param groundyTask reference of the groundy task implementation
     * @return new Groundy instance (does not execute anything)
     */
    public static Groundy create(Context context, Class<? extends GroundyTask> groundyTask) {
        if (context == null) {
            throw new IllegalStateException("Invalid context");
        }
        if (groundyTask == null) {
            throw new IllegalStateException("GroundyTask no provided");
        }
        return new Groundy(context, groundyTask);
    }

    /**
     * Set the parameters that the task needs in order to run
     *
     * @param params a bundle of params
     * @return itself
     */
    public Groundy params(Bundle params) {
        if (mAlreadyProcessed) {
            throw new IllegalStateException("This method can only be called before queue() or execute() methods");
        }
        mParams = params;
        return this;
    }

    /**
     * {@link ResultReceiver}s allows you to communicate from a groundy
     * task to the activity that executed the task
     *
     * @param resultReceiver the result receiver
     * @return itself
     */
    public Groundy receiver(ResultReceiver resultReceiver) {
        if (mAlreadyProcessed) {
            throw new IllegalStateException("This method can only be called before queue() or execute() methods");
        }
        mResultReceiver = resultReceiver;
        return this;
    }

    /**
     * This allows you to set an identification groupId to the task
     * which can be later used to cancel it. Group ids can be shared
     * by several groundy tasks even if their implementation is
     * different. If cancelling tasks using a groupId, all tasks
     * created with this groupId will be cancelled and/or removed
     * from the queue.
     *
     * @param groupId groupId for this task
     * @return itself
     */
    public Groundy group(int groupId) {
        if (groupId <= 0) {
            throw new IllegalStateException("Group id must be greater than zero");
        }
        if (mAlreadyProcessed) {
            throw new IllegalStateException("This method can only be called before queue() or execute() methods");
        }
        mGroupId = groupId;
        return this;
    }

    /**
     * This allows you to use a different GroundyService implementation.
     * This
     *
     * @param groundyClass a different Groundy service implementation
     * @return itself
     */
    public Groundy service(Class<? extends GroundyService> groundyClass) {
        if (groundyClass == GroundyService.class) {
            throw new IllegalStateException("This method is meant to set a different GroundyService implementation. " +
                    "You cannot use GroundyService.class, http://i.imgur.com/IR23PAe.png");
        }
        if (mAlreadyProcessed) {
            throw new IllegalStateException("This method can only be called before queue() or execute() methods");
        }
        mGroundyClass = groundyClass;
        return this;
    }

    /**
     * Queues a task to the Groundy Service. This task won't be
     * executed until the previous queued tasks are done.
     * If you need your task to execute right away use the
     * {@link Groundy#execute()} method.
     */
    public void queue() {
        if (mAlreadyProcessed) {
            throw new IllegalStateException("Task already queued or executed");
        }
        mAlreadyProcessed = true;
        boolean async = false;
        startApiService(async);
    }

    /**
     * Execute a task right away
     */
    public void execute() {
        if (mAlreadyProcessed) {
            throw new IllegalStateException("Task already queued or executed");
        }
        mAlreadyProcessed = true;
        boolean async = true;
        startApiService(async);
    }

    private void startApiService(boolean async) {
        Intent intent = new Intent(mContext, mGroundyClass);
        intent.setAction(async ? GroundyService.ACTION_EXECUTE : GroundyService.ACTION_QUEUE);
        if (mParams != null) {
            intent.putExtra(KEY_PARAMETERS, mParams);
        }
        if (mResultReceiver != null) {
            intent.putExtra(KEY_RECEIVER, mResultReceiver);
        }
        intent.putExtra(KEY_TASK, mGroundyTask);
        intent.putExtra(KEY_GROUP_ID, mGroupId);
        mContext.startService(intent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Groundy groundy = (Groundy) o;

        if (mGroupId != groundy.mGroupId) return false;
        if (mContext != null ? !mContext.equals(groundy.mContext) : groundy.mContext != null) return false;
        if (mGroundyTask != null ? !mGroundyTask.equals(groundy.mGroundyTask) : groundy.mGroundyTask != null)
            return false;
        if (mParams != null ? !mParams.equals(groundy.mParams) : groundy.mParams != null) return false;
        if (mResultReceiver != null ? !mResultReceiver.equals(groundy.mResultReceiver) : groundy.mResultReceiver != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mContext != null ? mContext.hashCode() : 0;
        result = 31 * result + (mGroundyTask != null ? mGroundyTask.hashCode() : 0);
        result = 31 * result + (mResultReceiver != null ? mResultReceiver.hashCode() : 0);
        result = 31 * result + (mParams != null ? mParams.hashCode() : 0);
        result = 31 * result + mGroupId;
        return result;
    }

    @Override
    public String toString() {
        return "Groundy{" +
                "context=" + mContext +
                ", groundyTask=" + mGroundyTask +
                ", resultReceiver=" + mResultReceiver +
                ", extras=" + mParams +
                ", groupId=" + mGroupId +
                '}';
    }

    /**
     * Cancel all tasks: the ones running and parallel and
     * future tasks.
     *
     * @param context used to interact with the service
     */
    public static void cancelAll(Context context) {
        new GroundyServiceConnection(context) {
            @Override
            protected void onGroundyServiceBound(GroundyService.GroundyServiceBinder binder) {
                binder.cancelAllTasks();
            }
        }.start();
    }

    /**
     * Cancels all tasks of the specified group. The tasks get cancelled with
     * the {@link GroundyTask#CANCEL_BY_GROUP} reason.
     *
     * @param context used to interact with the service
     * @param groupId the group id to cancel
     */
    public static void cancelTasks(Context context, int groupId) {
        cancelTasks(context, groupId, GroundyTask.CANCEL_BY_GROUP);
    }

    /**
     * Cancels all tasks of the specified group w/ the specified reason.
     *
     * @param context used to interact with the service
     * @param groupId the group id to cancel
     */
    public static void cancelTasks(final Context context, final int groupId, final int reason) {
        if (groupId <= 0) {
            throw new IllegalStateException("Group id must be greater than zero");
        }
        new GroundyServiceConnection(context) {
            @Override
            protected void onGroundyServiceBound(GroundyService.GroundyServiceBinder binder) {
                binder.cancelTasks(groupId, reason);
            }
        }.start();
    }

    private abstract static class GroundyServiceConnection implements ServiceConnection {
        private Context mContext;
        private boolean mAlreadyStarted;

        private GroundyServiceConnection(Context context) {
            mContext = context;
        }

        @Override
        public final void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof GroundyService.GroundyServiceBinder) {
                GroundyService.GroundyServiceBinder binder = (GroundyService.GroundyServiceBinder) service;
                onGroundyServiceBound(binder);
            }
            mContext.unbindService(this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        void start() {
            if (mAlreadyStarted) {
                throw new IllegalStateException("Trying to use already started groundy service connector");
            }
            mAlreadyStarted = true;
            Intent intent = new Intent(mContext, GroundyService.class);
            mContext.bindService(intent, this, Context.BIND_AUTO_CREATE);
        }

        protected abstract void onGroundyServiceBound(GroundyService.GroundyServiceBinder binder);
    }

    public static void setLogEnabled(boolean enabled) {
        L.logEnabled = enabled;
    }
}
