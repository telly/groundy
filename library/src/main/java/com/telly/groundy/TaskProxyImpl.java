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
import android.os.Parcel;

class TaskProxyImpl<T extends GroundyTask> implements TaskProxy {

  private final Groundy<T> groundy;
  private boolean mIsValid = true;

  TaskProxyImpl(Groundy<T> groundy) {
    this.groundy = groundy;
  }

  @Override public void updateCallbackHandlers(Object... callbackHandlers) {
    if (mIsValid && callbackHandlers != null) {
      InternalReceiver internalReceiver = groundy.getReceiver();
      internalReceiver.clearHandlers();
      internalReceiver.appendCallbackHandlers(callbackHandlers);
    }
  }

  @Override public void cancel(Context context, int reason,
                               GroundyManager.SingleCancelListener cancelListener) {
    if (mIsValid) {
      GroundyManager.cancelTaskById(context, groundy.getId(), reason, cancelListener,
          groundy.getGroundyServiceClass());
    } else {
      cancelListener.onCancelResult(groundy.getId(), GroundyService.COULD_NOT_CANCEL);
    }
  }

  @Override public void clearCallbackHandlers() {
    InternalReceiver internalReceiver = groundy.getReceiver();
    if (internalReceiver != null) {
      internalReceiver.clearHandlers();
    }
  }

  @Override public void onTaskEnded() {
    mIsValid = false;
  }

  @Override public boolean shouldRecycle() {
    return !mIsValid;
  }

  @Override public void appendCallbackHandlers(Object... handlers) {
    InternalReceiver internalReceiver = groundy.getReceiver();
    internalReceiver.appendCallbackHandlers(handlers);
  }

  @Override public void removeCallbackHandlers(Object... handlers) {
    InternalReceiver internalReceiver = groundy.getReceiver();
    internalReceiver.removeCallbackHandlers(groundy.getGroundyTaskClass(), handlers);
  }

  @Override public long getTaskId() {
    return groundy.getId();
  }

  @SuppressWarnings("UnusedDeclaration")
  public static final Creator<TaskProxyImpl> CREATOR = new Creator<TaskProxyImpl>() {
    @Override public TaskProxyImpl createFromParcel(Parcel source) {
      Groundy groundy = source.readParcelable(Groundy.class.getClassLoader());
      //noinspection unchecked
      return new TaskProxyImpl(groundy);
    }

    @Override public TaskProxyImpl[] newArray(int size) {
      return new TaskProxyImpl[size];
    }
  };

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(groundy, flags);
  }
}
