Groundy library for Android
===========================

A bunch of boilerplate code that is used when creating apps that must call external
services (such REST APIs). This will basically offer a service that will take care
of create background threads to execute that kind of code (right away or queue it).
This library is heavily inspired in a Google I/O 2010 presentation by Virgil Dobjanschi
called [Android REST client applications][1]; you might want to take a look at it.

###Maven integration

In order to use this library from you Android project using maven your pom should look like this:

```xml
<dependency>
    <groupId>com.codeslap</groupId>
    <artifactId>groundy</artifactId>
    <version>0.6</version>
    <scope>compile</scope>
</dependency>
```
###Basic usage

First of all add the `GroundyService` to the `AndroidManifest.xml` file:

```xml
<service android:name="com.codeslap.groundy.GroundyService"/>
```

Then, create a subclass of `GroundyTask`:

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
// this is usually perform from within an Activity
Bundle params = new Bundler().add(ExampleTask.PARAM_EXAMPLE, "foo").build();
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

Extras
======

Besides the background service feature, there are a couple of extra cool classes you might want to use:

###ListLoader

This is basically a content `Loader` that can be used with the `LoaderManager` class. It allows you to easily
return a list of objects from within the loader. An example of its usage can be found in [GitHub Jobs][2] app.

###Generic BaseAdapter

How many times have you written a `BaseAdapter` your self? Do you use the `ViewHolder` technique that makes
list faster? It gets kind of boring sometimes, ain't? There's a class called `ListBaseAdapter` you
can use to easily create an adapter:

```java
public class ExampleAdapter extends ListBaseAdapter<ExampleItem, ViewHolder> {

    public ExampleAdapter(Context context) {
        super(context, ViewHolder.class);
    }

    @Override
    public void populateHolder(int position, View view, ViewGroup parent, ExampleItem item, ViewHolder holder) {
        holder.icon.setImageBitmap(getItem(position).getIcon());
        holder.label.setText(getItem(position).getLabel());
    }

    @Layout(R.layout.your_layout)
    public static class ViewHolder {
        @ResourceId(R.id.label) TextView label;
        @ResourceId(R.id.image) ImageView icon;
    }
}
```

###Real-life example

If you want to see a real life example, take a look at [GitHub Jobs][3] app. Also, there is an example of
how to do a file downloader in this [StackOverflow's answer][4].

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

  [1]: http://www.youtube.com/watch?v=xHXn3Kg2IQE
  [2]: https://github.com/casidiablo/github-jobs/blob/master/android-app/src/com/github/jobs/loader/TemplatesLoader.java
  [3]: https://github.com/casidiablo/github-jobs
  [4]: http://stackoverflow.com/questions/3028306/download-a-file-with-android-and-showing-the-progress-in-a-progressdialog/3028660#3028660