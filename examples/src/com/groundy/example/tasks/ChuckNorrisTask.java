/*
 * Copyright 2013 Telly Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.groundy.example.tasks;

import com.github.kevinsawicki.http.HttpRequest;
import com.telly.groundy.GroundyTask;
import org.json.JSONObject;

/**
 * @author Cristian Castiblanco <cristian@elhacker.net>
 */
public class ChuckNorrisTask extends GroundyTask {
  @Override
  protected boolean doInBackground() {
    try {
      String jsonBody = HttpRequest.get("http://api.icndb.com/jokes/random").body();
      JSONObject jsonObject = new JSONObject(jsonBody);
      String fact = jsonObject.getJSONObject("value").getString("joke");
      addStringResult("fact", fact);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
