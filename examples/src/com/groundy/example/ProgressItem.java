/*
 * Copyright 2013 Telly Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.groundy.example;

public class ProgressItem {
  public static final int DEFAULT = 0;
  public static final int INTERRUPTED = 1;
  public static final int CANCELLED = 2;
  public static final int DONE = 3;
  private long mId;
  private int mProgress;
  private int mEstimated;
  private int mState = DEFAULT;

  public long getId() {
    return mId;
  }

  public int getEstimated() {
    return mEstimated;
  }

  public void setId(long id) {
    mId = id;
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

    if (mId != that.mId) return false;
    if (mProgress != that.mProgress) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = (int) (mId ^ (mId >>> 32));
    result = 31 * result + mProgress;
    return result;
  }

  @Override
  public String toString() {
    return "ProgressItem{" +
        "mId=" + mId +
        ", mProgress=" + mProgress +
        '}';
  }
}
