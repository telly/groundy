/*
 * Copyright 2012 Twitvid Inc.
 * Copyright 2013 Cristian Castiblanco
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.groundy.example;

/**
 * @author Cristian Castiblanco <cristian@elhacker.net>
 */
public class ProgressItem {
    private long mCount;
    private int mProgress;
    private int mEstimated;

    public long getCount() {
        return mCount;
    }

    public int getEstimated() {
        return mEstimated;
    }

    public void setCount(long count) {
        mCount = count;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProgressItem that = (ProgressItem) o;

        if (mCount != that.mCount) return false;
        if (mProgress != that.mProgress) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (mCount ^ (mCount >>> 32));
        result = 31 * result + mProgress;
        return result;
    }

    @Override
    public String toString() {
        return "ProgressItem{" +
                "mCount=" + mCount +
                ", mProgress=" + mProgress +
                '}';
    }
}
