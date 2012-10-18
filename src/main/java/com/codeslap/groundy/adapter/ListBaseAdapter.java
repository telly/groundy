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

package com.codeslap.groundy.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic class used to easily create a list adapter.
 *
 * @author cristian
 */
public abstract class ListBaseAdapter<I, H> extends BaseAdapter {

    private final List<I> mItems = new ArrayList<I>();
    private final LayoutInflater mInflater;
    private final Context mContext;
    private final Class<H> mViewHolderClass;
    private final int mLayoutId;
    private final Map<Field, Integer> mFieldCache = new HashMap<Field, Integer>();

    public ListBaseAdapter(Context context, Class<H> viewHolder) {
        mContext = context;
        mViewHolderClass = viewHolder;
        mInflater = LayoutInflater.from(context);
        if (!viewHolder.isAnnotationPresent(Layout.class)) {
            throw new IllegalStateException("viewHolder class must have a " + Layout.class + " annotation");
        }
        Layout layout = viewHolder.getAnnotation(Layout.class);
        mLayoutId = layout.value();

        // cache all fields to know what field corresponds to what resource id
        for (Field field : mViewHolderClass.getDeclaredFields()) {
            field.setAccessible(true);
            int resourceId;
            if (!field.isAnnotationPresent(ResourceId.class)) {
                try {
                    resourceId = getContext().getResources().getIdentifier(field.getName(), "id", getContext().getPackageName());
                } catch (Exception e) {
                    throw new RuntimeException("Could not find id for field: " + field
                            + ". Either add a @ResourceId annotation or make the field have the same name than the id." +
                            "Also, you can use the @ResourceId:ignore field.", e);
                }
            } else {
                ResourceId resourceIdAnnotation = field.getAnnotation(ResourceId.class);
                if (resourceIdAnnotation.ignore()) {
                    continue;
                }
                resourceId = resourceIdAnnotation.value();
            }

            try {
                mFieldCache.put(field, resourceId);
            } catch (Exception e) {
                throw new RuntimeException("Could not set view (" + resourceId + ") to " + field, e);
            }
        }

    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public I getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public abstract long getItemId(int position);

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        H holder;
        if (view == null) {
            // create an instance of the view holder class
            try {
                holder = mViewHolderClass.newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("Could not instantiate view holder: " + mViewHolderClass + ". Make sure it has an empty constructor.", e);
            }

            // inflate the base view
            view = mInflater.inflate(mLayoutId, null);

            for (Field field : mFieldCache.keySet()) {
                int resourceId = mFieldCache.get(field);
                View viewById = view.findViewById(resourceId);
                try {
                    field.set(holder, viewById);
                } catch (Exception e) {
                    throw new RuntimeException("Could not set view (" + resourceId + ") to field " + field + ". Holder: " + holder + ", found view: " + viewById, e);
                }
            }
            view.setTag(holder);
        } else {
            holder = (H) view.getTag();
        }
        populateHolder(position, view, parent, getItem(position), holder);
        return view;
    }

    public abstract void populateHolder(int position, View view, ViewGroup parent, I item, H holder);

    public Context getContext() {
        return mContext;
    }

    public List<I> getItems() {
        return mItems;
    }

    public void updateItems(List<I> data) {
        mItems.clear();
        mItems.addAll(data);
        notifyDataSetChanged();
    }

    public void clear() {
        mItems.clear();
    }

    public void addItems(List<I> is) {
        mItems.addAll(is);
        notifyDataSetChanged();
    }
}
