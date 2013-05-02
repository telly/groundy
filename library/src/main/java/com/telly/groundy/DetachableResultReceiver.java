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

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

/**
 * Proxy {@link android.os.ResultReceiver} that offers a listener interface that can be
 * detached. Useful for when sending callbacks to a {@link android.app.Service} where a
 * listening {@link android.app.Activity} can be swapped out during configuration changes.
 */
public class DetachableResultReceiver extends ResultReceiver {
  private static final String TAG = "DetachableResultReceiver";

  private Receiver mReceiver;

  public DetachableResultReceiver(Handler handler) {
    super(handler);
  }

  public void clearReceiver() {
    mReceiver = null;
  }

  public void setReceiver(Receiver receiver) {
    mReceiver = receiver;
  }

  public interface Receiver {
    public void onReceiveResult(int resultCode, Bundle resultData);
  }

  @Override
  protected void onReceiveResult(int resultCode, Bundle resultData) {
    if (mReceiver != null) {
      mReceiver.onReceiveResult(resultCode, resultData);
    } else {
      Log.w(TAG, "Dropping result on floor for code " + resultCode + ": " + resultData.toString());
    }
  }
}
