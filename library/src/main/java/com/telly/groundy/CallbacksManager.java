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

import android.os.Bundle;
import java.util.ArrayList;

/**
 * Helper class that manage your callbacks upon configuration changes. If you want Groundy to play
 * nicely with Activity configuration changes (like rotating the phone), you should use this.
 * <p/>
 * Add a field of this class into your class, initialize it from within your onCreate method by
 * calling its {@link #init(android.os.Bundle, Object...)} method and pass it the onSaveInstance
 * bundle. You can also pass the callbacks you want to manage or use the {@link
 * #linkCallbacks(Object...)} method.
 * <p/>
 * Make sure to call {@link #onSaveInstanceState(android.os.Bundle)} from your activity or fragment
 * onSaveInstanceState method. Also, it's a good idea to call {@link #onDestroy()} from within your
 * activity or fragment onDestroy method.
 * <p/>
 * When you create a Groundy value, you must call {@link Groundy#callbackManager(CallbacksManager)}
 * method and pass the instance of this class. This is important; otherwise the callback manager
 * won't know which tasks it should manage.
 */
public final class CallbacksManager {

  public static final String GROUNDY_PROXY_KEY_PREFIX = "com.telly.groundy.key.GROUNDY_PROXY_KEY:";
  private static final String TASK_PROXY_LIST = "com.telly.groundy.key.TASK_PROXY_LIST";

  private final ArrayList<TaskHandler> proxyTasks = new ArrayList<TaskHandler>();

  private CallbacksManager() {
  }

  /**
   * Call from within your activity or fragment onCreate method.
   *
   * @param bundle the onSaveInstance bundle
   * @param callbackHandlers an array of callback handlers to mange
   * @return an instance of {@link CallbacksManager}
   */
  public static CallbacksManager init(Bundle bundle, Object... callbackHandlers) {
    if (bundle == null) {
      return new CallbacksManager();
    }

    CallbacksManager callbacksManager = new CallbacksManager();
    ArrayList<TaskHandler> taskProxies = bundle.getParcelableArrayList(TASK_PROXY_LIST);
    if (taskProxies != null) {
      callbacksManager.proxyTasks.addAll(taskProxies);
    }

    if (callbackHandlers != null) {
      for (TaskHandler proxyTask : new ArrayList<TaskHandler>(callbacksManager.proxyTasks)) {
        proxyTask.clearCallbacks();
        proxyTask.appendCallbacks(callbackHandlers);
      }
    }
    return callbacksManager;
  }

  /**
   * Links the specified callback handlers to their respective tasks.
   *
   * @param callbackHandlers an array of callback handlers
   */
  public void linkCallbacks(Object... callbackHandlers) {
    if (callbackHandlers != null) {
      for (TaskHandler proxyTask : new ArrayList<TaskHandler>(proxyTasks)) {
        proxyTask.clearCallbacks();
        proxyTask.appendCallbacks(callbackHandlers);
      }
    }
  }

  /**
   * Saves the current callback handlers information in order to restore them after the
   * configuration change.
   *
   * @param bundle the same bundle you receive from within your activity or fragment
   * onSaveInstanceState method
   */
  public void onSaveInstanceState(Bundle bundle) {
    bundle.putParcelableArrayList(TASK_PROXY_LIST, proxyTasks);
    for (TaskHandler proxyTask : proxyTasks) {
      bundle.putParcelable(GROUNDY_PROXY_KEY_PREFIX + proxyTask.getTaskId(), proxyTask);
    }
  }

  /** Frees all callback handlers from their current tasks. */
  public void onDestroy() {
    for (TaskHandler proxyTask : proxyTasks) {
      proxyTask.clearCallbacks();
    }
  }

  void register(TaskHandler taskHandler) {
    proxyTasks.add(taskHandler);
  }
}
