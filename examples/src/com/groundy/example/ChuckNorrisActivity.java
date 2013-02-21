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
import android.widget.EditText;
import com.codeslap.groundy.Groundy;
import com.codeslap.groundy.example.R;
import com.groundy.example.tasks.ChuckNorrisTask;

/**
 * @author Cristian Castiblanco <cristian@elhacker.net>
 */
public class ChuckNorrisActivity extends Activity {

    private EditText mNorrisFacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chuck_norris);

        mNorrisFacts = (EditText) findViewById(R.id.norris_facts);

        findViewById(R.id.get_norris_fact).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Groundy.create(ChuckNorrisActivity.this, ChuckNorrisTask.class)
                        .receiver(mReceiver)
                        .queue();
            }
        });
    }

    private final ResultReceiver mReceiver = new ResultReceiver(new Handler()) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            if (resultCode == Groundy.STATUS_FINISHED) {
                String fact = resultData.getString("fact");
                if (fact != null) {
                    mNorrisFacts.append(fact + "\n\n");
                }
            }
        }
    };
}