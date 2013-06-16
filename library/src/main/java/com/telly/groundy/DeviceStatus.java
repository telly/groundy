/**
 * Copyright Telly, Inc. and other Groundy contributors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.telly.groundy;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

final class DeviceStatus {
  private static final String TAG = "groundy";

  /** Non instance constant class. */
  private DeviceStatus() {
  }

  /**
   * Checks whether there's a network connection.
   *
   * @param context Context to use
   * @return true if there's an active network connection, false otherwise
   */
  public static boolean isOnline(Context context) {
    ConnectivityManager cm =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    if (cm == null) {
      return false;
    }
    NetworkInfo info = cm.getActiveNetworkInfo();
    return info != null && info.isConnectedOrConnecting();
  }

  /**
   * Check if current connection is Wi-Fi.
   *
   * @param context Context to use
   * @return true if current connection is Wi-Fi false otherwise
   */
  public static boolean isCurrentConnectionWifi(Context context) {
    ConnectivityManager cm =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    if (cm == null) {
      return false;
    }
    NetworkInfo info = cm.getActiveNetworkInfo();
    return info != null && info.getType() == ConnectivityManager.TYPE_WIFI;
  }

  /** CPU wake lock unique instance. */
  private static WakeLock cpuWakeLock;

  /**
   * Register a wake lock to power management in the device.
   *
   * @param context Context to use
   * @param awake if true the device cpu will keep awake until false is called back. if true is
   * passed several times only the first time after a false call will take effect,
   * also if false is passed and previously the cpu was not turned on (true call)
   * does nothing.
   */
  public static void keepCpuAwake(Context context, boolean awake) {
    if (cpuWakeLock == null) {
      PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
      if (pm != null) {
        cpuWakeLock =
            pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
        cpuWakeLock.setReferenceCounted(true);
      }
    }
    if (cpuWakeLock != null) { //May be null if pm is null
      if (awake) {
        cpuWakeLock.acquire();
        L.d(TAG, "Adquired CPU lock");
      } else if (cpuWakeLock.isHeld()) {
        cpuWakeLock.release();
        L.d(TAG, "Released CPU lock");
      }
    }
  }

  /** WiFi lock unique instance. */
  private static WifiLock wifiLock;

  /**
   * Register a WiFi lock to WiFi management in the device.
   *
   * @param context Context to use
   * @param on if true the device WiFi radio will keep awake until false is called back. if
   * true is passed several times only the first time after a false call will take
   * effect, also if false is passed and previously the WiFi radio was not turned on
   * (true call) does nothing.
   */
  public static void keepWiFiOn(Context context, boolean on) {
    if (wifiLock == null) {
      WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
      if (wm != null) {
        wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, TAG);
        wifiLock.setReferenceCounted(true);
      }
    }
    if (wifiLock != null) { // May be null if wm is null
      if (on) {
        wifiLock.acquire();
        L.d(TAG, "Adquired WiFi lock");
      } else if (wifiLock.isHeld()) {
        wifiLock.release();
        L.d(TAG, "Released WiFi lock");
      }
    }
  }
}
