package com.codeslap.groundy;

import android.util.Log;

/**
 * Platform log gateway
 * @hide
 */
class L {
    /**
     * Turns on of the enabled flag
     */
    public static final boolean logEnabled = true;

    /**
     * Non instanceable constant class
     */
    private L() {
    }

    /**
     * Sends a debug message to the log
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
     * Send an error message to the log
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
     * Send an error message to the log
     *
     * @param tag Tag to use
     * @param msg Message to send
     * @param tr  Throwable to dump
     */
    public static void e(String tag, String msg, Throwable tr) {
        if (logEnabled) {
            Log.e(tag, msg, tr);
        }
    }

}
