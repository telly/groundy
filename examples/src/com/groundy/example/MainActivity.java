package com.groundy.example;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.codeslap.groundy.GroundyManger;
import com.codeslap.groundy.example.R;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        GroundyManger.setLogEnabled(false);

        findViewById(R.id.simple_example).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SimpleTaskTest.class));
            }
        });

        findViewById(R.id.safe_simple_example).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SafeSimpleTaskTest.class));
            }
        });

        findViewById(R.id.attach_receiver_example).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AttachReceiverExample.class));
            }
        });

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
