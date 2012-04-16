/*
 * Copyright 2012 CodeSlap
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

package com.codeslap.groundy;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.Fragment;

/**
 * @author cristian
 */
public abstract class ReceiverFragment extends Fragment implements DetachableResultReceiver.Receiver {
    public static final String TAG = ReceiverFragment.class.getSimpleName();

    private boolean mSyncing = false;
    private DetachableResultReceiver mReceiver;

    public ReceiverFragment() {
        mReceiver = new DetachableResultReceiver(new Handler());
        mReceiver.setReceiver(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    /**
     * {@inheritDoc}
     */
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (getActivity() == null) {
            return;
        }
        if (!validateResultState(resultCode, resultData)) {
            return;
        }

        switch (resultCode) {
            case Groundy.STATUS_RUNNING: {
                mSyncing = true;
                onRunning(resultData);
                break;
            }
            case Groundy.STATUS_FINISHED: {
                mSyncing = false;
                onFinished(resultData);
                break;
            }
            case Groundy.STATUS_ERROR: {
                mSyncing = false;
                onError(resultData);
                break;
            }
        }
        onProgressChanged(mSyncing);
    }

    /**
     * Override this to determine whether on result methods should be called.
     * @return true if they must be called; false otherwise
     */
    protected boolean validateResultState(int resultCode, Bundle resultData) {
        return true;
    }

    public ResultReceiver getReceiver() {
        return mReceiver;
    }

    protected void onRunning(Bundle resultData) {
    }

    protected void onFinished(Bundle resultData) {
    }

    protected void onError(Bundle resultData) {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        onProgressChanged(mSyncing);
    }

    protected abstract void onProgressChanged(boolean running);
}