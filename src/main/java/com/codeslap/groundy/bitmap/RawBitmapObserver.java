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

import android.graphics.Bitmap;
import android.os.Handler;

import java.util.Observable;
import java.util.Observer;

public abstract class RawBitmapObserver implements Observer {
    private final String url;
    private final Handler uiHandler;

    /**
     * Creates an observer by associating a given imgView with given URL
     *
     * @param url             URL to associate
     * @param uiThreadHandler Handler created in UI Thread
     */
    public RawBitmapObserver(String url, Handler uiThreadHandler) {
        uiHandler = uiThreadHandler;
        this.url = url;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof BitmapHelper.BitmapRef) {
            final BitmapHelper.BitmapRef ref = (BitmapHelper.BitmapRef) o;
            String refUri = ref.getUri();
            if (refUri.equals(url)) {
                final Bitmap bmp = ref.getBitmap();
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onBitmapDownloaded(bmp);
                    }
                });
            }
        }
    }

    protected abstract void onBitmapDownloaded(Bitmap bmp);
}
