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

package com.telly.groundy;

import android.content.Context;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

final class GroundyTaskFactory {
  private static final String TAG = "GroundyTaskFactory";

  private static final Map<Class<? extends GroundyTask>, GroundyTask> CACHE =
      new HashMap<Class<? extends GroundyTask>, GroundyTask>();

  private GroundyTaskFactory() {
  }

  /**
   * Builds a GroundyTask based on call.
   *
   * @param taskClass groundy value implementation class
   * @param context used to instantiate the value
   * @return An instance of a GroundyTask if a given call is valid null otherwise
   */
  static GroundyTask get(Class<? extends GroundyTask> taskClass, Context context) {
    if (CACHE.containsKey(taskClass)) {
      return CACHE.get(taskClass);
    }
    GroundyTask groundyTask = null;
    try {
      L.d(TAG, "Instantiating " + taskClass);
      Constructor ctc = taskClass.getConstructor();
      groundyTask = (GroundyTask) ctc.newInstance();
      if (groundyTask.canBeCached()) {
        CACHE.put(taskClass, groundyTask);
      } else if (CACHE.containsKey(taskClass)) {
        CACHE.remove(taskClass);
      }
      groundyTask.setContext(context);
      groundyTask.onCreate();
      return groundyTask;
    } catch (Exception e) {
      L.e(TAG, "Unable to create value for call " + taskClass, e);
    }
    return groundyTask;
  }
}
