package com.telly.groundy;

public class ClassUtils {
  static String buildGeneratedProxyName(String callbackClassName, String taskName) {
    return callbackClassName + "$" + taskName + "$Proxy";
  }
}
