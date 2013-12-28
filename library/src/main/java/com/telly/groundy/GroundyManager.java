/**
 * Copyright Telly, Inc. and other Groundy contributors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.telly.groundy;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import java.util.List;

/**
 * Allows you to manage your groundy services: cancel all tasks, cancel tasks by group, attach new
 * result receivers, etc.
 */
public final class GroundyManager {

  private GroundyManager() {
  }

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
   * @param context used to interact with the service
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
   * @param context used to interact with the service
   * @param groupId the group id to cancel
   * @param cancelListener callback for cancel result
   */
  public static void cancelTasksByGroup(Context context, int groupId,
      CancelListener cancelListener) {
    cancelTasksByGroup(context, groupId, GroundyTask.CANCEL_BY_GROUP, cancelListener);
  }

  /**
   * Cancels all tasks of the specified group w/ the specified reason.
   *
   * @param context used to interact with the service
   * @param groupId the group id to cancel
   * @param cancelListener callback for cancel result
   */
  public static void cancelTasksByGroup(Context context, int groupId, int reason,
      CancelListener cancelListener) {
    cancelTasks(context, GroundyService.class, groupId, reason, cancelListener);
  }

  /**
   * Cancels all tasks of the specified group w/ the specified reason.
   *
   * @param context used to interact with the service
   * @param id the value to cancel
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
   * @param context used to interact with the service
   * @param groupId the group id to cancel
   * @param cancelListener callback for cancel result
   */
  public static void cancelTasks(final Context context,
      Class<? extends GroundyService> groundyServiceClass, final int groupId, final int reason,
      final CancelListener cancelListener) {
    if (groupId <= 0) {
      throw new IllegalStateException("Group id must be greater than zero");
    }
    new GroundyServiceConnection(context, groundyServiceClass) {
      @Override
      protected void onGroundyServiceBound(GroundyService.GroundyServiceBinder binder) {
        GroundyService.CancelGroupResponse cancelGroupResponse =
            binder.cancelTasks(groupId, reason);
        if (cancelListener != null) {
          cancelListener.onCancelResult(groupId, cancelGroupResponse);
        }
      }
    }.start();
  }

  public static void attachCallbacks(Context context, OnAttachListener onAttachListener,
      final Class<? extends GroundyTask> task, final Object... callbacks) {
    attachCallbacks(context, GroundyService.class, onAttachListener, task, callbacks);
  }

  public static void attachCallbacks(Context context,
                                     Class<? extends GroundyService> groundyServiceClass,
                                     final OnAttachListener onAttachListener,
                                     final Class<? extends GroundyTask> task,
                                     final Object... callbacks) {
    new GroundyServiceConnection(context, groundyServiceClass) {
      @Override
      protected void onGroundyServiceBound(GroundyService.GroundyServiceBinder binder) {
        List<TaskHandler> taskHandlers = binder.attachCallbacks(task, callbacks);
        if (onAttachListener != null) {
          onAttachListener.attachePerformed(task, taskHandlers);
        }
      }
    }.start();
  }

  public static void setLogEnabled(boolean enabled) {
    L.logEnabled = enabled;
  }

  private abstract static class GroundyServiceConnection implements ServiceConnection {
    private final Context mContext;
    private boolean mAlreadyStarted;
    private final Class<? extends GroundyService> mGroundyServiceClass;

    GroundyServiceConnection(Context context, Class<? extends GroundyService> groundyServiceClass) {
      mContext = context.getApplicationContext();
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

  public interface CancelListener {
    void onCancelResult(int groupId, GroundyService.CancelGroupResponse tasksCancelled);
  }

  public interface SingleCancelListener {
    /**
     * @param id the id of the cancelled value
     * @param result either {@link GroundyService#COULD_NOT_CANCEL}, {@link
     * GroundyService#INTERRUPTED} and {@link GroundyService#NOT_EXECUTED}
     */
    void onCancelResult(long id, int result);
  }

  /** Listens for results of the callback attachment. */
  public interface OnAttachListener {
    /**
     * @param task the task that was targeted for the attachment
     * @param taskHandlers task handlers for each groundy task that we attached to
     */
    void attachePerformed(Class<? extends GroundyTask> task, List<TaskHandler> taskHandlers);
  }
}
