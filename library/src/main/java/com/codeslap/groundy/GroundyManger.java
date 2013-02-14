package com.codeslap.groundy;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.text.TextUtils;

/**
 * Allows you to manage your groundy services: cancel all tasks, cancel tasks by group,
 * attach new result receivers, etc.
 *
 * @author cristian
 */
public class GroundyManger {
    /**
     * Cancel all tasks: the ones running and parallel and
     * future tasks.
     *
     * @param context used to interact with the service
     */
    public static void cancelAll(Context context) {
        cancelAll(context, GroundyService.class);
    }

    /**
     * Cancel all tasks: the ones running and parallel and
     * future tasks.
     *
     * @param context             used to interact with the service
     * @param groundyServiceClass custom groundy service implementation
     */
    public static void cancelAll(Context context, Class<? extends GroundyService> groundyServiceClass) {
        new GroundyServiceConnection(context, groundyServiceClass) {
            @Override
            protected void onGroundyServiceBound(GroundyService.GroundyServiceBinder binder) {
                binder.cancelAllTasks();
            }
        }.start();
    }

    /**
     * Cancels all tasks of the specified group. The tasks get cancelled with
     * the {@link com.codeslap.groundy.GroundyTask#CANCEL_BY_GROUP} reason.
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
    public static void cancelTasks(Context context, int groupId, int reason) {
        cancelTasks(context, GroundyService.class, groupId, reason);
    }

    /**
     * Cancels all tasks of the specified group w/ the specified reason.
     *
     * @param context used to interact with the service
     * @param groupId the group id to cancel
     */
    public static void cancelTasks(final Context context, Class<? extends GroundyService> groundyServiceClass,
                                   final int groupId, final int reason) {
        if (groupId <= 0) {
            throw new IllegalStateException("Group id must be greater than zero");
        }
        new GroundyServiceConnection(context, groundyServiceClass) {
            @Override
            protected void onGroundyServiceBound(GroundyService.GroundyServiceBinder binder) {
                binder.cancelTasks(groupId, reason);
            }
        }.start();
    }

    /**
     * Attach a receiver to an existing groundy service
     *
     * @param context        used to reach the service
     * @param token          used to refer to a specific task
     * @param resultReceiver result receiver to attach
     * @return true if the service was reached (it does not mean the result receiver was attached)
     */
    public static boolean attachReceiver(Context context, final String token, final ResultReceiver resultReceiver) {
        return attachReceiver(context, GroundyService.class, token, resultReceiver);
    }

    /**
     * Attach a receiver to an existing groundy service
     *
     * @param context             used to reach the service
     * @param groundyServiceClass a custom GroundyService implementation
     * @param token               used to refer to a specific task
     * @param resultReceiver      result receiver to attach
     * @return true if the service was reached (it does not mean the result receiver was attached)
     */
    public static boolean attachReceiver(Context context, Class<? extends GroundyService> groundyServiceClass,
                                         final String token, final ResultReceiver resultReceiver) {
        if (TextUtils.isEmpty(token)) {
            throw new IllegalStateException("token cannot be null");
        }

        if (resultReceiver == null) {
            throw new IllegalStateException("result receiver cannot be null");
        }

        return context.bindService(new Intent(context, GroundyService.class), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (service instanceof GroundyService.GroundyServiceBinder) {
                    GroundyService.GroundyServiceBinder binder = (GroundyService.GroundyServiceBinder) service;
                    binder.attachReceiver(token, resultReceiver);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        }, Context.BIND_AUTO_CREATE);
    }

    public static void setLogEnabled(boolean enabled) {
        L.logEnabled = enabled;
    }

    private abstract static class GroundyServiceConnection implements ServiceConnection {
        private Context mContext;
        private boolean mAlreadyStarted;
        private final Class<? extends GroundyService> mGroundyServiceClass;

        private GroundyServiceConnection(Context context, Class<? extends GroundyService> groundyServiceClass) {
            mContext = context;
            mGroundyServiceClass = groundyServiceClass;
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
            Intent intent = new Intent(mContext, mGroundyServiceClass);
            mContext.bindService(intent, this, Context.BIND_AUTO_CREATE);
        }

        protected abstract void onGroundyServiceBound(GroundyService.GroundyServiceBinder binder);
    }
}
