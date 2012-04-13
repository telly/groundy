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

package com.codeslap.groundy.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Helper to deal with Bitmaps, including downloading and caching.
 *
 * @author evelio
 * @author cristian
 * @version 1.1
 */
public class BitmapHelper {
    /**
     * Unique instance of this helper
     */
    private static BitmapHelper instance;
    /**
     * On memory pool to add already loaded from file bitmaps
     * Note: will be purged on by itself in case of low memory
     */
    private final Map<String, BitmapRef> cache;
    /**
     * the hard worker
     */
    private final BitmapLoader loader;

    /**
     * Unique constructor
     * must be quick as hell
     */
    private BitmapHelper() {
        cache = new WeakHashMap<String, BitmapRef>();
        loader = new BitmapLoader();
    }

    /**
     * Singleton method
     *
     * @return {@link #instance}
     */
    public static BitmapHelper getInstance() {
        if (instance == null) {
            instance = new BitmapHelper();
        }
        return instance;
    }

    /**
     * Clears current cache if any
     */
    public void clearCache() {
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Try to get the bitmap from cache
     *
     * @param urlFrom A valid URL pointing to a bitmap
     * @return A bitmap associated to given url if any available or will try to download it
     *         <p/>
     *         Note: in case of urlFrom parameter is null this method does nothing
     */
    public Bitmap getBitmap(String urlFrom) {
        if (isInvalidUri(urlFrom)) {
            return null;
        }
        //Lets check the cache
        BitmapRef ref = cache.get(urlFrom);
        if (ref != null) {
            return ref.getBitmap();
        }
        return null;
    }


    /**
     * Download and put in cache a bitmap
     *
     * @param context  Context to use
     * @param urlFrom  A valid URL pointing to a bitmap
     * @param observer Will be notified on bitmap loaded
     */
    public void registerBitmapObserver(Context context, String urlFrom, Observer observer) {
        if (isInvalidUri(urlFrom)) {
            return;
        }
        //Lets check the cache
        BitmapRef ref = cache.get(urlFrom);
        Bitmap bitmap = null;
        if (ref == null) {
            //Hummm nothing in cache lets try to put it in cache
            ref = new BitmapRef(urlFrom);
            cache.put(urlFrom, ref);
        } else {
            bitmap = ref.getBitmap();
        }

        if (bitmap == null) { //humm garbage collected or not already loaded lest try to load it anyway
            ref.addObserver(observer);
            loader.load(context, ref);
        } else {
            observer.update(ref, null);
        }
    }

    private static boolean isInvalidUri(String url) {
        return url == null || url.length() == 0;
    }

    public static final BitmapFactory.Options normalOptions = new Options();

    static {
        normalOptions.inDither = true;
        normalOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        normalOptions.inPurgeable = true;
        normalOptions.inScaled = true;
    }

    private static Bitmap loadBitmapFile(String path) {
        return BitmapFactory.decodeFile(path, BitmapHelper.normalOptions);
    }

    /**
     * Wrapper to an association bewteen an URL and a in memory cached bitmap
     *
     * @author evelio
     */
    public static class BitmapRef extends Observable {
        // ESCA-JAVA0098: default is null
        Bitmap bitmapRef;
        String from;

        /**
         * Creates a new instance with given uri
         *
         * @param uri a bitmap url
         */
        public BitmapRef(String uri) {
            if (isInvalidUri(uri)) {
                throw new IllegalArgumentException("Invalid URL");
            }
            from = uri;
        }

        /**
         * @return Bitmap cached or null if was garbage collected
         */
        public Bitmap getBitmap() {
            return bitmapRef;
        }

        /**
         * @return URL associated to this BitmapRef
         */
        public String getUri() {
            return from;
        }

        @Override
        public int hashCode() {
            return from.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof BitmapRef) {
                BitmapRef otherRef = (BitmapRef) obj;
                return from.equals(otherRef.getUri());
            }
            return false;
        }

        /**
         * @param bmp Bitmap to associate
         */
        public void loaded(Bitmap bmp) {
            bitmapRef = bmp;
            setChanged();
            notifyObservers();
        }

        @Override
        public String toString() {
            return super.toString() + "{ "
                    + "bitmap: " + getBitmap()
                    + "from: " + from
                    + " }";
        }
    }

    /**
     * Calls that makes the dirty work
     *
     * @author evelio
     */
    private static class BitmapLoader {

        private final ExecutorService executor;
        /**
         * reference to those already enqueued
         */
        private final Set<BitmapRef> enqueued;

        /**
         * Default constructor
         */
        private BitmapLoader() {
            executor = Executors.newCachedThreadPool();
            enqueued = Collections.synchronizedSet(new HashSet<BitmapRef>());
        }

        /**
         * Loads a Bitmap into the given ref
         *
         * @param context context needed to get app cache directory
         * @param ref     Reference to use
         */
        private void load(Context context, BitmapRef ref) {
            if (ref == null || ref.getBitmap() != null) {
                return;
            }

            if (enqueued.add(ref)) {
                executor.execute(new LoadTask(ref, IOUtils.getCacheDirectory(context)));
            }
        }


        private class LoadTask implements Runnable {
            private static final String TAG = "Codeslap.BitmapHelper.LoadTask";
            private final BitmapRef reference;
            private final File cacheDir;

            private LoadTask(BitmapRef ref, File cacheDir) {
                reference = ref;
                this.cacheDir = cacheDir;
            }

            @Override
            public void run() {
                try {
                    //load it
                    Bitmap bmp = doLoad(reference.getUri(), cacheDir);
                    reference.loaded(bmp);
                } catch (Exception e) {
                    if (e != null) {
                        Log.e(TAG, "Unable to load bitmap", e);
                    }
                }
                enqueued.remove(reference);
            }

        }
    }
    
    public static Bitmap doLoad(Context context, String url) throws IOException {
        return doLoad(url, IOUtils.getCacheDirectory(context));
    }
    
    private static Bitmap doLoad(String url, File cacheDir) throws IOException {
        Bitmap image = null;

        String filename = String.valueOf(url.hashCode());
        File file = new File(cacheDir, filename);

        if (file.exists()) {//Something is stored
            image = loadBitmapFile(file.getCanonicalPath());
        }

        if (image == null) {//So far nothing is cached, lets download it
            IOUtils.downloadFile(url, file);
            if (file.exists()) {
                image = loadBitmapFile(file.getCanonicalPath());
            }
        }
        return image;
    }

}
