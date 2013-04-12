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
import android.widget.Button;
import android.widget.ListView;
import com.groundy.example.tasks.RandomTimeTask;
import com.telly.groundy.Groundy;
import com.telly.groundy.example.R;
import com.telly.groundy.util.Bundler;

import java.util.Random;

public class QueueExample extends Activity {

  protected MyReceiver mReceiver;
  private ProgressAdapter mAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mReceiver = new MyReceiver();
    setContentView(R.layout.queue_example);

    ListView listView = (ListView) findViewById(R.id.list);
    mAdapter = new ProgressAdapter(this);
    listView.setAdapter(mAdapter);

    Button btnAddTask = (Button) findViewById(R.id.btn_queue);
    btnAddTask.setText(R.string.queue_task);
    btnAddTask.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        int time = new Random().nextInt(10000);
        if (time < 1000) {
          time = 1000;
        }

        long id = processTask(new Bundler().add(RandomTimeTask.KEY_ESTIMATED, time)
          .build());

        ProgressItem progressItem = new ProgressItem();
        progressItem.setId(id);
        progressItem.setProgress(0);
        progressItem.setEstimated(time / 1000);
        mAdapter.addItem(progressItem);
      }
    });
  }

  protected long processTask(Bundle params) {
    return Groundy.create(this, RandomTimeTask.class).params(params).receiver(mReceiver).queue();
  }

  private class MyReceiver extends ResultReceiver {
    public MyReceiver() {
      super(new Handler());
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
      super.onReceiveResult(resultCode, resultData);
      if (resultCode == Groundy.STATUS_PROGRESS) {
        long count = resultData.getLong(Groundy.KEY_TASK_ID);
        int progress = resultData.getInt(Groundy.KEY_PROGRESS);
        findItem(count).setProgress(progress);
        mAdapter.notifyDataSetChanged();
      }
    }

    private ProgressItem findItem(long count) {
      for (ProgressItem progressItem : mAdapter.getItems()) {
        if (count == progressItem.getId()) {
          return progressItem;
        }
      }
      return null;
    }
  }
}
