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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import com.groundy.example.tasks.CancelableTask;
import com.groundy.example.tasks.RandomTimeTask;
import com.telly.groundy.Groundy;
import com.telly.groundy.GroundyManager;
import com.telly.groundy.GroundyService;
import com.telly.groundy.TaskHandler;
import com.telly.groundy.annotations.OnCancel;
import com.telly.groundy.annotations.OnProgress;
import com.telly.groundy.annotations.OnSuccess;
import com.telly.groundy.annotations.Param;
import com.telly.groundy.example.R;
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
    final ProgressItem item = findItem(id);
    item.getTaskProxy().cancel(this, FOO_CANCEL_REASON, new GroundyManager.SingleCancelListener() {
      @Override
      public void onCancelResult(long id, int result) {
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

  private void queueTask(int groupId) {
    // configure value parameters
    int time = new Random().nextInt(10000);

    // queue value
    TaskHandler taskHandler = Groundy.create(CancelableTask.class)
        .callback(this)
        .group(groupId)
        .arg(RandomTimeTask.KEY_ESTIMATED, time)
        .queue(CancelTaskExample.this);

    ProgressItem progressItem = new ProgressItem();
    progressItem.setTaskProxy(taskHandler);
    progressItem.setProgress(0);
    progressItem.setEstimated(time / 1000);
    progressItem.setColor(groupId);
    mAdapter.addItem(progressItem);
  }

  private ProgressItem findItem(long id) {
    for (ProgressItem progressItem : mAdapter.getItems()) {
      if (id == progressItem.getId()) {
        return progressItem;
      }
    }
    return null;
  }

  @OnProgress(CancelableTask.class)
  public void onProgressUpdate(@Param(Groundy.TASK_ID) long id,
                               @Param(Groundy.PROGRESS) int progress) {
    findItem(id).setProgress(progress);
    mAdapter.notifyDataSetChanged();
  }

  @OnCancel(CancelableTask.class)
  public void onCancel(@Param(Groundy.TASK_ID) long id) {
    findItem(id).setState(ProgressItem.INTERRUPTED);
    mAdapter.notifyDataSetChanged();
  }

  @OnSuccess(CancelableTask.class)
  public void onSuccess(@Param(Groundy.TASK_ID) long id) {
    findItem(id).setState(ProgressItem.DONE);
    mAdapter.notifyDataSetChanged();
  }

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
