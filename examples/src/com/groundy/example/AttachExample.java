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
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.groundy.example.tasks.FakeDownloadTask;
import com.telly.groundy.Groundy;
import com.telly.groundy.GroundyManager;
import com.telly.groundy.GroundyTask;
import com.telly.groundy.TaskHandler;
import com.telly.groundy.annotations.OnProgress;
import com.telly.groundy.annotations.OnSuccess;
import com.telly.groundy.annotations.Param;
import com.telly.groundy.example.R;
import java.util.List;

public class AttachExample extends Activity {

  private ProgressBar mProgressBar;
  private TaskHandler mTaskHandler;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.attach_callback_example);

    mProgressBar = (ProgressBar) findViewById(R.id.progress);

    GroundyManager.attachCallbacks(this, attachListener, FakeDownloadTask.class, this);
  }

  private final GroundyManager.OnAttachListener attachListener =
      new GroundyManager.OnAttachListener() {
        @Override
        public void attachePerformed(Class<? extends GroundyTask> task,
            final List<TaskHandler> taskHandlers) {
          if (taskHandlers.isEmpty()) {
            // could not attach any task. it means there is nothing running...
            // let's start it right now
            queueTask();
          } else {
            // callback attached... it means the task is running :)
            cancelTask(taskHandlers);
          }
        }
      };

  private void cancelTask(final List<TaskHandler> taskHandlers) {
    Toast.makeText(this, R.string.callback_attached, Toast.LENGTH_SHORT).show();
    new Handler().postDelayed(new Runnable() {
      @Override public void run() {
        taskHandlers.get(0)
            .cancel(AttachExample.this, 0, new GroundyManager.SingleCancelListener() {
              @Override public void onCancelResult(long id, int result) {
                Toast.makeText(AttachExample.this, R.string.task_cancelled, Toast.LENGTH_LONG)
                    .show();
              }
            });
      }
    }, 3000);
  }

  private void queueTask() {
    Toast.makeText(this, R.string.attach_toast, Toast.LENGTH_LONG).show();
    // queue task
    mTaskHandler = Groundy.create(FakeDownloadTask.class).callback(this).queue(this);

    new Handler().postDelayed(new Runnable() {
      @Override public void run() {
        finish();
      }
    }, 5000);
  }

  @OnProgress(FakeDownloadTask.class)
  public void onProgress(@Param(Groundy.PROGRESS) int progress) {
    mProgressBar.setProgress(progress);
  }

  @OnSuccess(FakeDownloadTask.class) public void onSuccess() {
    Toast.makeText(AttachExample.this, R.string.task_finished_successfully, Toast.LENGTH_LONG)
        .show();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    if (mTaskHandler != null) {
      mTaskHandler.clearCallbacks();
    }
  }
}
