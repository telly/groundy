package com.groundy.example;

import android.os.Bundle;
import com.codeslap.groundy.Groundy;
import com.groundy.example.tasks.RandomTimeTask;

public class ExecuteTest extends QueueTest {
    @Override
    protected void processTask(Bundle params) {
        Groundy.create(this, RandomTimeTask.class)
                .params(params)
                .service(AsyncGroundyService.class)
                .receiver(mReceiver)
                .execute();
    }
}
