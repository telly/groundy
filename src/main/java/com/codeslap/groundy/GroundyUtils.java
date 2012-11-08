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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author evelio
 * @author cristian
 * @version 1.1
 */
public class GroundyUtils {
    private static boolean alreadyCheckedInternetPermission = false;

    /**
     * Non instance constants class
     */
    private GroundyUtils() {
    }

    /**
     * Read it's name
     */
    private static final int DEFAULT_BUFFER_SIZE = 4096; // 4 KiB

    /**
     * Finds out the cache directory
     *
     * @param context Context to use
     * @return A File where means a directory where cache files should be written
     */
    public static File getCacheDirectory(Context context) {
        File cacheDir = context.getCacheDir();
        if (!cacheDir.exists() && cacheDir.mkdirs()) {
            Log.d(GroundyUtils.class.getSimpleName(), "Cache directory created");
        }
        return cacheDir;
    }

    /**
     * Download a file at <code>fromUrl</code> to a file specified by <code>toFile</code>
     *
     * @param fromUrl An url pointing to a file to download
     * @param toFile  File to save to, if existent will be overwrite
     * @throws java.io.IOException If fromUrl is invalid or there is any IO issue.
     */
    public static void downloadFile(Context context, String fromUrl, File toFile, ProgressListener listener) throws IOException {
        downloadFileHandleRedirect(context, fromUrl, toFile, 0, listener);
    }

    public interface ProgressListener {
        void onProgress(String url, int progress);
    }

    /**
     * Returns a progress listener that will post progress to the specified resolver
     *
     * @param callResolver the resolver to post progress to. Cannot be null.
     * @return a progress listener
     */
    public static ProgressListener fromResolver(final CallResolver callResolver) {
        return new ProgressListener() {
            @Override
            public void onProgress(String url, int progress) {
                callResolver.updateProgress(progress);
            }
        };
    }

    /**
     * Amount of maximum allowed redirects
     * number by:
     * http://www.google.com/support/forum/p/Webmasters/thread?tid=3760b68fb305088a&hl=en
     */
    private static final int MAX_REDIRECTS = 5;

    /**
     * Internal version of {@link #downloadFile(Context, String, java.io.File, ProgressListener}
     *
     * @param fromUrl  the url to download from
     * @param toFile   the file to download to
     * @param redirect true if it should accept redirects
     * @param listener used to report result back
     * @throws java.io.IOException
     */
    private static void downloadFileHandleRedirect(Context context, String fromUrl, File toFile, int redirect, ProgressListener listener) throws IOException {
        if (context == null) {
            throw new RuntimeException("Context shall not be null");
        }
        if (!alreadyCheckedInternetPermission) {
            try {
                PackageManager pm = context.getPackageManager();
                PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
                String[] requestedPermissions = packageInfo.requestedPermissions;
                if (requestedPermissions == null) {
                    throw new RuntimeException("You must add android.permission.INTERNET to your app");
                }
                boolean found = false;
                for (String requestedPermission : requestedPermissions) {
                    if ("android.permission.INTERNET".equals(requestedPermission)) {
                        found = true;
                    }
                }
                if (!found) {
                    throw new RuntimeException("You must add android.permission.INTERNET to your app");
                } else {
                    alreadyCheckedInternetPermission = true;
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }

        if (redirect > MAX_REDIRECTS) {
            throw new IOException("Too many redirects for " + fromUrl);
        }

        URL url = new URL(fromUrl);
        URLConnection urlConnection = url.openConnection();
        urlConnection.connect();
        int contentLength = urlConnection.getContentLength();
        if (contentLength == -1) {
            fromUrl = urlConnection.getHeaderField("Location");
            if (fromUrl == null) { /* I'd love to leave it as "Que Dios se apiade de nosotros" XD */
                throw new IOException("No content or redirect found for URL " + url + " with " + redirect + " redirects.");
            }
            downloadFileHandleRedirect(context, fromUrl, toFile, redirect + 1, listener);
            return;
        }
        InputStream input = urlConnection.getInputStream();
        OutputStream output = new FileOutputStream(toFile);
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long total = 0;
        int count;
        int fileLength = urlConnection.getContentLength();
        while ((count = input.read(buffer)) > 0) {// > 0 due zero sized streams
            total += count;
            output.write(buffer, 0, count);
            if (listener != null && fileLength > 0) {
                listener.onProgress(fromUrl, (int) (total * 100 / fileLength));
            }
        }
        output.close();
        input.close();
    }
}
