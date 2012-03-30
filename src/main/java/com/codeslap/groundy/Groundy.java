package com.codeslap.groundy;

public class Groundy {
    public static void register(String tag, Class<? extends CallResolver> callResolverClass) {
        CallResolverFactory.resolvers.put(tag, callResolverClass);
    }
}
