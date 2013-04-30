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
import android.os.Bundle;
import android.os.ResultReceiver;
import com.telly.groundy.annotations.OnCallback;
import com.telly.groundy.annotations.OnProgress;
import java.lang.annotation.Annotation;

/** Implementation of this class get executed by the {@link GroundyService} */
public abstract class GroundyTask {
  protected static final int CANCEL_ALL = -1;
  protected static final int SERVICE_DESTROYED = -2;
  protected static final int CANCEL_BY_GROUP = -3;

  private Context mContext;
  //  private final Bundle mResultData = new Bundle();
  private final Bundle mParameters = new Bundle();
  private int mStartId;
  private ResultReceiver mReceiver;
  private volatile int mQuittingReason = Integer.MIN_VALUE;
  private int mGroupId;
  private boolean mRedelivered;
  private long mId;

  /** Creates a GroundyTask composed of */
  public GroundyTask() {
  }

  final void setContext(Context context) {
    mContext = context;
  }

  void setGroupId(int groupId) {
    mGroupId = groupId;
  }

  protected final int getGroupId() {
    return mGroupId;
  }

  void setStartId(int startId) {
    mStartId = startId;
  }

  protected final int getStartId() {
    return mStartId;
  }

  void setRedelivered(boolean redelivered) {
    mRedelivered = redelivered;
  }

  void setId(long id) {
    mId = id;
  }

  protected long getId() {
    return mId;
  }

  protected TaskResult boolToResult(boolean success) {
    return success ? new Succeeded() : new Failed();
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
   * Determinate if there is Internet connection
   *
   * @return true if Online false otherwise
   */
  protected boolean isOnline() {
    return DeviceStatus.isOnline(mContext);
  }

  /** @param parameters the parameters to set */
  void addParameters(Bundle parameters) {
    if (parameters != null) {
      mParameters.putAll(parameters);
    }
  }

  protected Bundle getParameters() {
    return mParameters;
  }

  protected String getStringParam(String key) {
    return getStringParam(key, null);
  }

  protected String getStringParam(String key, String defValue) {
    String value = mParameters.getString(key);
    return value != null ? value : defValue;
  }

  protected CharSequence getCharSequenceParam(String key) {
    return getCharSequenceParam(key, null);
  }

  protected CharSequence getCharSequenceParam(String key, String defValue) {
    CharSequence value = mParameters.getCharSequence(key);
    return value != null ? value : defValue;
  }

  protected int getIntParam(String key) {
    return getIntParam(key, 0);
  }

  protected int getIntParam(String key, int defValue) {
    return mParameters.getInt(key, defValue);
  }

  protected float getFloatParam(String key) {
    return getFloatParam(key, 0);
  }

  protected float getFloatParam(String key, float defValue) {
    return mParameters.getFloat(key, defValue);
  }

  protected double getDoubleParam(String key) {
    return getDoubleParam(key, 0);
  }

  protected double getDoubleParam(String key, double defValue) {
    return mParameters.getDouble(key, defValue);
  }

  protected boolean getBooleanParam(String key) {
    return getBooleanParam(key, false);
  }

  protected boolean getBooleanParam(String key, boolean defValue) {
    return mParameters.getBoolean(key, defValue);
  }

  void setReceiver(ResultReceiver receiver) {
    mReceiver = receiver;
  }

  void send(Class<? extends Annotation> callbackAnnotation, Bundle resultData) {
    if (mReceiver != null) {
      if (resultData == null) resultData = new Bundle();
      resultData.putLong(Groundy.TASK_ID, getId());
      resultData.putSerializable(Groundy.KEY_CALLBACK_ANNOTATION, callbackAnnotation);
      mReceiver.send(Groundy.RESULT_CODE_CALLBACK_ANNOTATION, resultData);
    }
  }

  /**
   * Sends this data to the callback methods annotated with the specified name
   *
   * @param name the name of the callback to invoke
   */
  protected void callback(String name) {
    callback(name, new Bundle());
  }

  /**
   * Sends this data to the callback methods annotated with the specified name
   *
   * @param name the name of the callback to invoke
   * @param resultData optional params to send
   */
  protected void callback(String name, Bundle resultData) {
    if (resultData == null) resultData = new Bundle();
    resultData.putString(Groundy.KEY_CALLBACK_NAME, name);
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
   * some other custom reason
   *
   * @return quitting reason
   */
  protected int getQuittingReason() {
    return mQuittingReason;
  }

  /**
   * Mark this value as quitting
   *
   * @param reason the reason to stop this value
   */
  void stopTask(int reason) {
    mQuittingReason = reason;
  }

  /**
   * Prepare and sends a progress update to the current receiver. OnCallback used is {@link
   * com.telly.groundy.annotations.OnProgress} and it will contain a bundle with an integer extra
   * called {@link Groundy#KEY_PROGRESS}
   *
   * @param progress percentage to send to receiver
   */
  public void updateProgress(int progress) {
    if (mReceiver != null) {
      Bundle resultData = new Bundle();
      resultData.putInt(Groundy.KEY_PROGRESS, progress);
      send(OnProgress.class, resultData);
    }
  }

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
   * @return true if the job finished successfully; false otherwise.
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
    if (!mParameters.isEmpty()) {
      toString += ", parameters=" + mParameters;
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

  /** Called once the value has been instantiated and it has a valid context */
  protected void onCreate() {
  }
}
