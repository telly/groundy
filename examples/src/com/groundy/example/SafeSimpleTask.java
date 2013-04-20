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
import android.widget.TextView;
import android.widget.Toast;
import com.groundy.example.tasks.RandomTimeTask;
import com.telly.groundy.CallbacksManager;
import com.telly.groundy.Groundy;
import com.telly.groundy.annotations.OnSuccess;
import com.telly.groundy.example.R;
import com.telly.groundy.util.Bundler;
import java.util.Random;

public class SafeSimpleTask extends Activity {

  private View mBtnAddTask;

  private CallbacksManager mCallbacksManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.simple_example);
    mCallbacksManager = CallbacksManager.init(savedInstanceState);
    mCallbacksManager.linkCallbacks(this);

    TextView explanation = (TextView) findViewById(R.id.simple_example_explanation);
    explanation.setText(R.string.safe_example_explanation);

    mBtnAddTask = findViewById(R.id.send_random_task);
    if (savedInstanceState != null) {
      mBtnAddTask.setEnabled(savedInstanceState.getBoolean("is_button_enabled"));
    }

    mBtnAddTask.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mBtnAddTask.setEnabled(false);

        // configure task parameters
        int time = new Random().nextInt(10000);
        Bundle params = new Bundler().add(RandomTimeTask.KEY_ESTIMATED, time).build();
        Toast.makeText(SafeSimpleTask.this, getString(R.string.task_will_take_x, time),
            Toast.LENGTH_SHORT).show();

        // queue task
        Groundy.create(RandomTimeTask.class)
            .callback(SafeSimpleTask.this)
            .callbackManager(mCallbacksManager)
            .params(params)
            .queue(SafeSimpleTask.this);
      }
    });
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mCallbacksManager.onSaveInstanceState(outState);
    outState.putBoolean("is_button_enabled", mBtnAddTask.isEnabled());
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mCallbacksManager.onDestroy();
  }

  @OnSuccess(RandomTimeTask.class)
  public void onReceiveResult() {
    mBtnAddTask.setEnabled(true);
    Toast.makeText(SafeSimpleTask.this, R.string.task_finished, Toast.LENGTH_LONG).show();
  }
}
