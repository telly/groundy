package com.groundy.example.tasks;

import com.codeslap.groundy.GroundyTask;
import com.codeslap.groundy.util.DownloadUtils;

import java.io.File;

public class DownloadTask extends GroundyTask {
    public static final String PARAM_URL = "com.groundy.example.param.URL";

    @Override
    protected boolean doInBackground() {
        try {
            String url = getParameters().getString(PARAM_URL);
            File dest = new File(getContext().getFilesDir(), new File(url).getName());
            DownloadUtils.downloadFile(getContext(), url, dest, DownloadUtils.getDownloadListenerForTask(this));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
