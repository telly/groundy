package com.groundy.example;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.groundy.example.tasks.AnimalTask;
import com.groundy.example.tasks.DogTask;
import com.telly.groundy.Groundy;
import com.telly.groundy.annotations.OnStart;
import com.telly.groundy.annotations.OnSuccess;
import com.telly.groundy.annotations.Param;
import com.telly.groundy.example.R;

public class InheritanceCallbackExample extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.inheritance_callback_example);

    findViewById(R.id.send_random_task).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        v.setEnabled(false);

        Groundy.create(DogTask.class)
            .callback(new CallbackDog())
            .queueUsing(InheritanceCallbackExample.this);
      }
    });
  }

  public class CallbackBase {
    @OnStart(AnimalTask.class)
    public void startingAnimal() {
      Toast.makeText(InheritanceCallbackExample.this, R.string.starting_animal, Toast.LENGTH_SHORT).show();
    }
  }

  public class CallbackDog extends CallbackBase {
    @OnSuccess(DogTask.class)
    public void onSuccess(@Param("sound") String sound) {
      Toast.makeText(InheritanceCallbackExample.this, sound, Toast.LENGTH_SHORT).show();
      findViewById(R.id.send_random_task).setEnabled(true);
    }
  }
}