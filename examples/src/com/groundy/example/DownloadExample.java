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

package com.groundy.example;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.groundy.example.tasks.DownloadTask;
import com.telly.groundy.Groundy;
import com.telly.groundy.annotations.OnFailure;
import com.telly.groundy.annotations.OnProgress;
import com.telly.groundy.annotations.OnSuccess;
import com.telly.groundy.annotations.Param;
import com.telly.groundy.example.R;

public class DownloadExample extends Activity {

  private EditText mEditUrl;
  private ProgressDialog mProgressDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.download_example);

    mEditUrl = (EditText) findViewById(R.id.edit_url);

    findViewById(R.id.start_download).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mProgressDialog = new ProgressDialog(DownloadExample.this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

        String url = mEditUrl.getText().toString().trim();
        Groundy.create(DownloadTask.class)
            .callback(mCallback)
            .arg(DownloadTask.PARAM_URL, url)
            .queue(DownloadExample.this);
      }
    });
  }

  // a callback can be any kind of object :)
  private final Object mCallback = new Object() {
    @OnProgress(DownloadTask.class)
    public void onNiceProgress(@Param(Groundy.KEY_PROGRESS) int progress) {
      mProgressDialog.setProgress(progress);
    }

    @OnSuccess(DownloadTask.class)
    public void onBeautifulSuccess() {
      Toast.makeText(DownloadExample.this, R.string.file_downloaded, Toast.LENGTH_LONG);
      mProgressDialog.dismiss();
    }

    @OnFailure(DownloadTask.class)
    public void onTragedy(@Param(Groundy.CRASH_MESSAGE) String error) {
      Toast.makeText(DownloadExample.this, error, Toast.LENGTH_LONG).show();
      mProgressDialog.dismiss();
    }
  };
}
