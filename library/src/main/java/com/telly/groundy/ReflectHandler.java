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

import com.telly.groundy.annotations.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ReflectHandler implements ResponseHandler {

  private static final Class<?>[] GROUNDY_CALLBACKS = {OnStart.class, OnSuccess.class, OnFailed.class, OnCancel.class, OnProgress.class};
  private final Map<Class<? extends Annotation>, List<MethodSpec>> callbacksMap;

  ReflectHandler(Class<? extends GroundyTask> groundyTaskType, Object... callbackHandlers) {
    callbacksMap = new HashMap<Class<? extends Annotation>, List<MethodSpec>>();
    if (callbackHandlers != null) {
      for (Object callbackHandler : callbackHandlers) {
        appendMethodSpec(groundyTaskType, callbackHandler);
      }
    }
  }

  private void appendMethodSpec(Class<? extends GroundyTask> groundyTaskType, Object handler) {
    Class<?> type = handler.getClass();
    while (type != Object.class) {
      appendMethodSpec(groundyTaskType, handler, type);
      if (!groundyTaskType.isAnnotationPresent(Traverse.class)) {
        // traverse class hierarchy when @Traverse annotation is present only
        break;
      }
      type = type.getSuperclass();
    }
  }

  private void removeMethodSpec(Class<? extends GroundyTask> groundyTaskType, Object handler) {
    for (List<MethodSpec> methodSpecs : new ArrayList<List<MethodSpec>>(callbacksMap.values())) {
      for (MethodSpec methodSpec : methodSpecs) {
        boolean handlerAnnotationMatched = false;
        for (Annotation annotation : methodSpec.method.getDeclaredAnnotations()) {
          if (isValid(groundyTaskType, annotation)) {
            handlerAnnotationMatched = true;
            break;
          }

          Class<? extends Annotation> annotationType = annotation.annotationType();
          boolean customCallback = annotationType.isAnnotationPresent(Callback.class);
          if (customCallback && groundyTaskType.isAnnotationPresent(annotationType)) {
            handlerAnnotationMatched = true;
            break;
          }
        }

        if (handlerAnnotationMatched && methodSpec.handler == handler) {
          methodSpecs.remove(methodSpec);
        }
      }
    }
  }

  private void appendMethodSpec(Class<? extends GroundyTask> groundyTaskType, Object handler,
                                Class<?> type) {
    for (Method method : type.getDeclaredMethods()) {
      // register groundy callbacks
      for (Class<?> groundyCallback : GROUNDY_CALLBACKS) {
        //noinspection unchecked
        Class<? extends Annotation> annotation = (Class<? extends Annotation>) groundyCallback;
        appendMethodCallback(groundyTaskType, annotation, handler, method);
      }

      Annotation[] annotations = groundyTaskType.getDeclaredAnnotations();
      if (annotations != null) {
        for (Annotation annotation : annotations) {
          Class<? extends Annotation> callbackAnnotation = annotation.annotationType();
          if (callbackAnnotation.isAnnotationPresent(Callback.class)) {
            appendMethodCallback(groundyTaskType, callbackAnnotation, handler, method);
          }
        }
      }
    }
  }

  @Override public List<MethodSpec> getMethodSpecs(Class<? extends Annotation> callbackAnnotation) {
    return callbacksMap.get(callbackAnnotation);
  }

  @Override public void appendCallbackHandlers(Class<? extends GroundyTask> groundyTaskClass,
                                               Object[] callbackHandlers) {
    if (callbackHandlers != null) {
      for (Object callbackHandler : callbackHandlers) {
        appendMethodSpec(groundyTaskClass, callbackHandler);
      }
    }
  }

  @Override public void removeCallbackHandlers(Class<? extends GroundyTask> groundyTaskClass,
                                               Object... callbackHandlers) {
    if (callbackHandlers != null) {
      for (Object callbackHandler : callbackHandlers) {
        removeMethodSpec(groundyTaskClass, callbackHandler);
      }
    }
  }

  @Override public void clearHandlers() {
    callbacksMap.clear();
  }

  private void appendMethodCallback(Class<? extends GroundyTask> groundyTaskType,
                                    Class<? extends Annotation> phaseAnnotation, Object handler,
                                    Method method) {
    Annotation methodAnnotation = method.getAnnotation(phaseAnnotation);
    if (methodAnnotation == null || !isValid(groundyTaskType, methodAnnotation)) {
      return;
    }

    if (!Modifier.isPublic(method.getModifiers())) {
      throw new IllegalStateException("Callback methods can only be public");
    }

    Class<?> returnType = method.getReturnType();
    if (returnType != void.class) {
      throw new IllegalStateException("Callback methods must return void");
    }

    List<String> paramNames = new ArrayList<String>();
    Annotation[][] paramAnnotations = method.getParameterAnnotations();
    for (int i = 0; i < paramAnnotations.length; i++) {
      Annotation[] paramAnnotation = paramAnnotations[i];
      Param param = null;
      for (Annotation annotation : paramAnnotation) {
        if (annotation instanceof Param) {
          param = (Param) annotation;
          break;
        }
      }

      if (param == null) {
        throw new IllegalStateException("All parameters must be annotated with @Param. " + method +
            ", param " + i + " doesn't");
      }
      paramNames.add(param.value());
    }

    Class<? extends Annotation> annotationType = methodAnnotation.annotationType();
    List<MethodSpec> methodSpecs;
    if (callbacksMap.containsKey(annotationType)) {
      methodSpecs = callbacksMap.get(annotationType);
    } else {
      methodSpecs = new ArrayList<MethodSpec>();
      callbacksMap.put(annotationType, methodSpecs);
    }
    methodSpecs.add(new MethodSpec(handler, method, paramNames));
  }

  private boolean isValid(Class<? extends GroundyTask> groundyTaskType,
                          Annotation methodAnnotation) {
    if (methodAnnotation instanceof OnSuccess) {
      OnSuccess onSuccess = (OnSuccess) methodAnnotation;
      if (onSuccess.value() != groundyTaskType) {
        return false;
      }
    } else if (methodAnnotation instanceof OnFailed) {
      OnFailed onFailed = (OnFailed) methodAnnotation;
      if (onFailed.value() != groundyTaskType) {
        return false;
      }
    } else if (methodAnnotation instanceof OnProgress) {
      OnProgress onProgress = (OnProgress) methodAnnotation;
      if (onProgress.value() != groundyTaskType) {
        return false;
      }
    } else if (methodAnnotation instanceof OnStart) {
      OnStart onStart = (OnStart) methodAnnotation;
      if (onStart.value() != groundyTaskType) {
        return false;
      }
    } else if (methodAnnotation instanceof OnCancel) {
      OnCancel onCancel = (OnCancel) methodAnnotation;
      if (onCancel.value() != groundyTaskType) {
        return false;
      }
    }
    return true;
  }
}