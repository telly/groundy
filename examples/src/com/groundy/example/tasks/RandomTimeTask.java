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

import com.telly.groundy.GroundyTask;
import com.telly.groundy.Succeeded;
import com.telly.groundy.TaskResult;
import com.telly.groundy.annotations.Traverse;

@Traverse
public class RandomTimeTask extends GroundyTask {

  public static final String KEY_ESTIMATED = "estimated";

  @Override
  protected TaskResult doInBackground() {
    int time = getIntParam(KEY_ESTIMATED);
    if (time < 1000) {
      time = 1000;
    }

    int interval = time / 100;

    int currentPercentage = 0;
    while (currentPercentage <= 100) {
      try {
        updateProgress(currentPercentage);

        // let's fake some work ^_^
        Thread.sleep(interval);
        currentPercentage++;
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return new Succeeded();
  }
}