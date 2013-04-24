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
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import com.telly.groundy.annotations.OnCancel;
import com.telly.groundy.annotations.OnFailed;
import com.telly.groundy.annotations.OnSuccess;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class InternalReceiver extends ResultReceiver implements HandlersHolder {

  private static final String TAG = "groundy:receiver";
  private final List<Object> callbackHandlers;
  private final Class<? extends GroundyTask> groundyTaskType;
  private TaskProxy groundyProxy;
  public static final Pattern INNER_PATTERN = Pattern.compile("^.+?\\$\\d$");
  private static final Map<TaskAndHandler, ResultProxy> proxies;

  static {
    proxies = Collections.synchronizedMap(new HashMap<TaskAndHandler, ResultProxy>());
  }

  InternalReceiver(Class<? extends GroundyTask> taskType, Object... handlers) {
    super(new Handler()); // TODO make sure we are in the main thread
    callbackHandlers = new ArrayList<Object>();
    groundyTaskType = taskType;
    appendCallbackHandlers(handlers);
  }

  @Override
  public void onReceiveResult(int resultCode, Bundle resultData) {
    if (resultCode == Groundy.RESULT_CODE_CALLBACK_ANNOTATION) {//noinspection unchecked
      Class<? extends Annotation> callbackAnnotation = (Class<? extends Annotation>) resultData.getSerializable(
          Groundy.KEY_CALLBACK_ANNOTATION);
      handleCallback(callbackAnnotation, resultData);
    }
  }

  @Override public void appendCallbackHandlers(Object... handlers) {
    if (handlers != null) {
      for (Object callbackHandler : handlers) {
        callbackHandlers.add(callbackHandler);
      }
    }
  }

  @Override public void removeCallbackHandlers(Class<? extends GroundyTask> groundyTaskClass,
                                               Object... handlers) {
    if (handlers != null) {
      for (Object callbackHandler : handlers) {
        callbackHandlers.remove(callbackHandler);
      }
    }
  }

  @Override public void clearHandlers() {
    callbackHandlers.clear();
  }

  @Override public void handleCallback(Class<? extends Annotation> callbackAnnotation,
                                       Bundle resultData) {
    boolean isEndingAnnotation = callbackAnnotation == OnSuccess.class ||
        callbackAnnotation == OnFailed.class ||
        callbackAnnotation == OnCancel.class;
    if (isEndingAnnotation && groundyProxy != null) {
      groundyProxy.onTaskEnded();
    }

    for (Object callbackHandler : callbackHandlers) {
      ResultProxy methodProxy = getMethodProxy(callbackHandler);
      if (methodProxy != null) {
        methodProxy.apply(callbackHandler, callbackAnnotation, resultData);
      }
    }
  }

  private ResultProxy getMethodProxy(Object handler) {
    if (handler == null) {
      return null;
    }

    Class<?> handlerType = handler.getClass();
    TaskAndHandler taskAndHandler = new TaskAndHandler(groundyTaskType, handlerType);
    synchronized (proxies) {
      if (proxies.containsKey(taskAndHandler)) {
        return proxies.get(taskAndHandler);
      }
    }

    ResultProxy resultProxy;
    boolean isNotPublic = !Modifier.isPublic(handlerType.getModifiers());
    if (isInner(handlerType) || isNotPublic) {
      if (isNotPublic) {
        Log.d(TAG,
            "Using reflection for " + handlerType + " because its not public. It's recommended to use public callbacks which enables code generation which makes things way faster.");
      }
      resultProxy = new ReflectProxy(groundyTaskType, handlerType);
    } else {
      try {
        String pkg = "com.telly.groundy.generated.";
        String callbackClassName = handlerType.getSimpleName();
        String taskName = groundyTaskType.getName().replaceAll("\\.", "\\$");
        Class<?> proxyClass = Class.forName(pkg + callbackClassName + "$" + taskName + "$Proxy");
        resultProxy = (ResultProxy) proxyClass.newInstance();
        if (resultProxy == null) {
          throw new NullPointerException("Could not create proxy: " + proxyClass);
        }
        Log.d(TAG, "Using fast proxy for: " + handlerType);
      } catch (Exception e) {
        e.printStackTrace();
        resultProxy = new ReflectProxy(groundyTaskType, handlerType);
        Log.d(TAG, "Using reflection proxy for " + handlerType);
      }
    }

    proxies.put(taskAndHandler, resultProxy);
    return resultProxy;
  }

  private static boolean isInner(Class<?> type) {
    Matcher matcher = INNER_PATTERN.matcher(type.getName());
    return matcher.matches();
  }

  public void setOnFinishedListener(TaskProxyImpl<? extends GroundyTask> groundyProxy) {
    this.groundyProxy = groundyProxy;
  }

  private static class TaskAndHandler {
    final Class<? extends GroundyTask> taskType;
    final Class<?> handlerType;

    private TaskAndHandler(Class<? extends GroundyTask> taskType, Class<?> handlerType) {
      this.taskType = taskType;
      this.handlerType = handlerType;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      TaskAndHandler that = (TaskAndHandler) o;

      if (handlerType != null ? !handlerType.equals(that.handlerType) : that.handlerType != null)
        return false;
      if (taskType != null ? !taskType.equals(that.taskType) : that.taskType != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = taskType != null ? taskType.hashCode() : 0;
      result = 31 * result + (handlerType != null ? handlerType.hashCode() : 0);
      return result;
    }
  }
}