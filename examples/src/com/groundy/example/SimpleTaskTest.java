/*
 * Copyright 2012 Twitvid Inc.
 * Copyright 2013 Cristian Castiblanco
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import com.codeslap.groundy.Groundy;
import com.codeslap.groundy.example.R;
import com.codeslap.groundy.util.Bundler;
import com.groundy.example.tasks.RandomTimeTask;

import java.util.Random;

/**
 * @author Cristian Castiblanco <cristian@elhacker.net>
 */
public class SimpleTaskTest extends Activity {

    private View mBtnAddTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_example);
        mBtnAddTask = findViewById(R.id.send_random_task);
        mBtnAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtnAddTask.setEnabled(false);

                // configure task parameters
                int time = new Random().nextInt(10000);
                Bundle params = new Bundler().add(RandomTimeTask.KEY_ESTIMATED, time).build();
                Toast.makeText(SimpleTaskTest.this, getString(R.string.task_will_take_x, time), Toast.LENGTH_SHORT).show();

                // queue task
                Groundy.create(SimpleTaskTest.this, RandomTimeTask.class)
                        .receiver(resultReceiver)
                        .params(params)
                        .queue();
            }
        });
    }

    private final ResultReceiver resultReceiver = new ResultReceiver(new Handler()) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            if (resultCode == Groundy.STATUS_FINISHED) {
                mBtnAddTask.setEnabled(true);
                Toast.makeText(SimpleTaskTest.this, R.string.task_finished, Toast.LENGTH_LONG).show();
            }
        }
    };
}
