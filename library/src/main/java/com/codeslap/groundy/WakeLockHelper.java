package com.codeslap.groundy;

import android.content.Context;

class WakeLockHelper {
    private final Context mContext;

    WakeLockHelper(Context context) {
        mContext = context;
    }

    synchronized void acquire() {
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
