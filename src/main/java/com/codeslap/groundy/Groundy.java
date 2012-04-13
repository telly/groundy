package com.codeslap.groundy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

public class Groundy {
    public static final String KEY_PARAMETERS = "com.codeslap.groundy.key.paramaters";
    public static final String KEY_RESULT = "com.codeslap.groundy.key.result";
    public static final String KEY_ERROR = "com.codeslap.groundy.key.error";
    public static final String KEY_RECEIVER = "com.codeslap.groundy.key.receiver";
    public static final int STATUS_FINISHED = 200;
    public static final int STATUS_ERROR = 232;
    public static final int STATUS_RUNNING = 224;
    public static final int STATUS_CONNECTIVITY_FAILED = 8433;

    /**
     * Queue a call resolver to be executed
     *
     * @param context  needed to start the service
     * @param resolver call resolver implementation
     * @param receiver result receiver to report results back
     * @param extras   the application code
     */
    public static void queue(Context context, Class<? extends CallResolver> resolver, ResultReceiver receiver,
                             Bundle extras) {
        startApiService(context, receiver, resolver, extras, false);
    }

    /**
     * Execute this call resolver asynchronously
     *
     * @param context  needed to start the service
     * @param resolver call resolver implementation
     * @param receiver result receiver to report results back
     * @param extras   the application code
     */
    public static void execute(Context context, Class<? extends CallResolver> resolver, ResultReceiver receiver,
                               Bundle extras) {
        startApiService(context, receiver, resolver, extras, true);
    }

    private static void startApiService(Context context, ResultReceiver receiver,
                                        Class<? extends CallResolver> resolver, Bundle params, boolean async) {
        Intent intent = new Intent(context, GroundyService.class);
        if (async) {
            intent.putExtra(GroundyIntentService.EXTRA_ASYNC, async);
        }
        intent.setAction(resolver.getName());
        if (params != null) {
            intent.putExtra(KEY_PARAMETERS, params);
        }
        intent.putExtra(KEY_RECEIVER, receiver);
        context.startService(intent);
    }
}
