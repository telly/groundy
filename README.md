[![Build Status](https://travis-ci.org/casidiablo/groundy.png?branch=develop)](https://travis-ci.org/casidiablo/groundy)
Groundy library for Android
===========================

Groundy is a fancy implementation of the 'Service + ResultReceiver' technique which
is heavily inspired in a Google I/O 2010 presentation by Virgil Dobjanschi
called [Android REST client applications][1].

Groundy is useful in any kind of apps that requires the use of background threads:

- Execute calls to external services (e.g. RESTful web services)
- Download and/or process files
- Encoding audio/video
- Any kind of task that could block the main thread

Groundy offers a special `Service` that will take care of creating background
threads and allows to report results to the `Activity` using a `ResultReceiver`.

Basic usage
===========

Create a subclass of `GroundyTask`:

```java
public class ExampleTask extends GroundyTask {
    public static final String PARAM_EXAMPLE = "com.example.param.EXAMPLE";
    public static final String RESULT_EXAMPLE = "com.example.result.EXAMPLE";

    @Override
    protected boolean doInBackground() {
        // use params
        String exampleParam = getStringParam(PARAM_EXAMPLE);

        // lots of code

        // add results... this will be sent back to the activity
        // through the ResultReceiver once this method has returned
        addStringResult(RESULT_EXAMPLE, "some result");

        return success; // true if task was executed successfully
    }
}
```

Whenever you want to execute the task, just do this:

```java
// this is usually performed from within an Activity
Bundle params = new Bundler().add(ExampleTask.PARAM_EXAMPLE, "foo").build();
Groundy.create(this, ExampleTask.class)
       .receiver(receiver)
       .params(params)
       .queue();
```

You will get results in your result receiver (in the main thread):

```java
private final ResultReceiver receiver = new ResultReceiver(new Handler()){
    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        String result = resultData.getString(ExampleTask.RESULT_EXAMPLE);
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
    <version>0.6</version>
    <scope>compile</scope>
</dependency>
```

License
=======

>Copyright 2013 CodeSlap
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