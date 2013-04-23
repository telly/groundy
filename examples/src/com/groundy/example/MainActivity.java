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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.telly.groundy.GroundyManager;
import com.telly.groundy.example.R;

import java.util.SortedMap;
import java.util.TreeMap;

public class MainActivity extends Activity {

  private static final SortedMap<Integer, Class<? extends Activity>> map;

  static {
    map = new TreeMap<Integer, Class<? extends Activity>>();
    map.put(R.id.simple_example, SimpleExample.class);
    map.put(R.id.cancel_example, CancelTaskExample.class);
    map.put(R.id.safe_simple_example, SafeSimpleTask.class);
    map.put(R.id.attach_receiver_example, AttachReceiverExample.class);
    map.put(R.id.chuck_norris_example, ChuckNorrisActivity.class);
    map.put(R.id.queue_example, QueueExample.class);
    map.put(R.id.execute_example, AsyncExample.class);
    map.put(R.id.download_example, DownloadExample.class);
    map.put(R.id.callback_test, CallbackTest.class);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    GroundyManager.setLogEnabled(false);
  }

  public void onButtonClick(View view) {
    startActivity(new Intent(this, map.get(view.getId())));
  }
}
