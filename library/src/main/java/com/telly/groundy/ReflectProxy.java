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
import com.telly.groundy.annotations.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ReflectProxy implements ResultProxy {

  private static final Class<?>[] GROUNDY_CALLBACKS = {OnStart.class, OnSuccess.class, OnFailed.class, OnCancel.class, OnProgress.class};
  private static final Map<Class<?>, Method[]> METHODS_CACHE = new HashMap<Class<?>, Method[]>();
  private static final Map<Class<?>, Annotation[]> ANNOTATIONS_CACHE = new HashMap<Class<?>, Annotation[]>();

  private final Map<Class<? extends Annotation>, List<MethodSpec>> callbacksMap;
  private final Class<? extends GroundyTask> groundyTaskType;
  private final Class<?> handlerType;

  ReflectProxy(Class<? extends GroundyTask> groundyTaskType, Class<?> handlerType) {
    this.groundyTaskType = groundyTaskType;
    this.handlerType = handlerType;
    callbacksMap = new HashMap<Class<? extends Annotation>, List<MethodSpec>>();

    fillMethodSpecMap();
  }

  @Override public void apply(Object target, Class<? extends Annotation> callbackAnnotation, Bundle resultData) {
    List<MethodSpec> methodSpecs = callbacksMap.get(callbackAnnotation);
    if (methodSpecs == null || methodSpecs.isEmpty()) {
      return;
    }

    for (MethodSpec methodSpec : methodSpecs) {
      Object[] values = getReturnParams(resultData, methodSpec);
      try {
        methodSpec.method.invoke(target, values);
      } catch (Exception pokemon) {
        pokemon.printStackTrace();
      }
    }
  }

  private void fillMethodSpecMap() {
    Class<?> type = handlerType;
    while (type != Object.class) {
      fillMethodSpecMapWith(type);
      if (!groundyTaskType.isAnnotationPresent(Traverse.class)) {
        // traverse class hierarchy when @Traverse annotation is present only
        break;
      }
      type = type.getSuperclass();
    }
  }

  private void fillMethodSpecMapWith(Class<?> type) {
    for (Method method : getDeclaredMethods(type)) {
      // register groundy callbacks
      for (Class<?> groundyCallback : GROUNDY_CALLBACKS) {
        //noinspection unchecked
        Class<? extends Annotation> annotation = (Class<? extends Annotation>) groundyCallback;
        appendMethodCallback(groundyTaskType, annotation, method);
      }

      Annotation[] annotations = getDeclaredAnnotations(groundyTaskType);
      if (annotations != null) {
        for (Annotation annotation : annotations) {
          Class<? extends Annotation> callbackAnnotation = annotation.annotationType();
          if (callbackAnnotation.isAnnotationPresent(Callback.class)) {
            appendMethodCallback(groundyTaskType, callbackAnnotation, method);
          }
        }
      }
    }
  }

  private Annotation[] getDeclaredAnnotations(Class<?> type) {
    if (!ANNOTATIONS_CACHE.containsKey(type)) {
      ANNOTATIONS_CACHE.put(type, type.getDeclaredAnnotations());
    }
    return ANNOTATIONS_CACHE.get(type);
  }

  private static Method[] getDeclaredMethods(Class<?> type) {
    if (!METHODS_CACHE.containsKey(type)) {
      METHODS_CACHE.put(type, type.getDeclaredMethods());
    }
    return METHODS_CACHE.get(type);
  }

  private void appendMethodCallback(Class<? extends GroundyTask> groundyTaskType,
                                    Class<? extends Annotation> phaseAnnotation,
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
    methodSpecs.add(new MethodSpec(method, paramNames));
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

  private static Object[] getReturnParams(Bundle resultData, MethodSpec methodSpec) {
    Method method = methodSpec.method;

    Object[] values = new Object[methodSpec.paramNames.size()];
    Class<?>[] parameterTypes = method.getParameterTypes();

    List<String> paramNames = methodSpec.paramNames;
    for (int i = 0; i < paramNames.size(); i++) {
      Class<?> parameterType = parameterTypes[i];
      String paramName = paramNames.get(i);
      values[i] = resultData.get(paramName);
      if (values[i] == null) {
        values[i] = defaultValue(parameterType);
      } else {
        if (!isTypeValid(parameterType, values[i].getClass())) {
          throw new RuntimeException(
            paramName + " parameter is " + values[i].getClass().getSimpleName() + " but the method (" + method + ") expects " + parameterType.getSimpleName());
        }
      }
    }
    return values;
  }

  private static boolean isTypeValid(Class<?> expected, Class<?> actual) {
    if (expected == Double.class || expected == double.class) {
      return isAnyOf(actual, long.class, Long.class, int.class, Integer.class,
        double.class, Double.class, float.class, Float.class);
    } else if (expected == Float.class || expected == float.class) {
      return isAnyOf(actual, int.class, Integer.class, float.class, Float.class);
    } else if (expected == Long.class || expected == long.class) {
      return isAnyOf(actual, int.class, Integer.class, Long.class, long.class);
    } else if (expected == Integer.class || expected == int.class) {
      return isAnyOf(actual, int.class, Integer.class);
    } else if (expected == Boolean.class || expected == boolean.class) {
      return isAnyOf(actual, boolean.class, Boolean.class);
    }
    return expected.isAssignableFrom(actual);
  }

  private static boolean isAnyOf(Class<?> foo, Class<?>... bars) {
    for (Class<?> bar : bars) {
      if (foo == bar) {
        return true;
      }
    }
    return false;
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

  static class MethodSpec {
    final Method method;
    final List<String> paramNames;

    MethodSpec(Method method, List<String> paramNames) {
      this.method = method;
      this.paramNames = paramNames;
    }
  }
}