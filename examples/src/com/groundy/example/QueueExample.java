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
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import com.groundy.example.tasks.RandomTimeTask;
import com.telly.groundy.Groundy;
import com.telly.groundy.TaskHandler;
import com.telly.groundy.TaskHandler;
import com.telly.groundy.annotations.OnProgress;
import com.telly.groundy.annotations.Param;
import com.telly.groundy.example.R;
import com.telly.groundy.util.Bundler;
import java.util.Random;

public class QueueExample extends Activity {

  private ProgressAdapter mAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
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

        TaskHandler taskHandler = processTask(
            new Bundler().add(RandomTimeTask.KEY_ESTIMATED, time).build());

        ProgressItem progressItem = new ProgressItem();
        progressItem.setTaskProxy(taskHandler);
        progressItem.setProgress(0);
        progressItem.setEstimated(time / 1000);
        mAdapter.addItem(progressItem);
      }
    });
  }

  protected TaskHandler processTask(Bundle params) {
    return Groundy.create(RandomTimeTask.class).params(params).callback(this).queue(this);
  }

  @OnProgress(RandomTimeTask.class)
  public void onProgress(@Param(Groundy.TASK_ID) long taskId,
                         @Param(Groundy.KEY_PROGRESS) int progress) {
    findItem(taskId).setProgress(progress);
    mAdapter.notifyDataSetChanged();
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
