package com.codeslap.groundy.loader;

import android.content.AsyncTaskLoader;
import android.content.Context;

import java.util.List;

/**
 * @author cristian
 * @version 1.0
 */
public abstract class ListLoader<T> extends AsyncTaskLoader<List<T>> {
    private List<T> mList;

    public ListLoader(Context context) {
        super(context);
    }

    /* Runs on a worker thread */
    @Override
    public List<T> loadInBackground() {
        return getData();
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(List<T> list) {
        if (isReset()) {
            // An async query came in while the loader is stopped
            if (list != null) {
                list.clear();
            }
            return;
        }
        List<T> oldList = mList;
        mList = list;

        if (isStarted()) {
            super.deliverResult(list);
        }

        if (oldList != null && oldList != list && oldList.size() > 0) {
            oldList.clear();
        }
    }

    /**
     * Starts an asynchronous load of the contacts list data. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     * <p/>
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading() {
        if (mList != null) {
            deliverResult(mList);
        }
        if (takeContentChanged() || mList == null) {
            forceLoad();
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(List<T> list) {
        if (list != null && list.size() > 0) {
            list.clear();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        if (mList != null && mList.size() > 0) {
            mList.clear();
        }
        mList = null;
    }

    protected List<T> getList() {
        return mList;
    }

    /**
     * @return a List with the data to load
     */
    protected abstract List<T> getData();
}