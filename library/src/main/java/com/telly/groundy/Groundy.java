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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;

public class Groundy<T extends GroundyTask> implements Parcelable {
  public static final String KEY_PARAMETERS = "com.telly.groundy.key.PARAMATERS";
  public static final String CRASH_MESSAGE = "com.telly.groundy.key.ERROR";
  public static final String KEY_RECEIVER = "com.telly.groundy.key.RECEIVER";
  public static final String KEY_PROGRESS = "com.telly.groundy.key.PROGRESS";
  public static final String KEY_TASK = "com.telly.groundy.key.TASK";
  public static final String KEY_GROUP_ID = "com.telly.groundy.key.GROUP_ID";
  public static final String TASK_ID = "com.telly.groundy.key.TASK_ID";
  public static final String CANCEL_REASON = "com.telly.groundy.key.CANCEL_REASON";
  public static final String ORIGINAL_PARAMS = "com.telly.groundy.key.ORIGINAL_PARAMS";
  static final String KEY_CALLBACK_ANNOTATION = "com.telly.groundy.key.CALLBACK_ANNOTATION";
  static final int RESULT_CODE_CALLBACK_ANNOTATION = 888;

  private final Class<? extends GroundyTask> mGroundyTask;
  private final long mId;
  private InternalReceiver mResultReceiver;
  private Bundle mParams;
  private int mGroupId;
  private boolean mAlreadyProcessed = false;
  private CallbacksManager callbacksManager;
  private Class<? extends GroundyService> mGroundyClass = GroundyService.class;

  private Groundy(Class<T> groundyTask) {
    mGroundyTask = groundyTask;
    mId = System.nanoTime();
  }

  private Groundy(Class<T> groundyTask, long id) {
    mGroundyTask = groundyTask;
    mId = id;
  }

  /**
   * Creates a new Groundy instance ready to be queued or executed. You can configure it by adding
   * parameters ({@link #params(android.os.Bundle)}), setting a group id ({@link #group(int)}) or
   * providing a callback ({@link #callback(Object...)}).
   * <p/>
   * You must configure the task <b>before</b> queueing or executing it.
   *
   * @param groundyTask reference of the groundy task implementation
   * @return new Groundy instance (does not execute anything)
   */
  public static <T extends GroundyTask> Groundy<? extends GroundyTask> create(
      Class<T> groundyTask) {
    if (groundyTask == null) {
      throw new IllegalStateException("GroundyTask no provided");
    }
    return new Groundy<T>(groundyTask);
  }

  /**
   * Set the parameters that the task needs in order to run
   *
   * @param params a bundle of params
   * @return itself
   */
  public Groundy params(Bundle params) {
    checkAlreadyProcessed();
    mParams = params;
    return this;
  }

  /**
   * @param callbacks callbacks to register for this task
   * @return itself
   */
  public Groundy callback(Object... callbacks) {
    if (callbacks == null || callbacks.length == 0) {
      throw new IllegalArgumentException("You must pass at least one callback handler");
    }
    checkAlreadyProcessed();
    mResultReceiver = new InternalReceiver(mGroundyTask, callbacks);
    return this;
  }

  /**
   * This allows you to set an identification groupId to the task which can be later used to cancel
   * it. Group ids can be shared by several groundy tasks even if their implementation is different.
   * If cancelling tasks using a groupId, all tasks created with this groupId will be cancelled
   * and/or removed from the queue.
   *
   * @param groupId groupId for this task
   * @return itself
   */
  public Groundy group(int groupId) {
    if (groupId <= 0) {
      throw new IllegalStateException("Group id must be greater than zero");
    }
    checkAlreadyProcessed();
    mGroupId = groupId;
    return this;
  }

  /**
   * This allows you to use a different GroundyService implementation.
   *
   * @param groundyClass a different Groundy service implementation
   * @return itself
   */
  public Groundy service(Class<? extends GroundyService> groundyClass) {
    if (groundyClass == GroundyService.class) {
      throw new IllegalStateException(
          "This method is meant to set a different GroundyService implementation. " + "You cannot use GroundyService.class, http://i.imgur.com/IR23PAe.png");
    }
    checkAlreadyProcessed();
    mGroundyClass = groundyClass;
    return this;
  }

  /**
   * Sets a callback manager for this task. It allows you to easily attach/detach your callbacks on
   * configuration change. This is important if you are not handling the configuration changes by
   * your self, since it will prevent leaks or wrong results when callbacks are invoked.
   *
   * @param callbacksManager a callback manager instance
   * @return itself
   */
  public Groundy callbackManager(CallbacksManager callbacksManager) {
    checkAlreadyProcessed();
    this.callbacksManager = callbacksManager;
    return this;
  }

  /**
   * Queues a task to the Groundy Service. This task won't be executed until the previous queued
   * tasks are done. If you need your task to execute right away use the {@link
   * Groundy#execute(Context)} method.
   *
   * @param context used to start the groundy service
   * @return a unique number assigned to this task
   */
  public TaskProxy queue(Context context) {
    boolean async = false;
    return internalQueueOrExecute(context, async);
  }

  /**
   * Execute a task right away
   *
   * @param context used to start the groundy service
   * @return a unique number assigned to this task
   */
  public TaskProxyImpl<T> execute(Context context) {
    boolean async = true;
    return internalQueueOrExecute(context, async);
  }

  private TaskProxyImpl<T> internalQueueOrExecute(Context context, boolean async) {
    markAsProcessed();
    TaskProxyImpl<T> taskProxy = new TaskProxyImpl<T>(this);
    if (callbacksManager != null) {
      callbacksManager.register(taskProxy);
    }

    if (mResultReceiver != null) {
      mResultReceiver.setOnFinishedListener(taskProxy);
    }

    startApiService(context, async);
    return taskProxy;
  }

  long getId() {
    return mId;
  }

  Class<? extends GroundyService> getGroundyServiceClass() {
    return mGroundyClass;
  }

  Class<? extends GroundyTask> getGroundyTaskClass() {
    return mGroundyTask;
  }

  InternalReceiver getReceiver() {
    return mResultReceiver;
  }

  private void checkAlreadyProcessed() {
    if (mAlreadyProcessed) {
      throw new IllegalStateException(
          "This method can only be called before queue() or execute() methods");
    }
  }

  private void markAsProcessed() {
    if (mAlreadyProcessed) {
      throw new IllegalStateException("Task already queued or executed");
    }
    mAlreadyProcessed = true;
  }

  private void startApiService(Context context, boolean async) {
    Intent intent = new Intent(context, mGroundyClass);
    intent.setAction(async ? GroundyService.ACTION_EXECUTE : GroundyService.ACTION_QUEUE);
    if (mParams != null) {
      intent.putExtra(KEY_PARAMETERS, mParams);
    }
    if (mResultReceiver != null) {
      intent.putExtra(KEY_RECEIVER, mResultReceiver);
    }
    intent.putExtra(KEY_TASK, mGroundyTask);
    intent.putExtra(TASK_ID, mId);
    intent.putExtra(KEY_GROUP_ID, mGroupId);
    context.startService(intent);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Groundy)) {
      return false;
    }

    Groundy groundy = (Groundy) o;

    if (mId != groundy.mId) return false;
    if (!mGroundyTask.equals(groundy.mGroundyTask)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = mGroundyTask.hashCode();
    result = 31 * result + (int) (mId ^ (mId >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "Groundy{" +
        ", groundyTask=" + mGroundyTask +
        ", resultReceiver=" + mResultReceiver +
        ", extras=" + mParams +
        ", groupId=" + mGroupId +
        '}';
  }

  @SuppressWarnings("UnusedDeclaration")
  public static final Creator<Groundy> CREATOR = new Creator<Groundy>() {
    @Override public Groundy createFromParcel(Parcel source) {
      Class groundyTask = (Class) source.readSerializable();
      long id = source.readLong();

      //noinspection unchecked
      Groundy groundy = new Groundy(groundyTask, id);
      groundy.mResultReceiver = source.readParcelable(ResultReceiver.class.getClassLoader());
      groundy.mParams = source.readBundle();
      groundy.mGroupId = source.readInt();
      groundy.mAlreadyProcessed = source.readByte() == 1;
      groundy.mGroundyClass = (Class) source.readSerializable();
      return groundy;
    }

    @Override public Groundy[] newArray(int size) {
      return new Groundy[size];
    }
  };

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeSerializable(mGroundyTask);
    dest.writeLong(mId);
    dest.writeParcelable(mResultReceiver, flags);
    dest.writeBundle(mParams);
    dest.writeInt(mGroupId);
    dest.writeByte((byte) (mAlreadyProcessed ? 1 : 0));
    dest.writeSerializable(mGroundyClass);
  }
}
