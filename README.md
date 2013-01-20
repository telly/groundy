Groundy library for Android
===========================

A bunch of boilerplate code that is used when creating apps that must call external
services (such REST APIs). This will basically offer a service that will take care
of create background threads to execute that kind of code (right away or queue it).

###Maven integration

In order to use this library from you Android project using maven your pom should look like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project ...>
    <dependencies>
        <dependency>
            <groupId>com.codeslap</groupId>
            <artifactId>groundy</artifactId>
            <version>0.5</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>
</project>
```
###Basic usage

First of all add the `GroundyService` to the `AndroidManifest.xml` file:

```xml
<service android:name="com.codeslap.groundy.GroundyService"/>
```

Then, create a subclass of `GroundyTask`:

```java
import android.os.Bundle;
import com.codeslap.groundy.GroundyTask;
import com.codeslap.groundy.Groundy;

public class ExampleTask extends GroundyTask {
    public static final String PARAM_EXAMPLE = "com.example.param.EXAMPLE";
    public static final String RESULT_EXAMPLE = "com.example.result.EXAMPLE";

    @Override
    protected void updateData() {
        // use params
        Bundle parameters = getParameters();
        String exampleParam = parameters.getString(PARAM_EXAMPLE);

        // lots of code

        // set the result
        if (mSuccess) {
            Bundle resultData = getResultData();
            resultData.putString(RESULT_EXAMPLE, "some result");
            setResultCode(Groundy.STATUS_FINISHED);
        } else {
            setResultCode(Groundy.STATUS_ERROR);
        }
    }
}
```

Whenever you want to execute the task, just do this:

```java
// this is usually perform from within an Activity
Bundle params = new Bundle();
params.putString(ExampleTask.PARAM_EXAMPLE, "some parameter");
Groundy.queue(this, ExampleTask.class, mResultReceiver, params);
```

You will get results in your result receiver (in the main thread):

```java
private final ResultReceiver receiver = new ResultReceiver(new Handler()){
    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        super.onReceiveResult(resultCode, resultData);
        String result = resultData.getString(ExampleTask.RESULT_EXAMPLE);
        // do something
    }
};
```

License
=======

    Copyright 2012 CodeSlap

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.