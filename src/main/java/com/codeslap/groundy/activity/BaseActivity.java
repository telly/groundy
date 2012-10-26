/*
* Copyright 2011 Google Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.codeslap.groundy.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;

/**
 * A base activity that defers common functionality across app activities to an
 * {@link ActivityHelper}.
 */
public abstract class BaseActivity extends SherlockFragmentActivity {
    public static final String URI_KEY = "_uri";
    final ActivityHelper mActivityHelper = ActivityHelper.createInstance(this);
    private boolean mSetContentViewAlreadyCalled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int layoutResId) {
        super.setContentView(layoutResId);
        setSupportProgressBarIndeterminateVisibility(false);
        mSetContentViewAlreadyCalled = true;
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        setSupportProgressBarIndeterminateVisibility(false);
        mSetContentViewAlreadyCalled = true;
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        mSetContentViewAlreadyCalled = true;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return mActivityHelper.onKeyLongPress(keyCode, event)
                || super.onKeyLongPress(keyCode, event);
    }

    /**
     * Helper method that allows to initialize and add a fragment to activities that usually have
     * just one single fragment. Fragment is added using its class.getName() as tag.
     *
     * @param containerId   resource id of the fragment container (must be created through android resources)
     * @param fragmentClass the class of the fragment to setup
     */
    protected void setupBaseFragment(int containerId, Class<? extends Fragment> fragmentClass) {
        setupBaseFragment(containerId, fragmentClass, null);
    }

    /**
     * Helper method that allows to initialize and add a fragment to activities that usually have
     * just one single fragment. Fragment is added using its class.getName() as tag.
     *
     * @param containerId   resource id of the fragment container (must be created through android resources)
     * @param fragmentClass the class of the fragment to setup
     * @param args          bundle with the arguments to pass to the fragment
     */
    protected void setupBaseFragment(int containerId, Class<? extends Fragment> fragmentClass, Bundle args) {
        if (mSetContentViewAlreadyCalled) {
            View view = findViewById(containerId);
            if (!(view instanceof ViewGroup)) {
                throw new IllegalStateException("Since you already called setContentView, it must has a ViewGroup whose id is 'containerId'");
            }
        } else {
            FrameLayout container = new FrameLayout(this);
            container.setId(containerId);
            setContentView(container);
        }

        // let's check whether fragment is already added
        Fragment fragment = findFragment(fragmentClass);
        if (fragment == null) {
            // if not, let's create it and add it
            fragment = Fragment.instantiate(this, fragmentClass.getName(), args);

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(containerId, fragment, fragmentClass.getSimpleName());
            ft.commit();
        }
    }

    /**
     * Allows to retrieve the instance of a previously added fragment. We
     * use fragmentClass.getName() to find by tag.
     *
     * @param fragmentClass the fragment class
     * @return the fragment or null if it was has not been added
     */
    public <T> T findFragment(Class<? extends T> fragmentClass) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(fragmentClass.getSimpleName());
        if (!fragmentClass.isInstance(fragment)) {
            return null;
        }
        return (T) fragment;
    }

    /**
     * Returns the {@link ActivityHelper} object associated with this activity.
     */
    protected ActivityHelper getActivityHelper() {
        return mActivityHelper;
    }

    /**
     * Converts an intent into a {@link android.os.Bundle} suitable for use as fragment
     * arguments.
     */
    public static Bundle intentToFragmentArguments(Intent intent) {
        Bundle arguments = new Bundle();
        if (intent == null) {
            return arguments;
        }

        final Uri data = intent.getData();
        if (data != null) {
            arguments.putParcelable(URI_KEY, data);
        }

        final Bundle extras = intent.getExtras();
        if (extras != null) {
            arguments.putAll(intent.getExtras());
        }

        return arguments;
    }

    /**
     * Converts a fragment arguments bundle into an intent.
     */
    public static Intent fragmentArgumentsToIntent(Bundle arguments) {
        Intent intent = new Intent();
        if (arguments == null) {
            return intent;
        }

        final Uri data = arguments.getParcelable(URI_KEY);
        if (data != null) {
            intent.setData(data);
        }

        intent.putExtras(arguments);
        intent.removeExtra(URI_KEY);
        return intent;
    }

    /**
     * Takes a given intent and either starts a new ui to handle it (the default behavior),
     * or creates/updates a fragment (in the case of a multi-pane ui) that can handle the
     * intent.
     * <p/>
     * Must be called from the main (UI) thread.
     */
    public void openActivityOrFragment(Intent intent) {
        // Default implementation simply calls startActivity
        startActivity(intent);
    }
}