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
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.view.View;
import android.widget.Toast;
import com.groundy.example.tasks.CancelableTask;
import com.groundy.example.tasks.RandomTimeTask;
import com.telly.groundy.Groundy;
import com.telly.groundy.GroundyManger;
import com.telly.groundy.example.R;
import com.telly.groundy.util.Bundler;
import java.util.Random;

import static android.widget.Toast.makeText;

public class CancelTaskExample extends Activity {
  private static final int GROUP_ID = 333;
  private View mBtnAddTask, mBtnCancelTask;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.cancel_example);
    mBtnAddTask = findViewById(R.id.send_random_task);
    mBtnAddTask.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mBtnAddTask.setEnabled(false);
        mBtnCancelTask.setEnabled(true);

        // configure task parameters
        int time = new Random().nextInt(10000);
        Bundle params = new Bundler().add(RandomTimeTask.KEY_ESTIMATED, time).build();
        makeText(CancelTaskExample.this, getString(R.string.task_will_take_x, time),
            Toast.LENGTH_SHORT).show();

        // queue task
        Groundy.create(CancelTaskExample.this, CancelableTask.class).receiver(resultReceiver)
            .group(GROUP_ID).params(params).queue();
      }
    });

    mBtnCancelTask = findViewById(R.id.cancel_random_task);
    mBtnCancelTask.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        mBtnCancelTask.setEnabled(false);
        GroundyManger.cancelTasks(CancelTaskExample.this, GROUP_ID, listener);
      }
    });
  }

  private final ResultReceiver resultReceiver = new ResultReceiver(new Handler()) {
    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
      super.onReceiveResult(resultCode, resultData);
      if (resultCode == Groundy.STATUS_FINISHED) {
        mBtnAddTask.setEnabled(true);
        makeText(CancelTaskExample.this, R.string.task_finished, Toast.LENGTH_LONG).show();
      }
    }
  };

  private final GroundyManger.CancelListener listener = new GroundyManger.CancelListener() {
    @Override public void onCancelResult(int groupId, boolean oneOrMoreCancelled) {
      if (oneOrMoreCancelled) {
        mBtnAddTask.setEnabled(true);
        mBtnCancelTask.setEnabled(false);
      } else {
        makeText(CancelTaskExample.this, R.string.couldnt_cancel_task, Toast.LENGTH_SHORT).show();
      }
    }
  };
}
