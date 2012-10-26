/*
 * Copyright 2011 Google Inc.
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

package com.codeslap.groundy.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;

/**
 * A class that handles some common activity-related functionality in the app,
 * such as setting up the action bar. This class provides functioanlity useful
 * for both phones and tablets, and does not require any Android 3.0-specific
 * features.
 */
public class ActivityHelper {
    protected Activity mActivity;

    /**
     * Factory method for creating {@link ActivityHelper} objects for a given
     * activity. Depending on which device the app is running, either a basic
     * helper or Honeycomb-specific helper will be returned.
     */
    public static ActivityHelper createInstance(Activity activity) {
        return Topy.isHoneycombOrHigher() ? new ActivityHelperHoneycomb(activity) : new ActivityHelper(activity);
    }

    protected ActivityHelper(Activity activity) {
        mActivity = activity;
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            goHome();
            return true;
        }
        return false;
    }

    /**
     * Invoke "home" action, returning to ShowsActivity.
     */
    public void goHome() {
        Intent launcherIntent = mActivity.getPackageManager().getLaunchIntentForPackage(mActivity.getPackageName());
        Class<?> launcherClass = null;
        try {
            launcherClass = Class.forName(launcherIntent.getComponent().getClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (launcherClass != null && launcherClass.isInstance(mActivity)) {
            return;
        }

        launcherIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mActivity.startActivity(launcherIntent);
    }
}