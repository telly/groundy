package com.codeslap.groundy;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

public class GroundyService extends IntentService {
    /**
     * Log tag
     */
    private static final String TAG = GroundyService.class.getSimpleName();

    private final WakeLockHelper mWakeLockHelper;

    /**
     * Default constructor creates a service named {@link GroundyService#TAG}
     */
    public GroundyService() {
        super(TAG);
        mWakeLockHelper = new WakeLockHelper(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();
        extras = (extras == null) ? Bundle.EMPTY : extras;

        // Pessimistic by default
        int resultCode = GroundyConstants.RESULT_FAIL;
        Bundle resultData = Bundle.EMPTY;

        ResultReceiver receiver = (ResultReceiver) extras.get(GroundyConstants.KEY_RECEIVER);
        if (receiver != null) {
            receiver.send(GroundyConstants.STATUS_RUNNING, Bundle.EMPTY);
        }

        //This should be be the most common action
        CallResolver resolver = CallResolverFactory.get(action, this);
        L.d(TAG, "resolver is " + resolver);
        if (resolver != null) {
            resolver.setReceiver(receiver);
            resolver.setParameters(extras.getBundle(GroundyConstants.KEY_PARAMETERS));
            boolean requiresWifi = resolver.requiresWifi();
            if (requiresWifi) {
                mWakeLockHelper.adquire();
            }
            resolver.execute();
            if (requiresWifi) {
                mWakeLockHelper.release();
            }
            resultCode = resolver.getResultCode();
            resultData = resolver.getResultData();
        }

        //Lets try to send back the response
        if (receiver != null) {
            receiver.send(resultCode, resultData);
        }
    }

    static class WakeLockHelper {
        private final Context mContext;

        WakeLockHelper(Context context) {
            mContext = context;
        }

        synchronized void adquire() {
            DeviceStatus.keepCpuAwake(mContext, true);
            if (DeviceStatus.isCurrentConnectionWifi(mContext)) {
                DeviceStatus.keepWiFiOn(mContext, true);
            }
        }

        synchronized void release() {
            DeviceStatus.keepWiFiOn(mContext, false);
            DeviceStatus.keepCpuAwake(mContext, false);
        }
    }
}
