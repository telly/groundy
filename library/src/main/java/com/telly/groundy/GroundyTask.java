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
import android.os.ResultReceiver;
import com.telly.groundy.annotations.OnCallback;
import com.telly.groundy.annotations.OnProgress;
import java.lang.annotation.Annotation;
import java.util.ArrayList;

/** Implementation of this class get executed by the {@link GroundyService}. */
public abstract class GroundyTask {
  protected static final int CANCEL_ALL = -1;
  protected static final int SERVICE_DESTROYED = -2;
  protected static final int CANCEL_BY_GROUP = -3;
  static final int RESULT_CODE_CALLBACK_ANNOTATION = 888;

  private Context mContext;
  private final Bundle mArgs = new Bundle();
  private int mStartId;
  private ResultReceiver mReceiver;
  private volatile int mQuittingReason = Integer.MIN_VALUE;
  private int mGroupId;
  private boolean mRedelivered;
  private long mId;
  private StackTraceElement[] mStackTrace;
  private Intent mIntent;
  private ArrayList<ResultReceiver> mExtraReceivers;
  private boolean mExecuted;

  /** Creates a GroundyTask composed of. */
  public GroundyTask() {
  }

  final void setContext(Context context) {
    mContext = context;
  }

  final void setGroupId(int groupId) {
    mGroupId = groupId;
  }

  protected final int getGroupId() {
    return mGroupId;
  }

  final void setStartId(int startId) {
    mStartId = startId;
  }

  protected final int getStartId() {
    return mStartId;
  }

  final void setRedelivered(boolean redelivered) {
    mRedelivered = redelivered;
  }

  final void setId(long id) {
    mId = id;
  }

  protected long getId() {
    return mId;
  }

  /**
   * @param success true if the task was successful
   * @return a task result instance. {@link Succeeded} if true, {@link Failed} if false.
   */
  protected TaskResult boolToResult(boolean success) {
    return success ? succeeded() : failed();
  }

  /** @return a succeeded task result */
  protected TaskResult succeeded() {
    return new Succeeded();
  }

  /** @return a failed task result */
  protected TaskResult failed() {
    return new Failed();
  }

  /** @return a cancelled task result */
  protected TaskResult cancelled() {
    return new Cancelled();
  }

  /**
   * @return true if the value was run after a service was killed and force_queue_completion was
   *         used.
   */
  public boolean isRedelivered() {
    return mRedelivered;
  }

  protected final Context getContext() {
    return mContext;
  }

  /**
   * Determinate if there is Internet connection.
   *
   * @return true if Online false otherwise
   */
  protected boolean isOnline() {
    return DeviceStatus.isOnline(mContext);
  }

  /** @param args the args to add */
  void addArgs(Bundle args) {
    if (args != null) {
      mArgs.putAll(args);
    }
  }

  protected Bundle getArgs() {
    return mArgs;
  }

  protected String getStringArg(String key) {
    return getStringArg(key, null);
  }

  protected String getStringArg(String key, String defValue) {
    String value = mArgs.getString(key);
    return value != null ? value : defValue;
  }

  protected CharSequence getCharSequenceArg(String key) {
    return getCharSequenceArg(key, null);
  }

  protected CharSequence getCharSequenceArg(String key, String defValue) {
    CharSequence value = mArgs.getCharSequence(key);
    return value != null ? value : defValue;
  }

  protected int getIntArg(String key) {
    return getIntArg(key, 0);
  }

  protected int getIntArg(String key, int defValue) {
    return mArgs.getInt(key, defValue);
  }

  protected float getFloatArg(String key) {
    return getFloatArg(key, 0);
  }

  protected float getFloatArg(String key, float defValue) {
    return mArgs.getFloat(key, defValue);
  }

  protected double getDoubleArg(String key) {
    return getDoubleArg(key, 0);
  }

  protected double getDoubleArg(String key, double defValue) {
    return mArgs.getDouble(key, defValue);
  }

  protected boolean getBooleanArg(String key) {
    return getBooleanArg(key, false);
  }

  protected boolean getBooleanArg(String key, boolean defValue) {
    return mArgs.getBoolean(key, defValue);
  }

  protected long getLongArg(String key) {
    return getLongArg(key, 0);
  }

  protected long getLongArg(String key, long defValue) {
    return mArgs.getLong(key, defValue);
  }

  void setReceiver(ResultReceiver receiver) {
    mReceiver = receiver;
  }

  void send(Class<? extends Annotation> callbackAnnotation, Bundle resultData) {
    internalSend(mReceiver, resultData, callbackAnnotation);
    if (mExtraReceivers != null) {
      for (ResultReceiver extraReceiver : mExtraReceivers) {
        internalSend(extraReceiver, resultData, callbackAnnotation);
      }
    }
  }

  private void internalSend(ResultReceiver receiver, Bundle resultData,
      Class<? extends Annotation> callbackAnnotation) {
    if (receiver != null) {
      if (resultData == null) resultData = new Bundle();
      resultData.putLong(Groundy.TASK_ID, getId());
      resultData.putSerializable(Groundy.KEY_CALLBACK_ANNOTATION, callbackAnnotation);
      receiver.send(RESULT_CODE_CALLBACK_ANNOTATION, resultData);
    }
  }

  /**
   * Sends this data to the callback methods annotated with the specified name.
   *
   * @param name the name of the callback to invoke
   */
  protected void callback(String name) {
    callback(name, new Bundle());
  }

  /**
   * Sends this data to the callback methods annotated with the specified name.
   *
   * @param name the name of the callback to invoke
   * @param resultData optional arguments to send
   */
  protected void callback(String name, Bundle resultData) {
    if (resultData == null) resultData = new Bundle();
    resultData.putString(Groundy.KEY_CALLBACK_NAME, name);
    resultData.putSerializable(Groundy.TASK_IMPLEMENTATION, getClass());
    send(OnCallback.class, resultData);
  }

  /**
   * This must be checked every time you want to check whether the value is in quitting state. In
   * such cases you must make sure the value is stopped immediately. To know the reason causing the
   * value to be quited use the {@link GroundyTask#getQuittingReason()} method.
   *
   * @return true if the groundy value is in quitting state
   */
  protected boolean isQuitting() {
    return mQuittingReason != Integer.MIN_VALUE;
  }

  /**
   * This can be either {@link GroundyTask#CANCEL_ALL} or {@link GroundyTask#SERVICE_DESTROYED} or
   * some other custom reason.
   *
   * @return quitting reason
   */
  protected int getQuittingReason() {
    return mQuittingReason;
  }

  /**
   * Mark this value as quitting.
   *
   * @param reason the reason to stop this value
   */
  void stopTask(int reason) {
    mQuittingReason = reason;
  }

  /**
   * Prepare and sends a progress update to the current receiver. Callback used is {@link
   * com.telly.groundy.annotations.OnProgress} and it will contain a bundle with an integer extra
   * called {@link Groundy#PROGRESS}
   *
   * @param progress percentage to send to receiver or {@link Groundy#NO_SIZE_AVAILABLE}
   *                 if file size is not available.
   */
  public void updateProgress(int progress) {
    updateProgress(progress, null);
  }

  /**
   * Prepare and sends a progress update to the current receiver. Callback used is {@link
   * com.telly.groundy.annotations.OnProgress} and it will contain a bundle with an integer extra
   * called {@link Groundy#PROGRESS}
   *
   * @param extraData additional information to send to the progress callback
   * @param progress percentage to send to receiver
   */
  public void updateProgress(int progress, Bundle extraData) {
    if (mReceiver != null) {
      Bundle resultData = new Bundle();
      resultData.putInt(Groundy.PROGRESS, progress);
      resultData.putSerializable(Groundy.TASK_IMPLEMENTATION, getClass());
      if (extraData != null) resultData.putAll(extraData);
      send(OnProgress.class, resultData);
    }
  }

  /**
   * @return override and return true if you want to setup a {@link android.os.PowerManager.WakeLock}
   *         on the wireless connection.
   */
  protected boolean keepWifiOn() {
    return false;
  }

  /**
   * Override this if you want to cache the GroundyTask instance. Do it only if you are sure that
   * {@link GroundyTask#doInBackground()} method won't need a fresh instance each time they are
   * executed.
   *
   * @return true if this instance must be cached
   */
  protected boolean canBeCached() {
    return false;
  }

  /**
   * This must do all the background work.
   *
   * @return a {@link TaskResult} instance with optional data to send to the callback. If your task
   *         finished successfully, use the {@link #succeeded()} method to get a new instance of
   *         {@link TaskResult}; if the task failed use the {@link #failed()} method instead. If
   *         you are handling cases in which the task is cancelled, you can return {@link
   *         #cancelled()}
   */
  protected abstract TaskResult doInBackground();

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GroundyTask that = (GroundyTask) o;

    if (mId != that.mId) return false;

    return true;
  }

  @Override public int hashCode() {
    return (int) (mId ^ (mId >>> 32));
  }

  @Override public String toString() {
    String toString = getClass().getSimpleName() + "{groupId=" + mGroupId;
    toString += ", startId=" + mStartId;
    if (!mArgs.isEmpty()) {
      toString += ", arguments=" + mArgs;
    }
    if (mReceiver != null) {
      toString += ", receiver=" + mReceiver;
    }
    if (mRedelivered) {
      toString += ", redelivered";
    }
    if (mQuittingReason != 0) {
      switch (mQuittingReason) {
        case CANCEL_ALL:
          toString += ", quittingReason=CANCEL_ALL";
          break;
        case SERVICE_DESTROYED:
          toString += ", quittingReason=SERVICE_DESTROYED";
          break;
        case CANCEL_BY_GROUP:
          toString += ", quittingReason=CANCEL_BY_GROUP";
          break;
        default:
          toString += ", quittingReason=" + mQuittingReason;
      }
    }
    toString += '}';
    return toString;
  }

  /** Called once the value has been instantiated and it has a valid context. */
  protected void onCreate() {
  }

  void setStackTrace(StackTraceElement[] stackTrace) {
    mStackTrace = stackTrace;
  }

  protected StackTraceElement[] getStackTrace() {
    return mStackTrace;
  }

  void setIntent(Intent intent) {
    mIntent = intent;
  }

  /**
   * @return intent that can be used to repeat this task. The task id will be different and no
   *         callbacks will be assigned.
   */
  protected Intent asNewIntent() {
    if (mIntent != null) {
      Bundle extras = mIntent.getExtras();
      extras.putLong(Groundy.TASK_ID, System.nanoTime());
      extras.putSerializable(Groundy.KEY_RECEIVER, null);
    }
    return mIntent;
  }

  void appendReceiver(ResultReceiver resultReceiver) {
    if (mExtraReceivers == null) {
      mExtraReceivers = new ArrayList<ResultReceiver>();
    }
    mExtraReceivers.add(resultReceiver);
  }

  boolean alreadyExecuted() {
    return mExecuted;
  }

  void flagAsExecuted() {
    mExecuted = true;
  }
}
