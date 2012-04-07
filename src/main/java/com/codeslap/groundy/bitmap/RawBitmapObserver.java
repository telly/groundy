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
