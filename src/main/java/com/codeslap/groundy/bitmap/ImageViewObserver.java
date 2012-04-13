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
import android.widget.ImageView;

import java.util.Observable;
import java.util.Observer;

/**
 * Observer used to set the bitmap in given ImageView
 *
 * @author evelio
 * @version 1.0
 */
public class ImageViewObserver implements Observer {
    private volatile String url;
    private final ImageView view;
    private final Handler uiHandler;

    /**
     * Creates an observer by associating a given imgView with given URL
     *
     * @param imgView         View to assign bitmap to
     * @param url             URL to associate
     * @param uiThreadHandler Handler created in UI Thread
     */
    public ImageViewObserver(ImageView imgView, String url, Handler uiThreadHandler) {
        view = imgView;
        uiHandler = uiThreadHandler;
        setUrl(url);
    }

    /**
     * @param url url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof BitmapHelper.BitmapRef) {
            final BitmapHelper.BitmapRef ref = (BitmapHelper.BitmapRef) o;
            String refUri = ref.getUri();
            if (refUri.equals(url)) {
                final Bitmap bmp = ref.getBitmap();
                if (view != null && bmp != null) { //Check 1
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // in order to avoid repeating thumbnails or setting wrong ones, we check here
                            // the last tag (url) that was set to the image view. that way, this we make
                            // sure the bitmap that is shown is the correct one
                            if (view.getTag() != null && !view.getTag().equals(ref.from)) {
                                return;
                            }
                            view.setImageBitmap(bmp);
                            view.invalidate();
                        }
                    });
                }
            }
        }
    }
}
