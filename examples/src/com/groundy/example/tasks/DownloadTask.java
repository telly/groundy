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

package com.groundy.example.tasks;

import com.telly.groundy.Failed;
import com.telly.groundy.GroundyTask;
import com.telly.groundy.Succeeded;
import com.telly.groundy.TaskResult;
import com.telly.groundy.util.DownloadUtils;
import java.io.File;

public class DownloadTask extends GroundyTask {
  public static final String PARAM_URL = "com.groundy.example.param.URL";

  @Override
  protected TaskResult doInBackground() {
    try {
      String url = getParameters().getString(PARAM_URL);
      File dest = new File(getContext().getFilesDir(), new File(url).getName());
      DownloadUtils.downloadFile(getContext(), url, dest,
          DownloadUtils.getDownloadListenerForTask(this));
      return new Succeeded();
    } catch (Exception e) {
      return new Failed();
    }
  }
}
