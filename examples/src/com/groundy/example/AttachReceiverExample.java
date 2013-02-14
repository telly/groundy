package com.groundy.example;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.codeslap.groundy.DetachableResultReceiver;
import com.codeslap.groundy.Groundy;
import com.codeslap.groundy.GroundyManger;
import com.codeslap.groundy.example.R;
import com.codeslap.groundy.util.Bundler;
import com.groundy.example.tasks.RandomTimeTask;

import java.util.Random;

public class AttachReceiverExample extends Activity {

    private Button mBtnAddTask;

    private DetachableResultReceiver mDetachableReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_example);

        TextView explanation = (TextView) findViewById(R.id.simple_example_explanation);
        explanation.setText(R.string.safe_example_explanation);

        if (savedInstanceState != null) {
            mDetachableReceiver = savedInstanceState.getParcelable("receiver");
        } else {
            mDetachableReceiver = new DetachableResultReceiver(new Handler());
        }
        mDetachableReceiver.setReceiver(mReceiver);

        GroundyManger.attachReceiver(this, "the_token", mDetachableReceiver);

        mBtnAddTask = (Button) findViewById(R.id.send_random_task);
        if (savedInstanceState != null) {
            mBtnAddTask.setEnabled(savedInstanceState.getBoolean("is_button_enabled"));
        }

        mBtnAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtnAddTask.setEnabled(false);

                // configure task parameters
                int time = new Random().nextInt(10000);
                Bundle params = new Bundler().add(RandomTimeTask.KEY_ESTIMATED, time).build();
                Toast.makeText(AttachReceiverExample.this, getString(R.string.task_will_take_x, time), Toast.LENGTH_SHORT).show();

                // queue task
                Groundy.create(AttachReceiverExample.this, RandomTimeTask.class)
                        .receiver(mDetachableReceiver)
                        .token("the_token")
                        .params(params)
                        .queue();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("is_button_enabled", mBtnAddTask.isEnabled());
        outState.putParcelable("receiver", mDetachableReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDetachableReceiver.clearReceiver();
        GroundyManger.detachReceiver(this, "the_token", mDetachableReceiver);
    }

    private final DetachableResultReceiver.Receiver mReceiver = new DetachableResultReceiver.Receiver() {
        @Override
        public void onReceiveResult(int resultCode, Bundle resultData) {
            if (resultCode == Groundy.STATUS_FINISHED) {
                mBtnAddTask.setText("Got something");
                mBtnAddTask.setEnabled(true);
                Toast.makeText(AttachReceiverExample.this, R.string.task_finished, Toast.LENGTH_LONG).show();
            }
        }
    };
}
