package com.codeslap.groundy;

import android.content.Context;

import java.lang.reflect.Constructor;
import java.util.HashMap;

/**
 * Simple call resolver factory
 *
 * @author evelio
 * @version 1.0
 */
public final class CallResolverFactory {
    static final HashMap<String, Class<? extends CallResolver>> resolvers = new HashMap<String, Class<? extends CallResolver>>();
    private static final String TAG = "CallResolverFactory";

    /**
     * Non instances
     */
    private CallResolverFactory() {
    }

    /**
     * Builds a CallResolver based on call
     *
     * @param call    tag of the resolver
     * @param context used to instantiate the resolver
     * @return An instance of a Resolver if a given call is valid null otherwise
     */
    static CallResolver get(String call, Context context) {
        CallResolver resolver = null;
        if (resolvers.containsKey(call)) {
            Class<?> resolverClass = resolvers.get(call);
            try {
                @SuppressWarnings("rawtypes")
                Constructor ctc = resolverClass.getConstructor(Context.class);
                resolver = (CallResolver) ctc.newInstance(context);
                return resolver;
            } catch (Exception e) {
                L.e(TAG, "Unable to create resolver for call " + call, e);
            }
        }
        return resolver;
    }

}
