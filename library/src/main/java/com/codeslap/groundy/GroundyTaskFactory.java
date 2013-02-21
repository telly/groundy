/*
 * Copyright 2012 Twitvid Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeslap.groundy;

import android.content.Context;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link GroundyTask} factory
 *
 * @author Evelio Tarazona <evelio@twitvid.com>
 * @version 1.0
 */
class GroundyTaskFactory {
    private static final String TAG = "GroundyTaskFactory";

    private static final Map<Class<? extends GroundyTask>, GroundyTask> sCache = new HashMap<Class<? extends GroundyTask>, GroundyTask>();

    /**
     * Non instances
     */
    private GroundyTaskFactory() {
    }

    /**
     * Builds a GroundyTask based on call
     *
     * @param taskClass groundy task implementation class
     * @param context   used to instantiate the task
     * @return An instance of a GroundyTask if a given call is valid null otherwise
     */
    static GroundyTask get(Class<? extends GroundyTask> taskClass, Context context) {
        if (sCache.containsKey(taskClass)) {
            return sCache.get(taskClass);
        }
        GroundyTask groundyTask = null;
        try {
            L.d(TAG, "Instantiating " + taskClass);
            Constructor ctc = taskClass.getConstructor();
            groundyTask = (GroundyTask) ctc.newInstance();
            if (groundyTask.canBeCached()) {
                sCache.put(taskClass, groundyTask);
            } else if (sCache.containsKey(taskClass)) {
                sCache.remove(taskClass);
            }
            groundyTask.setContext(context);
            return groundyTask;
        } catch (Exception e) {
            L.e(TAG, "Unable to create task for call " + taskClass, e);
        }
        return groundyTask;
    }

}
