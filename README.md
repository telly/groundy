Groundy library for Android
===========================

Groundy is a fancy implementation of the 'Service + ResultReceiver' technique which
is heavily inspired in a Google I/O 2010 presentation by Virgil Dobjanschi
called [Android REST client applications][1]. Groundy is useful in any kind of apps
that requires the use of background threads:

- Execute calls to external services (e.g. RESTful web services)
- Download and/or process files
- Encoding audio/video
- Any kind of task that could block the main thread

Groundy offers a special `Service` that will take care of creating background
threads and allows to report results to the `Activity` using a `ResultReceiver`.
It will also force you to decouple the background work logic from your activities,
which in turn makes your code cleaner and easier to maintain.

Basic usage
===========

Create a subclass of `GroundyTask`:

```java
public class ExampleTask extends GroundyTask {
    @Override
    protected boolean doInBackground() {
        // you can send parameters to the task using a Bundle (optional)
        String exampleParam = getStringParam("key_name");

        // lots of code

        // add results... this will be sent back to the activity
        // through the ResultReceiver once this method has returned
        addStringResult("the_result", "some result");

        return success; // true if task was executed successfully
    }
}
```

Whenever you want to execute the task, just do this:

```java
// this is usually performed from within an Activity
Bundle params = new Bundler().add("key_name", "foo").build();
Groundy.create(this, ExampleTask.class)
       .receiver(receiver) // optional
       .params(params)     // optional
       .queue();
```

You will get results in your result receiver (in the main thread):

```java
private final ResultReceiver receiver = new ResultReceiver(new Handler()){
    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        String result = resultData.getString("the_result");
        // do something
    }
};
```

Do not forget to add `GroundyService` to the `AndroidManifest.xml` file:

```xml
<service android:name="com.codeslap.groundy.GroundyService"/>
```

Maven integration
=================

In order to use this library from you Android project using maven your pom should look like this:

```xml
<dependency>
    <groupId>com.codeslap</groupId>
    <artifactId>groundy</artifactId>
    <version>0.7</version>
    <scope>compile</scope>
</dependency>
```

If you don't use Maven, you can download this jar: [groundy-0.7.jar][2]

License
=======

>Copyright 2012 Twitvid Inc.
>
>Copyright 2012-2013 Cristian Castiblanco
>
>Licensed under the Apache License, Version 2.0 (the "License");
>you may not use this file except in compliance with the License.
>You may obtain a copy of the License at
>
>  http://www.apache.org/licenses/LICENSE-2.0
>
>Unless required by applicable law or agreed to in writing, software
>distributed under the License is distributed on an "AS IS" BASIS,
>WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
>See the License for the specific language governing permissions and
>limitations under the License.

  [1]: http://www.youtube.com/watch?v=xHXn3Kg2IQE
  [2]: https://github.com/downloads/casidiablo/groundy/groundy-0.7.jar