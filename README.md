Groundy library for Android
===========================

![Groundy](http://i.imgur.com/fgC2aaw.png)

Groundy is a fun, sexy way to do background work on your Android app; it's specially useful for
running tasks that must be executed even if your activities are rotated or even quited. It allows
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
    String exampleParam = getStringParam("key_name");

    // lots of code

    // return a TaskResult depending on the success of your task
    // and optionally pass some results back
    Succeeded result = new Succeeded();
    result.add("the_result", "some result");
    return result;
  }
}
```

Whenever you want to execute the task, just do this:

```java
// this is usually performed from within an Activity
Bundle params = new Bundler().add("key_name", "foo").build();
Groundy.create(this, ExampleTask.class)
    .callback(YourActivity.this)  // required if you want to get notified of your task lifecycle
    .params(params)               // optional
    .queue();
```

You will get results in your result receiver (in the main thread):

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

There are some already defined callback annotations: `@OnSuccess`, `@OnFailed`, `@OnCancel`,
`@OnProgress` and `@OnStart`, but you can also create your own callback types. Take a look
at the Custom callbacks example and learn how you can write callbacks like:

```java
@OnChuckNorris
public void onChuckNorrisAttack(@Param("target") String target) {
  Toast.makeText(this, "Chuck Norris kicked your " + target, Toast.LENGTH_SHORT).show();
}
```

Maven integration
=================

In order to use this library from you Android project using maven your pom should look like this:

```xml
<dependency>
  <groupId>com.telly</groupId>
  <artifactId>groundy</artifactId>
  <version>0.9-SNAPSHOT</version>
  <scope>compile</scope>
</dependency>
```
