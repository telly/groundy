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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.telly.groundy.GroundyManger;
import com.telly.groundy.example.R;

import java.util.SortedMap;
import java.util.TreeMap;

public class MainActivity extends Activity {

  private static final SortedMap<Integer, Class<? extends Activity>> map;
  static {
    map = new TreeMap<Integer, Class<? extends Activity>>();
    map.put(R.id.simple_example, SimpleTaskExample.class);
    map.put(R.id.cancel_example, CancelTaskExample.class);
    map.put(R.id.safe_simple_example, SafeSimpleTaskTest.class);
    map.put(R.id.attach_receiver_example, AttachReceiverExample.class);
    map.put(R.id.chuck_norris_example, ChuckNorrisActivity.class);
    map.put(R.id.queue_example, QueueExample.class);
    map.put(R.id.execute_example, ExecuteExample.class);
    map.put(R.id.download_example, DownloadExample.class);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    GroundyManger.setLogEnabled(false);
  }

  public void onButtonClick(View view) {
    startActivity(new Intent(this, map.get(view.getId())));
  }
}
