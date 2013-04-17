/*
 * Copyright 2013 Telly Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.groundy.example;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.groundy.example.tasks.DownloadTask;
import com.telly.groundy.Groundy;
import com.telly.groundy.example.R;
import com.telly.groundy.util.Bundler;

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
        Bundle extras = new Bundler().add(DownloadTask.PARAM_URL, url).build();
        Groundy.create(DownloadExample.this, DownloadTask.class).receiver(mReceiver).params(extras)
            .queue();
      }
    });
  }

  private final ResultReceiver mReceiver = new ResultReceiver(new Handler()) {
    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
      super.onReceiveResult(resultCode, resultData);
      switch (resultCode) {
        case Groundy.STATUS_PROGRESS:
          mProgressDialog.setProgress(resultData.getInt(Groundy.KEY_PROGRESS));
          break;
        case Groundy.STATUS_FINISHED:
          Toast.makeText(DownloadExample.this, R.string.file_downloaded, Toast.LENGTH_LONG);
          mProgressDialog.dismiss();
          break;
        case Groundy.STATUS_ERROR:
          Toast.makeText(DownloadExample.this, resultData.getString(Groundy.KEY_ERROR),
              Toast.LENGTH_LONG).show();
          mProgressDialog.dismiss();
          break;
      }
    }
  };
}
