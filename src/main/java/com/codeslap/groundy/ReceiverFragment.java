package com.codeslap.groundy;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;

/**
 * @author cristian
 */
public abstract class ReceiverFragment extends Fragment implements DetachableResultReceiver.Receiver {
    public static final String TAG = ReceiverFragment.class.getSimpleName();

    private boolean mSyncing = false;
    private DetachableResultReceiver mReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mReceiver = new DetachableResultReceiver(new Handler());
        mReceiver.setReceiver(this);
    }

    /**
     * {@inheritDoc}
     */
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (getActivity() == null) {
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