/*
 * Copyright 2013 CodeSlap
 *
 *   Authors: Cristian C. <cristian@elhacker.net>
 *            Evelio T.   <eveliotc@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.groundy.example.tasks;

import android.os.Bundle;
import com.codeslap.groundy.Groundy;
import com.codeslap.groundy.GroundyTask;

public class RandomTimeTask extends GroundyTask {

    public static final String KEY_ESTIMATED = "estimated";
    public static final String KEY_COUNT = "count";

    @Override
    protected boolean doInBackground() {
        int time = getIntParam(KEY_ESTIMATED);
        if (time < 1000) {
            time = 1000;
        }

        int interval = time / 100;

        int currentPercentage = 0;
        while (currentPercentage <= 100) {
            try {
                Bundle resultData = new Bundle();
                resultData.putInt(Groundy.KEY_PROGRESS, currentPercentage);
                resultData.putInt(KEY_COUNT, getIntParam(KEY_COUNT));
                send(Groundy.STATUS_PROGRESS, resultData);

                // let's fake some work ^_^
                Thread.sleep(interval);
                currentPercentage++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}