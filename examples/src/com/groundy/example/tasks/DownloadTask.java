/*
 * Copyright 2012 Twitvid Inc.
 * Copyright 2013 Cristian Castiblanco
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

package com.groundy.example.tasks;

import com.codeslap.groundy.GroundyTask;
import com.codeslap.groundy.util.DownloadUtils;

import java.io.File;

/**
 * @author Cristian Castiblanco <cristian@elhacker.net>
 */
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
