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

import com.squareup.javawriter.JavaWriter;
import com.sun.tools.javac.code.Symbol;
import com.telly.groundy.annotations.Param;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
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
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementKindVisitor6;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes({
    GroundyCodeGen.SUCCESS, GroundyCodeGen.FAILED, GroundyCodeGen.START, GroundyCodeGen.CANCEL,
    GroundyCodeGen.PROGRESS, GroundyCodeGen.CALLBACK
})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class GroundyCodeGen extends AbstractProcessor {

  private static final Logger LOGGER = setupLogger();
  public static final String SUCCESS = "com.telly.groundy.annotations.OnSuccess";
  public static final String FAILED = "com.telly.groundy.annotations.OnFailure";
  public static final String START = "com.telly.groundy.annotations.OnStart";
  public static final String CANCEL = "com.telly.groundy.annotations.OnCancel";
  public static final String PROGRESS = "com.telly.groundy.annotations.OnProgress";
  public static final String CALLBACK = "com.telly.groundy.annotations.OnCallback";
  public static final String GROUNDY_VERBOSE = "GROUNDY_VERBOSE";

  private final Map<HandlerAndTask, Set<ProxyImplContent>> implMap =
      new HashMap<HandlerAndTask, Set<ProxyImplContent>>();
  private boolean verboseMode;

  @Override
  public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment env) {
    if (typeElements.isEmpty()) {
      return true;
    }

    String groundyVerbose = System.getenv(GROUNDY_VERBOSE);
    verboseMode = String.valueOf(Boolean.TRUE).equals(groundyVerbose);

    for (TypeElement annotationElement : typeElements) {

      Set<? extends Element> annotatedElements = env.getElementsAnnotatedWith(annotationElement);
      for (Element annotatedElement : annotatedElements) {
        if (annotatedElement instanceof ExecutableElement) {
          // populate the proxy impl map
          ExecutableElement callbackMethod = (ExecutableElement) annotatedElement;
          processCallback(annotationElement, callbackMethod);
        }
      }
    }

    mergeProxiesWithClassHierarchy();

    for (Map.Entry<HandlerAndTask, Set<ProxyImplContent>> elementSetEntry : implMap.entrySet()) {
      HandlerAndTask proxyClassName = elementSetEntry.getKey();
      Set<ProxyImplContent> callbacks = elementSetEntry.getValue();
      generateProxy(proxyClassName, callbacks);
    }

    return true;
  }

  /**
   * Merges callbacks implementations taking into account the supper types of the handlers and
   * the tasks.
   * 1. It will look for callbacks of the super classes of the tasks, in the same handler.
   * 2. It will look for callbacks of the super classes of the handler, that use the same task.
   * 3. It will look for callbacks of the super classes of the handler, that use super classes of the task.
   */
  private void mergeProxiesWithClassHierarchy() {
    for (Map.Entry<HandlerAndTask, Set<ProxyImplContent>> elementSetEntry : implMap.entrySet()) {
      final HandlerAndTask handlerAndTask = elementSetEntry.getKey();

      // 1. merge super tasks, for same handler
      final Set<Element> superTasks = getSuperClasses(handlerAndTask.task);
      for (Element superTask : superTasks) {
        final HandlerAndTask handlerAndSuperTask = new HandlerAndTask(handlerAndTask.handler, superTask);
        if (implMap.containsKey(handlerAndSuperTask)) {
          appendNonExistentCallbacks(handlerAndSuperTask, handlerAndTask);
        }
      }

      // 2. merge super handlers, for same task
      final Set<Element> superHandlers = getSuperClasses(handlerAndTask.handler);
      for (Element superHandler : superHandlers) {
        final HandlerAndTask superHandlerAndTask = new HandlerAndTask(superHandler, handlerAndTask.task);
        if (implMap.containsKey(superHandlerAndTask)) {
          appendNonExistentCallbacks(superHandlerAndTask, handlerAndTask);
        }

        // 3. merge super handlers, for super tasks
        for (Element superTask : superTasks) {
          final HandlerAndTask superHandlerAndSuperTask = new HandlerAndTask(superHandler, superTask);
          if (implMap.containsKey(superHandlerAndSuperTask)) {
            appendNonExistentCallbacks(superHandlerAndSuperTask, handlerAndTask);
          }
        }
      }
    }
  }

  /**
   * Copy all callbacks implementations from -> to the specified set. It makes sure
   * to copy the callbacks that don't exist already on the destination set.
   *
   * @param from key for the set of callbacks to copy from
   * @param to   key for the set of callbacks to copy to
   */
  private void appendNonExistentCallbacks(HandlerAndTask from, HandlerAndTask to) {
    final Set<ProxyImplContent> proxyImplContentsTo = implMap.get(to);
    if (proxyImplContentsTo == null) {
      return;
    }

    final Set<ProxyImplContent> proxyImplContentsFrom = implMap.get(from);
    if (proxyImplContentsFrom == null) {
      return;
    }

    for (ProxyImplContent proxyImplContentFrom : proxyImplContentsFrom) {
      boolean exists = false;
      for (ProxyImplContent proxyImplContentTo : proxyImplContentsTo) {
        if (proxyImplContentTo.annotation.equals(proxyImplContentFrom.annotation)) {
          exists = true;
          break;
        }
      }

      if (!exists) {
        proxyImplContentsTo.add(proxyImplContentFrom);
      }
    }
  }

  private Set<Element> getSuperClasses(Element callbackElement) {
    final Set<Element> superHierarchy = new HashSet<Element>();
    callbackElement.accept(new SuperClassVisitor(), superHierarchy);
    return superHierarchy;
  }

  private void processCallback(Element annotationElement, ExecutableElement callbackMethod) {
    // ignore inner classes and non public classes
    Element handlerElement = callbackMethod.getEnclosingElement();
    LOGGER.info("Processing callback element: " + handlerElement);

    if (!handlerElement.getModifiers().contains(Modifier.PUBLIC)) {
      LOGGER.info(
          handlerElement + " is not public. Reflection will be used and it can slow things down.");
      return;
    }

    //noinspection unchecked
    Collection<AnnotationValue> tasksList = (Collection<AnnotationValue>)
        getAnnotationValue(callbackMethod, annotationElement, "value");
    if (tasksList == null) {
      LOGGER.info("Could not found task for " + annotationElement);
      System.exit(1);
    }

    for (AnnotationValue attribute : tasksList) {
      Object callbackName = getAnnotationValue(callbackMethod, annotationElement, "name");
      Element taskElement;
      if (!(attribute.getValue() instanceof DeclaredType)) {
        continue;
      }

      taskElement = ((DeclaredType) attribute.getValue()).asElement();

      // generate proxy class name
      final HandlerAndTask handlerAndTask = new HandlerAndTask(handlerElement, taskElement);

      Set<ProxyImplContent> proxyImplContents;
      if (implMap.containsKey(handlerAndTask)) {
        proxyImplContents = implMap.get(handlerAndTask);
      } else {
        proxyImplContents = new HashSet<ProxyImplContent>();
        implMap.put(handlerAndTask, proxyImplContents);
      }

      ProxyImplContent proxyImplContent = new ProxyImplContent();
      proxyImplContent.annotation = annotationElement.toString();
      proxyImplContent.paramNames = getParamNames(callbackMethod);
      proxyImplContent.methodName = callbackMethod.getSimpleName().toString();
      proxyImplContent.callbackElement = handlerElement;
      proxyImplContent.taskElement = taskElement;
      proxyImplContent.fullTargetClassName = handlerElement.toString();
      proxyImplContent.callbackName = callbackName != null ? callbackName.toString() : null;
      proxyImplContent.originatingElement = annotationElement;

      LOGGER.info("Adding proxy implementation: " + proxyImplContent);

      proxyImplContents.add(proxyImplContent);
    }
  }

  public class SuperClassVisitor extends ElementKindVisitor6<Void, Set<Element>> {
    @Override
    public Void visitType(TypeElement typeElement, Set<Element> elements) {
      final TypeMirror superclass = typeElement.getSuperclass();
      Types types = processingEnv.getTypeUtils();
      final Element superElement = types.asElement(superclass);
      if (superElement != null && !isObjectElement(superElement)) {
        elements.add(superElement);
        superElement.accept(this, elements);
      }
      return null;
    }
  }

  private static boolean isObjectElement(Element element) {
    if (element == null) {
      return false;
    }
    if (element instanceof Symbol.ClassSymbol) {
      Symbol.ClassSymbol classSymbol = (Symbol.ClassSymbol) element;
      return classSymbol.getQualifiedName().contentEquals("java.lang.Object");
    }
    return "java.lang.Object".equals(element.toString());
  }

  private Object getAnnotationValue(Element callbackMethod, Element annotationElement,
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
        AnnotationValue annotationValue = annotation.getElementValues().get(executableElement);
        return annotationValue.getValue();
      }
    }
    return null;
  }

  private void generateProxy(HandlerAndTask handlerAndTask, Set<ProxyImplContent> callbacks) {
    StringWriter classContent = new StringWriter();
    JavaWriter javaWriter = new JavaWriter(classContent);
    try {
      if (verboseMode) {
        ProxyImplContent[] callbacksArr = callbacks.toArray(new ProxyImplContent[callbacks.size()]);
        LOGGER.info("Generating source code for " + callbacksArr[0].fullTargetClassName + ":");
      }

      javaWriter.emitSingleLineComment("auto-generated file; don't modify");
      javaWriter.emitPackage("com.telly.groundy.generated");
      javaWriter.emitImports("android.os.Bundle", Annotation.class.getName(),
          "com.telly.groundy.*");

      String proxyClassName = handlerAndTask.generateClassName();
      javaWriter.beginType(proxyClassName, "class", EnumSet.of(Modifier.PUBLIC), null,
          "ResultProxy");

      javaWriter.beginMethod("void", "apply", EnumSet.of(Modifier.PUBLIC), "Object", "target",
          "Class<? extends Annotation>", "callbackAnnotation", "Bundle", "resultData");

      // make sure the target and result is OK
      String fullTargetClassName = callbacks.iterator().next().fullTargetClassName;
      String targetIsValid = "!(target instanceof " + fullTargetClassName + ")";
      String resultIsValid = "resultData == null";
      javaWriter.beginControlFlow("if(" + targetIsValid + " || " + resultIsValid + ")");
      javaWriter.emitStatement("return");
      javaWriter.endControlFlow();

      boolean alreadySetCallbackName = false;


      List<Element> originatingElements = new ArrayList<Element>();
      for (ProxyImplContent proxyImpl : callbacks) {
        originatingElements.add(proxyImpl.originatingElement);
        if (verboseMode) {
          LOGGER.info("Adding annotation proxy: " + proxyImpl.annotation);
        }
        String shouldHandleAnnotation = "callbackAnnotation == " + proxyImpl.annotation + ".class";
        if (proxyImpl.callbackName != null) {
          if (!alreadySetCallbackName) {
            javaWriter.emitStatement("String callbackName = "
                + "resultData.getString(\"" + Groundy.KEY_CALLBACK_NAME + "\")");
            alreadySetCallbackName = true;
          }
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

      if (verboseMode) {
        LOGGER.info("Generated file: " + proxyClassName + ".java");
        System.out.println(fileContent);
      }

      Filer filer = processingEnv.getFiler();
      Element[] elements = originatingElements.toArray(new Element[originatingElements.size()]);
      String fullClassName = "com.telly.groundy.generated." + proxyClassName;
      JavaFileObject sourceFile = filer.createSourceFile(fullClassName, elements);
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
        || "Integer".equals(parameterType)
        || parameterType.equals(float.class.getName())
        || "Float".equals(parameterType)
        || parameterType.equals(double.class.getName())
        || "Double".equals(parameterType)
        || parameterType.equals(long.class.getName())
        || "Long".equals(parameterType)
        || parameterType.equals(byte.class.getName())
        || "Byte".equals(parameterType)
        || parameterType.equals(char.class.getName())
        || "Character".equals(parameterType)
        || parameterType.equals(short.class.getName())
        || "Short".equals(parameterType)) {
      return "0";
    } else if (parameterType.equals(boolean.class.getName()) || "Boolean".equals(parameterType)) {
      return "false";
    }
    return "null";
  }

  private String cleanCasting(String type) {
    if (PRIMITIVES.containsKey(type)) {
      return PRIMITIVES.get(type);
    }
    return type;
  }

  /**
   * Makes sure method is public, returns void and all its parameters are annotated too.
   */
  private static List<NameAndType> getParamNames(Element callbackMethod) {
    Element parentClass = callbackMethod.getEnclosingElement();
    String methodFullInfo = parentClass + "#" + callbackMethod;

    ExecutableElement method = (ExecutableElement) callbackMethod;

    if (!method.getModifiers().contains(Modifier.PUBLIC)) {
      LOGGER.info(methodFullInfo + " must be public.");
      System.exit(-1);
    }

    if (method.getReturnType().getKind() != TypeKind.VOID) {
      LOGGER.info(methodFullInfo + " must return void.");
      System.exit(-1);
    }

    List<NameAndType> paramNames = new ArrayList<NameAndType>();
    for (VariableElement param : method.getParameters()) {
      Param paramAnnotation = param.getAnnotation(Param.class);
      if (paramAnnotation == null) {
        LOGGER.info(methodFullInfo
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
          @Override
          public synchronized String format(LogRecord r) {
            return "[GROUNDY] " + r.getMessage() + "\n";
          }
        };
        handler.setFormatter(newFormatter);
      }
    }
    return logger;
  }

  private static final class NameAndType {
    final String name;
    final String type;

    private NameAndType(String n, String t) {
      this.name = n;
      this.type = t;
    }

    @Override
    public String toString() {
      return "NameAndType{" +
          "name='" + name + '\'' +
          ", type='" + type + '\'' +
          '}';
    }
  }

  private static class ProxyImplContent {
    String annotation;
    List<NameAndType> paramNames;
    String methodName;
    String fullTargetClassName;
    String callbackName;
    Element originatingElement;
    Element callbackElement;
    Element taskElement;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ProxyImplContent that = (ProxyImplContent) o;

      return !(annotation != null ? !annotation.equals(that.annotation) : that.annotation != null)
          && !(callbackName != null ? !callbackName.equals(that.callbackName)
          : that.callbackName != null);
    }

    @Override
    public int hashCode() {
      int result = annotation != null ? annotation.hashCode() : 0;
      result = 31 * result + (callbackName != null ? callbackName.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      return "ProxyImplContent{" +
          "annotation='" + annotation + '\'' +
          ", paramNames=" + paramNames +
          ", methodName='" + methodName + '\'' +
          ", fullTargetClassName='" + fullTargetClassName + '\'' +
          ", callbackName='" + callbackName + '\'' +
          ", originatingElement=" + originatingElement +
          '}';
    }
  }

  private static class HandlerAndTask {
    final Element handler;
    final Element task;

    private HandlerAndTask(Element handler, Element task) {
      this.handler = handler;
      this.task = task;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      HandlerAndTask that = (HandlerAndTask) o;

      return !(handler != null ? !handler.equals(that.handler) : that.handler != null) &&
          !(task != null ? !task.equals(that.task) : that.task != null);
    }

    @Override
    public int hashCode() {
      int result = handler != null ? handler.hashCode() : 0;
      result = 31 * result + (task != null ? task.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      return "HandlerAndTask{" +
          "handler=" + handler +
          ", task=" + task +
          '}';
    }

    public String generateClassName() {
      String genClassName = handler.getSimpleName().toString();
      String groundyTaskName = task.toString().replaceAll("\\.", "\\$");
      return genClassName + "$" + groundyTaskName + "$Proxy";
    }
  }

  private static final Map<String, String> PRIMITIVES = new HashMap<String, String>();

  static {
    PRIMITIVES.put("int", "Integer");
    PRIMITIVES.put("long", "Long");
    PRIMITIVES.put("float", "Float");
    PRIMITIVES.put("double", "Double");
    PRIMITIVES.put("char", "Character");
    PRIMITIVES.put("boolean", "Boolean");
    PRIMITIVES.put("byte", "Byte");
    PRIMITIVES.put("short", "Short");
  }
}
