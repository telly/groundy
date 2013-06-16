package com.groundy.example.tasks;

import com.telly.groundy.GroundyTask;
import com.telly.groundy.TaskResult;
import java.util.Random;

public abstract class AnimalTask extends GroundyTask {

  @Override protected TaskResult doInBackground() {
    int time = new Random().nextInt(5000);
    if (time < 1000) {
      time = 1000;
    }

    try {
      // let's fake some work ^_^
      Thread.sleep(time);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return succeeded().add("sound", getSound());
  }

  protected abstract String getSound();
}
