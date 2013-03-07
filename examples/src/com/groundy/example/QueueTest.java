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
import com.telly.groundy.example.R;
import com.groundy.example.tasks.RandomTimeTask;
import com.telly.groundy.Groundy;
import com.telly.groundy.util.Bundler;

import java.util.Random;

/**
 * @author Cristian Castiblanco <cristian@elhacker.net>
 */
public class QueueTest extends Activity {

  protected MyReceiver mReceiver;
  private int mCounter = 1;
  private Button mBtnAddTask;
  private ProgressAdapter mAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mReceiver = new MyReceiver();
    setContentView(R.layout.queue_example);

    ListView listView = (ListView) findViewById(R.id.list);
    mBtnAddTask = (Button) findViewById(R.id.btn_queue);
    mAdapter = new ProgressAdapter(this);
    listView.setAdapter(mAdapter);

    mBtnAddTask.setText(getString(R.string.next_task_counter, mCounter));
    mBtnAddTask.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        int count = mCounter++;
        int time = new Random().nextInt(10000);
        if (time < 1000) {
          time = 1000;
        }

        processTask(new Bundler()
            .add(RandomTimeTask.KEY_COUNT, count)
            .add(RandomTimeTask.KEY_ESTIMATED, time)
            .build());

        mBtnAddTask.setText(getString(R.string.next_task_counter, mCounter));

        ProgressItem progressItem = new ProgressItem();
        progressItem.setCount(count);
        progressItem.setProgress(0);
        progressItem.setEstimated(time / 1000);
        mAdapter.addItem(progressItem);
      }
    });
  }

  protected void processTask(Bundle params) {
    Groundy.create(this, RandomTimeTask.class)
        .params(params)
        .receiver(mReceiver)
        .queue();
  }

  private class MyReceiver extends ResultReceiver {
    public MyReceiver() {
      super(new Handler());
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
      super.onReceiveResult(resultCode, resultData);
      if (resultCode == Groundy.STATUS_PROGRESS) {
        int count = resultData.getInt(RandomTimeTask.KEY_COUNT);
        int progress = resultData.getInt(Groundy.KEY_PROGRESS);
        findItem(count).setProgress(progress);
        mAdapter.notifyDataSetChanged();
      }
    }

    private ProgressItem findItem(int count) {
      for (ProgressItem progressItem : mAdapter.getItems()) {
        if (count == progressItem.getCount()) {
          return progressItem;
        }
      }
      return null;
    }
  }
}
