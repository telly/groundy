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

class AttachedTaskHandlerImpl implements TaskHandler {

  private boolean mTaskEnded = false;
  private final long mId;
  private final Class<? extends GroundyService> mGroundyServiceClass;
  private final CallbacksReceiver mCallbacksReceiver;
  private final Class<? extends GroundyTask> mGroundyTaskClass;

  AttachedTaskHandlerImpl(long id, Class<? extends GroundyService> groundyServiceClass,
      CallbacksReceiver callbacksReceiver, Class<? extends GroundyTask> groundyTaskClass) {
    mId = id;
    mGroundyServiceClass = groundyServiceClass;
    mCallbacksReceiver = callbacksReceiver;
    mGroundyTaskClass = groundyTaskClass;
  }

  @Override public void cancel(Context context, int reason,
      GroundyManager.SingleCancelListener cancelListener) {
    if (!mTaskEnded) {
      GroundyManager.cancelTaskById(context, mId, reason, cancelListener, mGroundyServiceClass);
    } else if (cancelListener != null) {
      cancelListener.onCancelResult(mId, GroundyService.COULD_NOT_CANCEL);
    }
  }

  @Override public void clearCallbacks() {
    if (mCallbacksReceiver != null) {
      mCallbacksReceiver.clearHandlers();
    }
  }

  @Override public boolean taskAlreadyEnded() {
    return mTaskEnded;
  }

  @Override public void appendCallbacks(Object... handlers) {
    if (mCallbacksReceiver != null) {
      mCallbacksReceiver.appendCallbackHandlers(handlers);
    }
  }

  @Override public void removeCallbacks(Object... handlers) {
    if (mCallbacksReceiver != null) {
      mCallbacksReceiver.removeCallbackHandlers(mGroundyTaskClass, handlers);
    }
  }

  @Override public long getTaskId() {
    return mId;
  }

  void onTaskEnded() {
    mTaskEnded = true;
  }

  @SuppressWarnings("UnusedDeclaration")
  public static final Creator<AttachedTaskHandlerImpl> CREATOR =
      new Creator<AttachedTaskHandlerImpl>() {
        @Override public AttachedTaskHandlerImpl createFromParcel(Parcel source) {
          //Groundy groundy = source.readParcelable(Groundy.class.getClassLoader());
          ////noinspection unchecked
          //return new AttachedTaskHandlerImpl(groundy);
          return null;
        }

        @Override public AttachedTaskHandlerImpl[] newArray(int size) {
          return new AttachedTaskHandlerImpl[size];
        }
      };

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    //dest.writeParcelable(groundy, flags);
  }
}
