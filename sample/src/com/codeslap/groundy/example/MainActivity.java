package com.codeslap.groundy.example;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import com.codeslap.groundy.Groundy;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Groundy.setLogEnabled(false);


        findViewById(R.id.run).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Groundy.create(MainActivity.this, DummyTask.class).queue();
                Groundy.create(MainActivity.this, DummyTask.class).execute();
                Groundy.create(MainActivity.this, DummyTask.class).execute();
                Groundy.create(MainActivity.this, DummyTask.class).group(2).execute();
            }
        });

        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Groundy.cancelTasks(MainActivity.this, 2);
            }
        });
    }
}
