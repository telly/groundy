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

package com.groundy.example;

import com.telly.groundy.TaskHandler;
import com.telly.groundy.TaskHandler;

public class ProgressItem {
  public static final int DEFAULT = 0;
  public static final int INTERRUPTED = 1;
  public static final int CANCELLED = 2;
  public static final int DONE = 3;

  private TaskHandler mTaskHandler;
  private int mProgress;
  private int mEstimated;
  private int mState = DEFAULT;
  private int mColor;

  public long getId() {
    return mTaskHandler.getTaskId();
  }

  public TaskHandler getTaskProxy() {
    return mTaskHandler;
  }

  public int getEstimated() {
    return mEstimated;
  }

  public void setTaskProxy(TaskHandler taskHandler) {
    mTaskHandler = taskHandler;
  }

  public int getProgress() {
    return mProgress;
  }

  public void setEstimated(int estimated) {
    mEstimated = estimated;
  }

  public void setProgress(int progress) {
    mProgress = progress;
  }

  public void setState(int cancelled) {
    mState = cancelled;
  }

  public int getState() {
    return mState;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProgressItem that = (ProgressItem) o;

    if (mTaskHandler != null ? !mTaskHandler.equals(that.mTaskHandler) : that.mTaskHandler != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    return mTaskHandler != null ? mTaskHandler.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "ProgressItem{" +
        "mTaskHandler=" + mTaskHandler +
        ", mProgress=" + mProgress +
        '}';
  }

  public void setColor(int color) {
    mColor = color;
  }

  public int getColor() {
    return mColor;
  }
}
