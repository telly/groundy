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
import android.widget.*;
import com.groundy.example.tasks.RandomTimeTask;
import com.telly.groundy.CallbacksManager;
import com.telly.groundy.Groundy;
import com.telly.groundy.TaskHandler;
import com.telly.groundy.annotations.OnSuccess;
import com.telly.groundy.example.R;
import com.telly.groundy.util.Bundler;
import java.util.Random;

public class AttachReceiverExample extends Activity {

  private Button mAddTaskBtn;
  private ToggleButton mAttachToastBtn;
  private CallbacksManager callbacksManager;
  private TaskHandler mTaskHandler;

  @Override
  protected void onCreate(Bundle saved) {
    super.onCreate(saved);
    setContentView(R.layout.attach_example);
    callbacksManager = CallbacksManager.init(saved, /* a callback */this, /* another callback*/
        mToastCallback);

    TextView explanation = (TextView) findViewById(R.id.simple_example_explanation);
    explanation.setText(R.string.safe_example_explanation);

    mAddTaskBtn = (Button) findViewById(R.id.send_random_task);
    if (saved != null) {
      mAddTaskBtn.setEnabled(saved.getBoolean("is_button_enabled"));
    }

    mAttachToastBtn = (ToggleButton) findViewById(R.id.attach_toast);
    mAttachToastBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
          mTaskHandler.appendCallbacks(mToastCallback);
        } else {
          mTaskHandler.removeCallbacks(mToastCallback);
        }
      }
    });

    mAddTaskBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mAddTaskBtn.setEnabled(false);
        mAttachToastBtn.setEnabled(true);

        // configure task parameters
        int time = new Random().nextInt(10000);
        Bundle params = new Bundler().add(RandomTimeTask.KEY_ESTIMATED, time).build();
        Toast.makeText(AttachReceiverExample.this, getString(R.string.task_will_take_x, time),
            Toast.LENGTH_SHORT).show();

        // queue task
        mTaskHandler = Groundy.create(RandomTimeTask.class)
            .callback(AttachReceiverExample.this)
            .callbackManager(callbacksManager)
            .params(params)
            .queue(AttachReceiverExample.this);
      }
    });
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean("is_button_enabled", mAddTaskBtn.isEnabled());
    callbacksManager.onSaveInstanceState(outState);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    callbacksManager.onDestroy();
  }

  @OnSuccess(RandomTimeTask.class)
  public void onSuccess() {
    mAddTaskBtn.setText("Got something");
    mAddTaskBtn.setEnabled(true);
    mAttachToastBtn.setEnabled(false);
  }

  private final Object mToastCallback = new Object() {
    @OnSuccess(RandomTimeTask.class)
    public void toastIt() {
      Toast.makeText(AttachReceiverExample.this, R.string.task_finished, Toast.LENGTH_LONG).show();
    }
  };
}
