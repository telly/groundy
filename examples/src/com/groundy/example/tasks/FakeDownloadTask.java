package com.groundy.example.tasks;

import com.telly.groundy.GroundyTask;
import com.telly.groundy.TaskResult;

public class FakeDownloadTask extends GroundyTask {
  @Override protected TaskResult doInBackground() {
    int time = 15000;

    int interval = time / 100;
    int currentPercentage = 0;
    while (currentPercentage <= 100) {
      try {
        if (isQuitting()) {
          return cancelled();
        }
        updateProgress(currentPercentage);

        // let's fake some work ^_^
        Thread.sleep(interval);
        currentPercentage++;
      } catch (InterruptedException e) {
      }
    }
    return succeeded();
  }
}
