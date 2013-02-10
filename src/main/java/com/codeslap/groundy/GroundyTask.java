/*
 * Copyright 2013 CodeSlap
 *
 *   Authors: Cristian C. <cristian@elhacker.net>
 *            Evelio T.   <eveliotc@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeslap.groundy;

import android.content.Context;
import android.os.Bundle;
import android.os.ResultReceiver;

/**
 * Implementation of this class get executed by the {@link GroundyService}
 *
 * @author evelio
 * @author cristian
 * @version 1.1
 */
public abstract class GroundyTask {
    public static final int CANCEL_ALL = -1;
    public static final int SERVICE_DESTROYED = -2;
    public static final int CANCEL_INDIVIDUAL = -3;
    private Context mContext;
    private int mResultCode = Groundy.STATUS_ERROR; // Pessimistic by default
    private final Bundle mResultData = new Bundle();
    private final Bundle mParameters = new Bundle();
    private ResultReceiver mReceiver;
    private volatile int mQuittingReason = 0;

    /**
     * Creates a GroundyTask composed of
     */
    public GroundyTask() {
        //Pessimistic by default
        setResultCode(Groundy.STATUS_ERROR);
    }

    /**
     * @return the resultCode
     */
    protected int getResultCode() {
        return mResultCode;
    }

    /**
     * @param resultCode the resultCode to set
     */
    protected void setResultCode(int resultCode) {
        mResultCode = resultCode;
    }

    /**
     * @return the resultData
     */
    protected Bundle getResultData() {
        return mResultData;
    }

    final void setContext(Context context) {
        mContext = context;
    }

    protected final Context getContext() {
        return mContext;
    }

    /**
     * Does its magic
     */
    protected final void execute() {
        try {
            boolean success = doInBackground();
            if (success) {
                setResultCode(Groundy.STATUS_FINISHED);
            } else {
                setResultCode(Groundy.STATUS_ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
            setResultCode(Groundy.STATUS_ERROR);
            mResultData.putString(Groundy.KEY_ERROR, String.valueOf(e.getMessage()));
            return;
        }
    }

    /**
     * Determinate if there is Internet connection
     *
     * @return true if Online
     *         false otherwise
     */
    protected boolean isOnline() {
        return DeviceStatus.isOnline(mContext);
    }

    /**
     * @param parameters the parameters to set
     */
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

    /**
     * Adds a string to the result bundle
     *
     * @param key   param key
     * @param value param value
     * @return object itself
     */
    protected GroundyTask addStringResult(String key, String value) {
        mResultData.putString(key, value);
        return this;
    }

    /**
     * Adds a long to the result bundle
     *
     * @param key   param key
     * @param value param value
     * @return object itself
     */
    protected GroundyTask addLongResult(String key, long value) {
        mResultData.putLong(key, value);
        return this;
    }

    /**
     * Adds an int to the result bundle
     *
     * @param key   param key
     * @param value param value
     * @return object itself
     */
    protected GroundyTask addIntResult(String key, int value) {
        mResultData.putInt(key, value);
        return this;
    }

    /**
     * Adds a boolean to the result bundle
     *
     * @param key   param key
     * @param value param value
     * @return object itself
     */
    protected GroundyTask addIntResult(String key, boolean value) {
        mResultData.putBoolean(key, value);
        return this;
    }

    /**
     * Indicates whatever if local data is fresh enough or not
     *
     * @return <code>true</code> if local data is not fresh
     *         <code>false</code> otherwise
     *         Note: default implementation always returns <code>true</code>
     *         Subclasses should override it according.
     */
    protected boolean requiresUpdate() {
        //By default it is hungry for updates
        return true;
    }

    void setReceiver(ResultReceiver receiver) {
        this.mReceiver = receiver;
    }

    protected ResultReceiver getReceiver() {
        return mReceiver;
    }

    /**
     * This must be checked every time you want to check whether
     * the task is in quitting state. In such cases you must make
     * sure the task is stopped immediately. To know the reason
     * causing the task to be quited use the
     * {@link GroundyTask#getQuittingReason()} method.
     *
     * @return true if the groundy task is in quitting state
     */
    protected boolean isQuitting() {
        return mQuittingReason != 0;
    }

    /**
     * This can be either {@link GroundyTask#CANCEL_ALL} or
     * {@link GroundyTask#SERVICE_DESTROYED} or some other custom reason
     *
     * @return quitting reason
     */
    protected int getQuittingReason() {
        return mQuittingReason;
    }

    /**
     * Mark this task as quitting
     *
     * @param reason the reason to stop this task
     */
    void stopTask(int reason) {
        mQuittingReason = reason;
    }

    /**
     * Prepare and sends a progress update to the current receiver.
     * Result code used is {@link Groundy#STATUS_PROGRESS} and it
     * will contain a bundle with an integer extra called {@link Groundy#KEY_PROGRESS}
     *
     * @param progress percentage to send to receiver
     */
    public void updateProgress(int progress) {
        if (mReceiver == null) {
            return;
        }
        Bundle resultData = new Bundle();
        resultData.putInt(Groundy.KEY_PROGRESS, progress);
        mReceiver.send(Groundy.STATUS_PROGRESS, resultData);
    }

    protected boolean keepWifiOn() {
        return false;
    }

    /**
     * Override this if you want to cache the GroundyTask instance. Do it only if you are
     * sure that {@link GroundyTask#doInBackground()} method won't need a fresh instance each
     * time they are executed.
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
    protected abstract boolean doInBackground();
}
