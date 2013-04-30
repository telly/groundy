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

package com.groundy.example.tasks;

import android.os.Bundle;
import com.telly.groundy.Failed;
import com.telly.groundy.GroundyTask;
import com.telly.groundy.Succeeded;
import com.telly.groundy.TaskResult;
import com.telly.groundy.example.R;
import java.util.Random;

public class ChuckNorrisKick extends GroundyTask {

  @Override
  protected TaskResult doInBackground() {
    String[] targets = getContext().getResources().getStringArray(R.array.kick_targets);
    Random random = new Random();

    for (int i = 0; i < 5; i++) {
      int currentTarget = random.nextInt(targets.length);
      Bundle resultData = new Bundle();
      resultData.putString("target", targets[currentTarget]);

      String callbackName = random.nextBoolean() ? "kick" : "punch";
      callback(callbackName, resultData);

      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    if (!random.nextBoolean()) {
      Failed failed = new Failed();
      failed.add("lifeExpectation", random.nextInt(10) + 1);
      return failed;
    }
    return new Succeeded();
  }
}
