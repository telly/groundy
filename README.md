Groundy library for Android
===========================

![Groundy](http://i.imgur.com/2xg2weE.png)

Groundy is a fun, sexy way to do background work on your Android app; it's specially useful for
running tasks that must be executed even if your activities/fragments are destroyed. It allows
you to receive notifications from the background task directly to your activity or any object.

It is useful for several scenarios like executing calls to external services (e.g. RESTful web
services), download and/or process files, encoding audio/video and any kind of task that could
block the main thread.

Basic usage
===========

Create a subclass of `GroundyTask`:

```java
public class ExampleTask extends GroundyTask {
  @Override
  protected TaskResult doInBackground() {
    // you can send parameters to the task using a Bundle (optional)
    String exampleParam = getStringArg("arg_name");

    // lots of code

    // return a TaskResult depending on the success of your task
    // and optionally pass some results back
    return succeeded().add("the_result", "some result");
  }
}
```

Whenever you want to execute the task, just do this:

```java
// this is usually performed from within an Activity
Groundy.create(ExampleTask.class)
    .callback(callbackObj)        // required if you want to get notified of your task lifecycle
    .arg("arg_name", "foo")       // optional
    .queueUsing(YourActivity.this);
```

You will get results in your callback object(s) (in the main thread):

```java
@OnSuccess(ExampleTask.class)
public void onSuccess(@Param("the_result") String result) {
  // do something with the result
}
```

Do not forget to add `GroundyService` to the `AndroidManifest.xml` file:

```xml
<service android:name="com.telly.groundy.GroundyService"/>
```

Extending callback system
=========================

There are some already defined onCallback annotations: `@OnSuccess`, `@OnFailed`, `@OnCancel`,
`@OnProgress` and `@OnStart`, but you can also create your own callback types like:

```java
@OnCallback(task = ChuckTask.class, name = "kick")
public void onChuckNorrisAttack(@Param("target") String target) {
  Toast.makeText(this, "Chuck Norris kicked your " + target, Toast.LENGTH_SHORT).show();
}
```

Take a look at the custom callbacks example for details on this.

Integration
===========

In order to use this library from you Android project using **Maven** your pom should look like this:

```xml
<dependency>
  <groupId>com.telly</groupId>
  <artifactId>groundy</artifactId>
  <version>(insert latest version)</version>
</dependency>

<!-- enables groundy JSR-269 processor which makes everything up to 5 times faster -->
<dependency>
  <groupId>com.telly</groupId>
  <artifactId>groundy-compiler</artifactId>
  <version>(insert latest version)</version>
</dependency>
```

For **Gradle** projects use:

```groovy
compile 'com.telly:groundy:(insert latest version)'
provided 'com.telly:groundy-compiler:(insert latest version)'
```
Note: `provided` dependencies are supported by android gradle plugin v0.8 or later.

At this point latest version is `1.5`.

Proguard
========

If you are using proguard, please add these rules

```txt
-keepattributes *Annotation*

-keepclassmembers,allowobfuscation class * {
    @com.telly.groundy.annotations.* *;
    <init>();
}

-keepnames class com.telly.groundy.generated.*
-keep class com.telly.groundy.generated.*
-keep class com.telly.groundy.ResultProxy
-keepnames class * extends com.telly.groundy.ResultProxy
-keep class * extends com.telly.groundy.GroundyTask
```
