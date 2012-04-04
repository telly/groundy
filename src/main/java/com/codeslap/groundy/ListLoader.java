package com.codeslap.groundy;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import java.util.List;

/**
 * AsyncTaskLoader which handles lists of genereic time
 * @param <T>
 */
public abstract class ListLoader<T> extends AsyncTaskLoader<List<T>> {
    public static final String TAG = ListLoader.class.getSimpleName();
    private List<T> mList;
    private final List<T> mCache;
    private final String mQuery;

    public ListLoader(Context context, String query, List<T> cache) {
        super(context);
        mQuery = query;
        mCache = cache;
    }

    /* Runs on a worker thread */
    @Override
    public List<T> loadInBackground() {
        if (getQuery() == null && getCache() != null && getCache().size() > 0) {
            return getCache();
        }
        return getData();
    }

    abstract List<T> getData();

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

    String getQuery() {
        return mQuery;
    }

    List<T> getCache() {
        return mCache;
    }

    boolean loadCacheIfPossible(List<T> items) {
        // there is no query and the initial collection already exist. load existent one.
        if (getQuery() == null && mCache != null && mCache.size() > 0) {
            L.d(TAG, "No query and the initial collection already exist. Loading existent one.");
            items.addAll(mCache);
            return true;
        }
        // load the items from the initial collection
        if (getQuery() != null && mCache != null && mCache.size() > 0) {
            L.d(TAG, "Querying (" + getQuery() + ") and the initial collection already exist. Searching inside...");
            for (T item : mCache) {
                if (matchQuery(item)) {
                    items.add(item);
                }
            }
            return true;
        }
        L.d(TAG, "Impossible to load data from cache");
        return false;
    }

    abstract boolean matchQuery(T item);
}