package com.groundy.example.tasks;

public class CatTask extends AnimalTask {
  @Override protected String getSound() {
    return "meow-meow";
  }
}
