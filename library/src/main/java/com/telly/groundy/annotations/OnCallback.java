package com.telly.groundy.annotations;

import com.telly.groundy.GroundyTask;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotation to use send custom callbacks from a background value to its callbacks. */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
public @interface OnCallback {
  Class<? extends GroundyTask>[] value();

  String name();
}
