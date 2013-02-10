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

import android.util.Log;

/**
 * Platform log gateway
 * @hide
 */
class L {
    /**
     * Turns on of the enabled flag
     */
    public static boolean logEnabled = true;

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
