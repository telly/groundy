/*
 * Copyright 2012 CodeSlap
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

package com.codeslap.groundy;

import android.content.Context;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple call resolver factory
 *
 * @author evelio
 * @version 1.0
 */
class CallResolverFactory {
    private static final String TAG = "CallResolverFactory";

    private static final Map<Class<? extends CallResolver>, CallResolver> sCache = new HashMap<Class<? extends CallResolver>, CallResolver>();

    /**
     * Non instances
     */
    private CallResolverFactory() {
    }

    /**
     * Builds a CallResolver based on call
     *
     * @param resolverClass tag of the resolver
     * @param context       used to instantiate the resolver
     * @return An instance of a Resolver if a given call is valid null otherwise
     */
    static CallResolver get(Class<? extends CallResolver> resolverClass, Context context) {
        if (sCache.containsKey(resolverClass)) {
            return sCache.get(resolverClass);
        }
        CallResolver resolver = null;
        try {
            L.d(TAG, "Instantiating "+resolverClass);
            Constructor ctc = resolverClass.getConstructor();
            resolver = (CallResolver) ctc.newInstance();
            if (resolver.canBeCached()) {
                sCache.put(resolverClass, resolver);
            } else if(sCache.containsKey(resolverClass)) {
                sCache.remove(resolverClass);
            }
            resolver.setContext(context);
            return resolver;
        } catch (Exception e) {
            L.e(TAG, "Unable to create resolver for call " + resolverClass, e);
        }
        return resolver;
    }

}
