package com.codeslap.groundy;

import android.content.Context;
import android.os.Bundle;
import android.os.ResultReceiver;

import java.io.Serializable;

/**
 * A base class for Api Call resolvers
 *
 * @author evelio
 * @version 1.0
 */
public abstract class CallResolver {
    private final Context mContext;
    private int mResultCode;
    private Bundle mResultData;
    private Bundle mParameters;
    private ResultReceiver mReceiver;

    /**
     * Creates a CallResolver composed of
     *
     * @param context
     */
    public CallResolver(Context context) {
        mContext = context;
        //Pessimistic by default
        setResultCode(GroundyConstants.RESULT_FAIL);
        setResultData(new Bundle());
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

    /**
     * @param resultData the resultData to set
     */
    protected void setResultData(Bundle resultData) {
        mResultData = resultData;
    }

    /**
     * Set the call resolver result
     *
     * @param result
     */
    protected void setResult(Serializable result) {
        mResultData.putSerializable(GroundyConstants.KEY_RESULT, result);
    }

    /**
     * @return Context instance to use
     */
    protected final Context getContext() {
        return mContext;
    }

    /**
     * Does its magic
     */
    protected final void execute() {
        if (requiresUpdate() && isOnline()) {
            try {
                updateData();
            } catch (Exception e) {
                setResultCode(GroundyConstants.RESULT_FAIL);
                mResultData.putSerializable(GroundyConstants.KEY_RESULT, e.getMessage());
                return;
            }
        }
        prepareResult();
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
    void setParameters(Bundle parameters) {
        this.mParameters = parameters;
    }

    /**
     * @return the parameters
     */
    protected Bundle getParameters() {
        return mParameters;
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

    protected boolean requiresWifi() {
        return false;
    }

    /**
     * This must fetch and persist data. If an error occurs,
     * it must throws an error so that it can be catch by the executer
     *
     * @throws Exception
     */
    protected abstract void updateData();

    /**
     * This is responsible of preparing the result. Here you will retrieve info from a persistence source,
     * and call the {@link #setResultCode(int)} and {@link #setResult(java.io.Serializable)}
     */
    protected abstract void prepareResult();
}
