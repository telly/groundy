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
import android.os.Parcelable;

/** Allows to get information about, cancel it, */
public interface TaskProxy extends Parcelable {

  /** @return the associated task id */
  long getTaskId();

  /**
   * Replace the current callback handlers with the specified ones.
   *
   * @param callbackHandlers an array of callback handlers
   */
  void updateCallbackHandlers(Object... callbackHandlers);

  /**
   * Called internally when a task is finished, either after a success, fail or cancellation. You
   * should never call this method directly (poor design; shame on me).
   */
  void onTaskEnded();

  /**
   * Cancels this task if possible.
   *
   * @param context        used to communicate with the groundy service
   * @param reason         the reason to cancel this task if any.
   * @param cancelListener a listener to get the result of the task cancellation
   */
  void cancel(Context context, int reason, GroundyManager.SingleCancelListener cancelListener);

  /** Removes this tasks callback handlers. */
  void clearCallbackHandlers();

  /** @return true if the task was already ended */
  boolean shouldRecycle();

  /**
   * Add more callback handlers to this task
   *
   * @param handlers an array of callback handlers to add
   */
  void appendCallbackHandlers(Object... handlers);

  /**
   * Remove the specified callback handlers from this task
   *
   * @param handlers the callback handlers to remove
   */
  void removeCallbackHandlers(Object... handlers);
}
