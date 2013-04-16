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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import com.groundy.example.tasks.CancelableTask;
import com.groundy.example.tasks.RandomTimeTask;
import com.telly.groundy.Groundy;
import com.telly.groundy.GroundyManager;
import com.telly.groundy.GroundyService;
import com.telly.groundy.example.R;
import com.telly.groundy.util.Bundler;

import java.util.Random;
import java.util.Set;

import static android.widget.Toast.makeText;

public class CancelTaskExample extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener {
  public static final int BLUE_TASKS = 333;
  public static final int ORANGE_TASKS = 444;
  private static final int FOO_CANCEL_REASON = 45;
  private CancelProgressAdapter mAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.cancel_example);

    ListView listView = (ListView) findViewById(R.id.list);
    listView.setOnItemClickListener(this);
    mAdapter = new CancelProgressAdapter(this);
    listView.setAdapter(mAdapter);

    findViewById(R.id.queue_blue_task).setOnClickListener(this);
    findViewById(R.id.queue_orange_task).setOnClickListener(this);
    findViewById(R.id.cancel_blue_task).setOnClickListener(this);
    findViewById(R.id.cancel_orange_task).setOnClickListener(this);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.queue_blue_task:
        queueTask(BLUE_TASKS);
        break;
      case R.id.queue_orange_task:
        queueTask(ORANGE_TASKS);
        break;
      case R.id.cancel_blue_task:
        cancelTasks(BLUE_TASKS);
        break;
      case R.id.cancel_orange_task:
        cancelTasks(ORANGE_TASKS);
        break;
    }
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    GroundyManager.cancelTaskById(this, id, new GroundyManager.SingleCancelListener() {
        @Override
        public void onCancelResult(long id, int result) {
            ProgressItem item = findItem(id);
            switch (result) {
                case GroundyService.INTERRUPTED:
                    item.setState(ProgressItem.INTERRUPTED);
                    break;
                case GroundyService.NOT_EXECUTED:
                    item.setState(ProgressItem.CANCELLED);
                    break;
            }
            mAdapter.notifyDataSetChanged();
        }
    });
  }

  private void cancelTasks(int taskGroup) {
    GroundyManager.cancelTasksByGroup(this, taskGroup, FOO_CANCEL_REASON, listener);
  }

  private long queueTask(int groupId) {
    // configure task parameters
    int time = new Random().nextInt(10000);
    Bundle params = new Bundler().add(RandomTimeTask.KEY_ESTIMATED, time).build();

    // queue task
    long taskId = Groundy.create(this, CancelableTask.class)
      .receiver(resultReceiver)
      .group(groupId)
      .params(params)
      .queue();

    ProgressItem progressItem = new ProgressItem();
    progressItem.setId(taskId);
    progressItem.setProgress(0);
    progressItem.setEstimated(time / 1000);
    progressItem.setColor(groupId);
    mAdapter.addItem(progressItem);
    return taskId;
  }

  private ProgressItem findItem(long id) {
    for (ProgressItem progressItem : mAdapter.getItems()) {
      if (id == progressItem.getId()) {
        return progressItem;
      }
    }
    return null;
  }

  private final ResultReceiver resultReceiver = new ResultReceiver(new Handler()) {
    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
      super.onReceiveResult(resultCode, resultData);
      long id = resultData.getLong(Groundy.KEY_TASK_ID);
      ProgressItem item = findItem(id);
      if (resultCode == Groundy.STATUS_PROGRESS) {
        int progress = resultData.getInt(Groundy.KEY_PROGRESS);
        item.setProgress(progress);
      } else if (resultCode == Groundy.STATUS_ERROR && resultData.getInt(Groundy.KEY_CANCEL_REASON,
        0) == FOO_CANCEL_REASON) {
        item.setState(ProgressItem.INTERRUPTED);
      } else if (resultCode == Groundy.STATUS_FINISHED) {
        item.setState(ProgressItem.DONE);
      }
      mAdapter.notifyDataSetChanged();
    }
  };

  private final GroundyManager.CancelListener listener = new GroundyManager.CancelListener() {
    @Override
    public void onCancelResult(int groupId, GroundyService.CancelGroupResponse cancelledTasks) {
      if (cancelledTasks == null) {
        makeText(CancelTaskExample.this, R.string.couldnt_cancel_task, Toast.LENGTH_SHORT).show();
      } else {
        Set<Long> notExecutedTasks = cancelledTasks.getNotExecutedTasks();
        int interruptedTasks = cancelledTasks.getInterruptedTasks().size();
        int notExecuted = notExecutedTasks.size();
        String message = getString(R.string.tasks_interrupted, interruptedTasks, notExecuted);
        makeText(CancelTaskExample.this, message, Toast.LENGTH_LONG).show();

        for (Long taskId : notExecutedTasks) {
          ProgressItem item = findItem(taskId);
          item.setState(ProgressItem.CANCELLED);
        }
      }
    }
  };
}
