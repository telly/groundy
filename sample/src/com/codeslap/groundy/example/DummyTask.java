package com.codeslap.groundy.example;

import android.util.Log;
import com.codeslap.groundy.GroundyTask;

public class DummyTask extends GroundyTask {
    private static final String TAG = DummyTask.class.getSimpleName();

    @Override
    protected boolean doInBackground() {
        Log.v(TAG, "Working hard " + this + ", quitting? " + isQuitting());
        pause();
        if (isQuitting()) {
            stop();
            return true;
        }

        Log.v(TAG, "Still working hard " + this + ", quitting? " + isQuitting());
        pause();
        if (isQuitting()) {
            stop();
            return true;
        }

        System.out.println("Worked so hard " + this);
        return true;
    }

    private void stop() {
        Log.v(TAG, "Stopping gracefully " + this);
    }

    private void pause() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
