package com.groundy.example.tasks;

import android.util.Log;
import com.codeslap.groundy.GroundyTask;

public class DummyGroundyTask extends GroundyTask {
    private static final String TAG = "GroundyTask";
    public static boolean x = true;
    private boolean mFoo;
    private String mName;

    @Override
    protected boolean doInBackground() {
        mName = getStringParam("name");
        mFoo = x;
        if (x) {
            x = false;
        }
        Log.v(TAG, "Working hard " + mName + ", id " + getStartId());
        pause();
        if (isQuitting()) {
            stop();
            return true;
        }

        Log.v(TAG, "Still working hard " + mName + ", id " + getStartId());
        pause();
        if (isQuitting()) {
            stop();
            return true;
        }

        Log.v(TAG, "Worked so hard " + mName + ", id " + getStartId());
        return true;
    }

    private void stop() {
        Log.v(TAG, "Stopping gracefully " + mName + ", id " + getStartId());
    }

    private void pause() {
        try {
            if (mFoo) {
                Thread.sleep(12000);
            } else {
            Thread.sleep(4000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
