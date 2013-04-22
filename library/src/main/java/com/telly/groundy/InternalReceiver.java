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
import com.telly.groundy.annotations.OnCancel;
import com.telly.groundy.annotations.OnFailed;
import com.telly.groundy.annotations.OnSuccess;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

class InternalReceiver extends ResultReceiver implements HandlersHolder {

  private ResponseHandler handlerProxy;
  private TaskProxy groundyProxy;

  InternalReceiver(Class<? extends GroundyTask> groundyTaskType, Object callbackHandler,
                   Object... callbackHandlers) {
    super(new Handler());
    setupHandler(groundyTaskType, callbackHandler, callbackHandlers);
  }

  @Override
  public void onReceiveResult(int resultCode, Bundle resultData) {
    if (resultCode == Groundy.RESULT_CODE_CALLBACK_ANNOTATION) {//noinspection unchecked
      Class<? extends Annotation> callbackAnnotation = (Class<? extends Annotation>) resultData.getSerializable(
          Groundy.KEY_CALLBACK_ANNOTATION);
      handleCallback(callbackAnnotation, resultData);
    }
  }

  @Override public void appendCallbackHandlers(Class<? extends GroundyTask> groundyTaskClass,
                                               Object... extraCallbackHandlers) {
    handlerProxy.appendCallbackHandlers(groundyTaskClass, extraCallbackHandlers);
  }

  @Override public void removeCallbackHandlers(Class<? extends GroundyTask> groundyTaskClass,
                                               Object... callbackHandlers) {
    handlerProxy.removeCallbackHandlers(groundyTaskClass, callbackHandlers);
  }

  @Override public void clearHandlers() {
    handlerProxy.clearHandlers();
  }

  public void setCallbackHandlers(Class<? extends GroundyTask> groundyTaskClass,
                                  Object... extraCallbackHandlers) {
    setupHandler(groundyTaskClass, extraCallbackHandlers);
  }

  public void setOnFinishedListener(TaskProxyImpl<? extends GroundyTask> groundyProxy) {
    this.groundyProxy = groundyProxy;
  }

  private void setupHandler(Class<? extends GroundyTask> groundyTaskType,
                            Object... extraCallbackHandlers) {
    handlerProxy = new ReflectHandler(groundyTaskType, extraCallbackHandlers);
  }

  private void handleCallback(Class<? extends Annotation> callbackAnnotation, Bundle resultData) {
    List<MethodSpec> methodSpecs = handlerProxy.getMethodSpecs(callbackAnnotation);
    if (methodSpecs == null || methodSpecs.isEmpty()) {
      return;
    }

    for (MethodSpec methodSpec : methodSpecs) {
      Object[] values = getReturnParams(resultData, methodSpec);
      try {
        boolean isEndingAnnotation = callbackAnnotation == OnSuccess.class ||
            callbackAnnotation == OnFailed.class ||
            callbackAnnotation == OnCancel.class;
        if (isEndingAnnotation && groundyProxy != null) {
          groundyProxy.onTaskEnded();
        }
        methodSpec.method.invoke(methodSpec.handler, values);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      }
    }
  }

  private Object[] getReturnParams(Bundle resultData, MethodSpec methodSpec) {
    Object[] values = new Object[methodSpec.paramNames.size()];
    Class<?>[] parameterTypes = methodSpec.method.getParameterTypes();

    List<String> paramNames = methodSpec.paramNames;
    for (int i = 0; i < paramNames.size(); i++) {
      Class<?> parameterType = parameterTypes[i];
      String paramName = paramNames.get(i);
      values[i] = resultData.get(paramName);
      if (values[i] == null) {
        values[i] = defaultValue(parameterType);
      } else {
        if (!parameterType.isAssignableFrom(values[i].getClass())) {
          throw new RuntimeException(
            paramName + " parameter is " + values[i].getClass().getSimpleName() + " but the method (" + methodSpec.method + ") expects " + parameterType.getSimpleName());
        }
      }
    }
    return values;
  }

  private static Object defaultValue(Class<?> parameterType) {
    if (parameterType == int.class || parameterType == Integer.class
      || parameterType == float.class || parameterType == Float.class
      || parameterType == Double.class || parameterType == Double.class
      || parameterType == byte.class || parameterType == Byte.class
      || parameterType == short.class || parameterType == Short.class) {
      return 0;
    } else if (parameterType == boolean.class || parameterType == Boolean.class) {
      return false;
    }
    return null;
  }
}