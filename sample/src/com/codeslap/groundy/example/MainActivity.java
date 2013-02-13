package com.codeslap.groundy.example;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.codeslap.groundy.Groundy;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Groundy.setLogEnabled(false);

        findViewById(R.id.queue_example).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, QueueTest.class));
            }
        });

        findViewById(R.id.execute_example).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ExecuteTest.class));
            }
        });
    }
}
