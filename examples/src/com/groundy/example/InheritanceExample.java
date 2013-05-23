package com.groundy.example;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.groundy.example.tasks.AnimalTask;
import com.groundy.example.tasks.CatTask;
import com.groundy.example.tasks.DogTask;
import com.telly.groundy.Groundy;
import com.telly.groundy.annotations.OnSuccess;
import com.telly.groundy.annotations.Param;
import com.telly.groundy.example.R;

public class InheritanceExample extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.inheritance_example);

    findViewById(R.id.send_random_task).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        v.setEnabled(false);

        Groundy.create(CatTask.class)
            .callback(InheritanceExample.this)
            .queue(InheritanceExample.this);

        Groundy.create(DogTask.class)
            .callback(InheritanceExample.this)
            .queue(InheritanceExample.this);
      }
    });
  }

  // we are using AnimalTask (super class of CatTask and DogTask)
  @OnSuccess({ AnimalTask.class, DogTask.class })
  public void onSuccess(@Param("sound") String sound,
      @Param(Groundy.TASK_IMPLEMENTATION) Class<?> impl) {
    if (impl == DogTask.class) findViewById(R.id.send_random_task).setEnabled(true);
    Toast.makeText(InheritanceExample.this, sound, Toast.LENGTH_SHORT).show();
  }
}