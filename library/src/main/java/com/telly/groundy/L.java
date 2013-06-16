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

import android.util.Log;

/**
 * Platform log gateway.
 *
 * @author Evelio Tarazona <evelio@telly.com>
 */
final class L {
  /** Turns on/off debugging messages. */
  public static boolean logEnabled = true;

  /** Non instanceable constant class. */
  private L() {
  }

  /**
   * Sends a debug message to the log.
   *
   * @param tag Tag to use
   * @param msg Message to send
   */
  public static void d(String tag, String msg) {
    if (logEnabled) {
      Log.d(tag, msg);
    }
  }

  /**
   * Send an error message to the log.
   *
   * @param tag Tag to use
   * @param msg Message to send
   */
  public static void e(String tag, String msg) {
    if (logEnabled) {
      Log.e(tag, msg);
    }
  }

  /**
   * Send an error message to the log.
   *
   * @param tag Tag to use
   * @param msg Message to send
   * @param tr Throwable to dump
   */
  public static void e(String tag, String msg, Throwable tr) {
    if (logEnabled) {
      Log.e(tag, msg, tr);
    }
  }
}
