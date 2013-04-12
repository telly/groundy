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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.text.TextUtils;

/**
 * Allows you to manage your groundy services: cancel all tasks, cancel tasks by group, attach new
 * result receivers, etc.
 *
 * @author Cristian <cristian@elhacker.net>
 */
public class GroundyManger {
  /**
   * Cancel all tasks: the ones running and parallel and future tasks.
   *
   * @param context used to interact with the service
   */
  public static void cancelAll(Context context) {
    cancelAll(context, GroundyService.class);
  }

  /**
   * Cancel all tasks: the ones running and parallel and future tasks.
   *
   * @param context             used to interact with the service
   * @param groundyServiceClass custom groundy service implementation
   */
  public static void cancelAll(Context context,
                               Class<? extends GroundyService> groundyServiceClass) {
    new GroundyServiceConnection(context, groundyServiceClass) {
      @Override
      protected void onGroundyServiceBound(GroundyService.GroundyServiceBinder binder) {
        binder.cancelAllTasks();
      }
    }.start();
  }

  /**
   * Cancels all tasks of the specified group. The tasks get cancelled with the {@link
   * GroundyTask#CANCEL_BY_GROUP} reason.
   *
   * @param context        used to interact with the service
   * @param groupId        the group id to cancel
   * @param cancelListener callback for cancel result
   */
  public static void cancelTasks(Context context, int groupId, CancelListener cancelListener) {
    cancelTasks(context, groupId, GroundyTask.CANCEL_BY_GROUP, cancelListener);
  }

  /**
   * Cancels all tasks of the specified group w/ the specified reason.
   *
   * @param context        used to interact with the service
   * @param groupId        the group id to cancel
   * @param cancelListener callback for cancel result
   */
  public static void cancelTasks(Context context, int groupId, int reason,
                                 CancelListener cancelListener) {
    cancelTasks(context, GroundyService.class, groupId, reason, cancelListener);
  }

  /**
   * Cancels all tasks of the specified group w/ the specified reason.
   *
   * @param context        used to interact with the service
   * @param id             the task to cancel
   * @param cancelListener callback for cancel result
   */
  public static void cancelTaskById(Context context, final long id, final SingleCancelListener cancelListener) {
    cancelTaskById(context, id, cancelListener, GroundyService.class);
  }

  /**
   * Cancels all tasks of the specified group w/ the specified reason.
   *
   * @param context        used to interact with the service
   * @param id             the task to cancel
   * @param cancelListener callback for cancel result
   */
  public static void cancelTaskById(Context context, final long id, final SingleCancelListener cancelListener,
                                    Class<? extends GroundyService> groundyServiceClass) {
    cancelTaskById(context, id, GroundyTask.CANCEL_BY_ID, cancelListener, groundyServiceClass);
  }

  /**
   * Cancels all tasks of the specified group w/ the specified reason.
   *
   * @param context        used to interact with the service
   * @param id             the task to cancel
   * @param cancelListener callback for cancel result
   */
  public static void cancelTaskById(Context context, final long id, final int reason,
                                    final SingleCancelListener cancelListener,
                                    Class<? extends GroundyService> groundyServiceClass) {
    if (id <= 0) {
      throw new IllegalStateException("id must be greater than zero");
    }
    new GroundyServiceConnection(context, groundyServiceClass) {
      @Override
      protected void onGroundyServiceBound(GroundyService.GroundyServiceBinder binder) {
        int result = binder.cancelTaskById(id, reason);
        if (cancelListener != null) {
          cancelListener.onCancelResult(id, result);
        }
      }
    }.start();
  }

  /**
   * Cancels all tasks of the specified group w/ the specified reason.
   *
   * @param context        used to interact with the service
   * @param groupId        the group id to cancel
   * @param cancelListener callback for cancel result
   */
  public static void cancelTasks(final Context context,
                                 Class<? extends GroundyService> groundyServiceClass,
                                 final int groupId, final int reason,
                                 final CancelListener cancelListener) {
    if (groupId <= 0) {
      throw new IllegalStateException("Group id must be greater than zero");
    }
    new GroundyServiceConnection(context, groundyServiceClass) {
      @Override
      protected void onGroundyServiceBound(GroundyService.GroundyServiceBinder binder) {
        GroundyService.CancelGroupResponse cancelGroupResponse = binder.cancelTasks(groupId, reason);
        if (cancelListener != null) {
          cancelListener.onCancelResult(groupId, cancelGroupResponse);
        }
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
  public static boolean attachReceiver(Context context, final String token,
                                       final ResultReceiver resultReceiver) {
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
  public static boolean attachReceiver(Context context,
                                       Class<? extends GroundyService> groundyServiceClass,
                                       final String token, final ResultReceiver resultReceiver) {
    if (TextUtils.isEmpty(token)) {
      throw new IllegalStateException("token cannot be null");
    }

    if (resultReceiver == null) {
      throw new IllegalStateException("result receiver cannot be null");
    }

    return context.bindService(new Intent(context, groundyServiceClass), new ServiceConnection() {
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

  /**
   * Detaches a receiver from the GroundyService
   *
   * @param context        used to reach the service
   * @param token          used to refer to a specific task
   * @param resultReceiver result receiver to attach
   * @return true if the service was reached (it does not mean the result receiver was attached)
   */
  public static boolean detachReceiver(Context context, final String token,
                                       final ResultReceiver resultReceiver) {
    return detachReceiver(context, GroundyService.class, token, resultReceiver);
  }

  /**
   * Detaches a receiver from the GroundyService
   *
   * @param context             used to reach the service
   * @param groundyServiceClass a custom GroundyService implementation
   * @param token               used to refer to a specific task
   * @param resultReceiver      result receiver to attach
   * @return true if the service was reached (it does not mean the result receiver was attached)
   */
  public static boolean detachReceiver(Context context,
                                       Class<? extends GroundyService> groundyServiceClass,
                                       final String token, final ResultReceiver resultReceiver) {
    if (TextUtils.isEmpty(token)) {
      throw new IllegalStateException("token cannot be null");
    }

    if (resultReceiver == null) {
      throw new IllegalStateException("result receiver cannot be null");
    }

    return context.bindService(new Intent(context, groundyServiceClass), new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName name, IBinder service) {
        if (service instanceof GroundyService.GroundyServiceBinder) {
          GroundyService.GroundyServiceBinder binder = (GroundyService.GroundyServiceBinder) service;
          binder.detachReceiver(token, resultReceiver);
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
    private final Context mContext;
    private boolean mAlreadyStarted;
    private final Class<? extends GroundyService> mGroundyServiceClass;

    private GroundyServiceConnection(Context context,
                                     Class<? extends GroundyService> groundyServiceClass) {
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

  public static interface CancelListener {
    void onCancelResult(int groupId, GroundyService.CancelGroupResponse tasksCancelled);
  }

  public static interface SingleCancelListener {
    void onCancelResult(long id, int result);
  }
}
