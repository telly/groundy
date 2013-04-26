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
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes(
    {GroundyAnnotationsProcessor.SUCCESS, GroundyAnnotationsProcessor.FAILED, GroundyAnnotationsProcessor.START, GroundyAnnotationsProcessor.CANCEL, GroundyAnnotationsProcessor.PROGRESS})
public class GroundyAnnotationsProcessor extends AbstractProcessor {

  private static final Logger logger = setupLogger();
  public static final String SUCCESS = "com.telly.groundy.annotations.OnSuccess";
  public static final String FAILED = "com.telly.groundy.annotations.OnFailed";
  public static final String START = "com.telly.groundy.annotations.OnStart";
  public static final String CANCEL = "com.telly.groundy.annotations.OnCancel";
  public static final String PROGRESS = "com.telly.groundy.annotations.OnProgress";

  private final Map<String, Set<ProxyImplContent>> implMap = new HashMap<String, Set<ProxyImplContent>>();

  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment env) {
    if (typeElements.isEmpty()) {
      return true;
    }
    for (TypeElement annotationElement : typeElements) {
      Set<? extends Element> annotatedElements = env.getElementsAnnotatedWith(annotationElement);
      for (Element callbackMethod : annotatedElements) {
        // ignore inner classes and non public classes
        Element callbackElement = callbackMethod.getEnclosingElement();
        if (!callbackElement.getModifiers().contains(Modifier.PUBLIC)) {
          logger.info(
              callbackElement + " is not public. Reflection will be used and it can slow things down.");
          continue;
        }

        String groundyTaskName = getGroundyTaskNameFrom(annotationElement, callbackMethod);
        if (groundyTaskName == null) {
          logger.info("Could not found task for " + annotationElement);
          System.exit(1);
        }

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

        proxyImplContents.add(proxyImplContent);
      }
    }

    for (Map.Entry<String, Set<ProxyImplContent>> elementSetEntry : implMap.entrySet()) {
      String proxyClassName = elementSetEntry.getKey();
      Set<ProxyImplContent> callbacks = elementSetEntry.getValue();
      generateProxy(proxyClassName, callbacks);
    }

    return true;
  }

  private String getGroundyTaskNameFrom(TypeElement annotationElement, Element callbackMethod) {
    String groundyTaskName = null;
    List<? extends AnnotationMirror> annotationMirrors = callbackMethod.getAnnotationMirrors();
    for (AnnotationMirror annotationMirror : annotationMirrors) {
      if (annotationMirror.getAnnotationType().equals(annotationElement.asType())) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> u : annotationMirror.getElementValues()
            .entrySet()) {
          AnnotationValue value = u.getValue();
          groundyTaskName = value.getValue().toString().replaceAll("\\.", "\\$");
        }
      }
    }
    return groundyTaskName;
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

      for (ProxyImplContent proxyImpl : callbacks) {
        javaWriter.beginControlFlow(
            "if (callbackAnnotation == " + proxyImpl.annotation + ".class)");

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
      System.out.println(fileContent);

      JavaFileObject sourceFile = processingEnv.getFiler()
          .createSourceFile(proxyClassName, (Element) null);
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
    if (parameterType.equals(int.class.getName()) || parameterType.equals("Integer")
      || parameterType.equals(float.class.getName()) || parameterType.equals("Float")
      || parameterType.equals(double.class.getName()) || parameterType.equals("Double")
      || parameterType.equals(long.class.getName()) || parameterType.equals("Long")
      || parameterType.equals(byte.class.getName()) || parameterType.equals("Byte")
      || parameterType.equals(char.class.getName()) || parameterType.equals("Character")
      || parameterType.equals(short.class.getName()) || parameterType.equals("Short")) {
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
        logger.info(
            methodFullInfo + ": all parameters must be annotated with the @" + Param.class.getName());
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

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ProxyImplContent that = (ProxyImplContent) o;

      if (annotation != null ? !annotation.equals(that.annotation) : that.annotation != null)
        return false;

      return true;
    }

    @Override
    public int hashCode() {
      return annotation != null ? annotation.hashCode() : 0;
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
