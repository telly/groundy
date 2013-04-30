/**
 * Copyright Telly, Inc. and other Groundy contributors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.groundy.example;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.groundy.example.tasks.ChuckNorrisKick;
import com.telly.groundy.Groundy;
import com.telly.groundy.annotations.OnCallback;
import com.telly.groundy.annotations.OnFailed;
import com.telly.groundy.annotations.OnSuccess;
import com.telly.groundy.annotations.Param;
import com.telly.groundy.example.R;

public class CallbackTest extends Activity {

  private View mByeBtn;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.chuck_norris_kick);

    mByeBtn = findViewById(R.id.sayonara);
    mByeBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mByeBtn.setEnabled(false);
        Groundy.create(ChuckNorrisKick.class).callback(CallbackTest.this).queue(CallbackTest.this);
      }
    });
  }

  @OnCallback(value = ChuckNorrisKick.class, name = "kick")
  public void onChuckNorrisAttack(@Param("target") String target) {
    Toast.makeText(CallbackTest.this, getString(R.string.chuck_norris_kicked, target),
        Toast.LENGTH_SHORT).show();
  }

  @OnCallback(value = ChuckNorrisKick.class, name = "punch")
  public void onChuckNorrisPunch(@Param("target") String target) {
    Toast.makeText(CallbackTest.this, getString(R.string.chuck_norris_punched, target),
        Toast.LENGTH_SHORT).show();
  }

  @OnSuccess(ChuckNorrisKick.class)
  public void onChuckNorrisSuccess() {
    mByeBtn.setEnabled(true);
    Toast.makeText(CallbackTest.this, R.string.you_were_kicked, Toast.LENGTH_LONG).show();
  }

  @OnFailed(ChuckNorrisKick.class)
  public void onChuckNorrisFail(@Param("lifeExpectation") int lifeExpectation) {
    mByeBtn.setEnabled(true);
    Toast.makeText(CallbackTest.this, getString(R.string.lifespan, lifeExpectation),
        Toast.LENGTH_LONG).show();
  }
}
