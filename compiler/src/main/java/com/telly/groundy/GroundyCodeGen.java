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

import com.squareup.java.JavaWriter;
import com.telly.groundy.annotations.Param;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.logging.*;

@SupportedAnnotationTypes({
    GroundyCodeGen.SUCCESS, GroundyCodeGen.FAILED, GroundyCodeGen.START, GroundyCodeGen.CANCEL,
    GroundyCodeGen.PROGRESS, GroundyCodeGen.CALLBACK
})
public class GroundyCodeGen extends AbstractProcessor {

  private static final Logger logger = setupLogger();
  public static final String SUCCESS = "com.telly.groundy.annotations.OnSuccess";
  public static final String FAILED = "com.telly.groundy.annotations.OnFailed";
  public static final String START = "com.telly.groundy.annotations.OnStart";
  public static final String CANCEL = "com.telly.groundy.annotations.OnCancel";
  public static final String PROGRESS = "com.telly.groundy.annotations.OnProgress";
  public static final String CALLBACK = "com.telly.groundy.annotations.OnCallback";

  private final Map<String, Set<ProxyImplContent>> implMap =
      new HashMap<String, Set<ProxyImplContent>>();

  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment env) {
    if (typeElements.isEmpty()) {
      return true;
    }
    for (TypeElement annotationElement : typeElements) {
      Set<? extends Element> annotatedElements = env.getElementsAnnotatedWith(annotationElement);
      for (Element annotatedElement : annotatedElements) {
        if (annotatedElement instanceof ExecutableElement) {
          ExecutableElement callbackMethod = (ExecutableElement) annotatedElement;
          processCallback(annotationElement, callbackMethod);
        }
      }
    }

    for (Map.Entry<String, Set<ProxyImplContent>> elementSetEntry : implMap.entrySet()) {
      String proxyClassName = elementSetEntry.getKey();
      Set<ProxyImplContent> callbacks = elementSetEntry.getValue();
      generateProxy(proxyClassName, callbacks);
    }

    return true;
  }

  private void processCallback(TypeElement annotationElement, ExecutableElement callbackMethod) {
    // ignore inner classes and non public classes
    Element callbackElement = callbackMethod.getEnclosingElement();
    if (!callbackElement.getModifiers().contains(Modifier.PUBLIC)) {
      logger.info(
          callbackElement + " is not public. Reflection will be used and it can slow things down.");
      return;
    }

    Object value = getAnnotationValue(callbackMethod, annotationElement, "value");
    if (value == null) {
      logger.info("Could not found task for " + annotationElement);
      System.exit(1);
    }

    Object callbackName = getAnnotationValue(callbackMethod, annotationElement, "name");
    String groundyTaskName = value.toString().replaceAll("\\.", "\\$");
    String genClassName = callbackElement.getSimpleName().toString();
    String proxyClassName = genClassName + "$" + groundyTaskName + "$Proxy";

    Set<ProxyImplContent> proxyImplContents;
    if (implMap.containsKey(proxyClassName)) {
      proxyImplContents = implMap.get(proxyClassName);
    } else {
      implMap.put(proxyClassName, proxyImplContents = new HashSet<ProxyImplContent>());
    }

    ProxyImplContent proxyImplContent = new ProxyImplContent();
    proxyImplContent.annotation = annotationElement.toString();
    proxyImplContent.paramNames = getParamNames(callbackMethod);
    proxyImplContent.methodName = callbackMethod.getSimpleName().toString();
    proxyImplContent.fullTargetClassName = callbackElement.toString();
    proxyImplContent.callbackName = callbackName != null ? callbackName.toString() : null;

    proxyImplContents.add(proxyImplContent);
  }

  private Object getAnnotationValue(Element callbackMethod, TypeElement annotationElement,
      String executable) {
    AnnotationMirror annotation = null;
    for (AnnotationMirror annotationMirror : callbackMethod.getAnnotationMirrors()) {
      if (annotationMirror.getAnnotationType().equals(annotationElement.asType())) {
        annotation = annotationMirror;
        break;
      }
    }

    if (annotation == null) {
      return null;
    }

    for (ExecutableElement executableElement : annotation.getElementValues().keySet()) {
      if (executableElement.getSimpleName().toString().equals(executable)) {
        return annotation.getElementValues().get(executableElement).getValue();
      }
    }
    return null;
  }

  private void generateProxy(String proxyClassName, Set<ProxyImplContent> callbacks) {
    StringWriter classContent = new StringWriter();
    JavaWriter javaWriter = new JavaWriter(classContent);
    try {
      javaWriter.emitEndOfLineComment("auto-generated file; don't modify");
      javaWriter.emitPackage("com.telly.groundy.generated");
      javaWriter.emitImports("android.os.Bundle", Annotation.class.getName(),
          "com.telly.groundy.*");

      javaWriter.beginType(proxyClassName, "class", java.lang.reflect.Modifier.PUBLIC, null,
          "ResultProxy");

      javaWriter.beginMethod("void", "apply", java.lang.reflect.Modifier.PUBLIC, "Object", "target",
          "Class<? extends Annotation>", "callbackAnnotation", "Bundle", "resultData");

      // make sure the target and result is OK
      String fullTargetClassName = callbacks.iterator().next().fullTargetClassName;
      String targetIsValid = "!(target instanceof " + fullTargetClassName + ")";
      String resultIsValid = "resultData == null";
      javaWriter.beginControlFlow("if(" + targetIsValid + " || " + resultIsValid + ")");
      javaWriter.emitStatement("return");
      javaWriter.endControlFlow();

      javaWriter.emitStatement(
          "String callbackName = resultData.getString(\"" + Groundy.KEY_CALLBACK_NAME + "\")");

      for (ProxyImplContent proxyImpl : callbacks) {
        String shouldHandleAnnotation = "callbackAnnotation == " + proxyImpl.annotation + ".class";
        if (proxyImpl.callbackName != null) {
          String callbackNameCheck = '\"' + proxyImpl.callbackName + "\".equals(callbackName)";
          shouldHandleAnnotation += " && " + callbackNameCheck;
        }
        javaWriter.beginControlFlow("if (" + shouldHandleAnnotation + ")");

        StringBuilder invocation = new StringBuilder();
        String separator = "";
        for (int i = 0; i < proxyImpl.paramNames.size(); i++) {
          NameAndType nameAndType = proxyImpl.paramNames.get(i);
          String paramInsertionName = "groundyCallbackParam" + i;

          String valName = "groundyValue" + i;
          String assignation = "resultData.get(\"" + nameAndType.name + "\")";
          javaWriter.emitStatement("Object " + valName + " = " + assignation);

          javaWriter.beginControlFlow("if(" + valName + " == null)");
          javaWriter.emitStatement(valName + " = " + defaultValue(nameAndType.type));
          javaWriter.endControlFlow();

          String declaration = nameAndType.type + " " + paramInsertionName;
          String casting = " = (" + cleanCasting(nameAndType.type) + ") ";
          javaWriter.emitStatement(declaration + casting + valName);

          invocation.append(separator).append(paramInsertionName);
          separator = ", ";

          javaWriter.emitEmptyLine();
        }

        String castTarget = "((" + fullTargetClassName + ")target).";
        String invokeCallback = proxyImpl.methodName + "(" + invocation + ")";
        javaWriter.emitStatement(castTarget + invokeCallback);

        javaWriter.emitStatement("return");
        javaWriter.endControlFlow();
      }

      javaWriter.endMethod();
      javaWriter.endType();
      javaWriter.close();

      String fileContent = classContent.toString();

      Filer filer = processingEnv.getFiler();
      JavaFileObject sourceFile = filer.createSourceFile(proxyClassName, (Element) null);
      Writer writer = sourceFile.openWriter();
      writer.write(fileContent);
      writer.flush();
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

  private static String defaultValue(String parameterType) {
    if (parameterType.equals(int.class.getName())
        || parameterType.equals("Integer")
        || parameterType.equals(float.class.getName())
        || parameterType.equals("Float")
        || parameterType.equals(double.class.getName())
        || parameterType.equals("Double")
        || parameterType.equals(long.class.getName())
        || parameterType.equals("Long")
        || parameterType.equals(byte.class.getName())
        || parameterType.equals("Byte")
        || parameterType.equals(char.class.getName())
        || parameterType.equals("Character")
        || parameterType.equals(short.class.getName())
        || parameterType.equals("Short")) {
      return "0";
    } else if (parameterType.equals(boolean.class.getName()) || parameterType.equals("Boolean")) {
      return "false";
    }
    return "null";
  }

  private String cleanCasting(String type) {
    if (primitives.containsKey(type)) {
      return primitives.get(type);
    }
    return type;
  }

  /** Makes sure method is public, returns void and all its parameters are annotated too. */
  private static List<NameAndType> getParamNames(Element callbackMethod) {
    Element parentClass = callbackMethod.getEnclosingElement();
    String methodFullInfo = parentClass + "#" + callbackMethod;

    ExecutableElement method = (ExecutableElement) callbackMethod;

    if (!method.getModifiers().contains(Modifier.PUBLIC)) {
      logger.info(methodFullInfo + " must be public.");
      System.exit(-1);
    }

    if (method.getReturnType().getKind() != TypeKind.VOID) {
      logger.info(methodFullInfo + " must return void.");
      System.exit(-1);
    }

    List<NameAndType> paramNames = new ArrayList<NameAndType>();
    for (VariableElement param : method.getParameters()) {
      Param paramAnnotation = param.getAnnotation(Param.class);
      if (paramAnnotation == null) {
        logger.info(methodFullInfo
            + ": all parameters must be annotated with the @"
            + Param.class.getName());
        System.exit(-1);
      }
      paramNames.add(new NameAndType(paramAnnotation.value(), param.asType().toString()));
    }

    return paramNames;
  }

  private static Logger setupLogger() {
    Logger logger = Logger.getAnonymousLogger();
    for (Handler handler : logger.getParent().getHandlers()) {
      if (handler instanceof ConsoleHandler) {
        SimpleFormatter newFormatter = new SimpleFormatter() {
          @Override public synchronized String format(LogRecord r) {
            return "[COMPILER] " + r.getMessage() + "\n";
          }
        };
        handler.setFormatter(newFormatter);
      }
    }
    return logger;
  }

  private static class NameAndType {
    final String name;
    final String type;

    private NameAndType(String name, String type) {
      this.name = name;
      this.type = type;
    }
  }

  private static class ProxyImplContent {
    String annotation;
    List<NameAndType> paramNames;
    String methodName;
    String fullTargetClassName;
    String callbackName;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ProxyImplContent that = (ProxyImplContent) o;

      if (annotation != null ? !annotation.equals(that.annotation) : that.annotation != null) {
        return false;
      }
      if (callbackName != null ? !callbackName.equals(that.callbackName)
          : that.callbackName != null) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = annotation != null ? annotation.hashCode() : 0;
      result = 31 * result + (callbackName != null ? callbackName.hashCode() : 0);
      return result;
    }
  }

  private static final Map<String, String> primitives = new HashMap<String, String>();

  static {
    primitives.put("int", "Integer");
    primitives.put("long", "Long");
    primitives.put("float", "Float");
    primitives.put("double", "Double");
    primitives.put("char", "Character");
    primitives.put("boolean", "Boolean");
    primitives.put("byte", "Byte");
    primitives.put("short", "Short");
  }
}
