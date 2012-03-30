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
