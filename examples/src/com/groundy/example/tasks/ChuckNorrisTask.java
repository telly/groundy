package com.groundy.example.tasks;

import com.codeslap.groundy.GroundyTask;
import com.github.kevinsawicki.http.HttpRequest;
import org.json.JSONObject;

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
