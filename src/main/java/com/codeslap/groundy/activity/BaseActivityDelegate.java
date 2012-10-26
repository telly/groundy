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

package com.codeslap.groundy.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;

/**
 * @author cristian
 * @version 1.0
 */
class BaseActivityDelegate {

    private boolean mSetContentViewAlreadyCalled;
    private final SherlockFragmentActivity mActivity;

    BaseActivityDelegate(SherlockFragmentActivity activity) {
        mActivity = activity;
    }

    void onCreate() {
        mActivity.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    }

    void setContentView() {
        mActivity.setSupportProgressBarIndeterminateVisibility(false);
        mSetContentViewAlreadyCalled = true;
    }

    /**
     * Helper method that allows to initialize and add a fragment to activities that usually have
     * just one single fragment. Fragment is added using its class.getName() as tag.
     *
     * @param containerId   resource id of the fragment container (must be created through android resources)
     * @param fragmentClass the class of the fragment to setup
     */
    void setupBaseFragment(int containerId, Class<? extends Fragment> fragmentClass) {
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
    void setupBaseFragment(int containerId, Class<? extends Fragment> fragmentClass, Bundle args) {
        if (mSetContentViewAlreadyCalled) {
            View view = mActivity.findViewById(containerId);
            if (!(view instanceof ViewGroup)) {
                throw new IllegalStateException("Since you already called setContentView, it must has a ViewGroup whose id is 'containerId'");
            }
        } else {
            FrameLayout container = new FrameLayout(mActivity);
            container.setId(containerId);
            mActivity.setContentView(container);
        }

        // let's check whether fragment is already added
        Fragment fragment = findFragment(fragmentClass);
        if (fragment == null) {
            // if not, let's create it and add it
            fragment = Fragment.instantiate(mActivity, fragmentClass.getName(), args);

            FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
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
    <T> T findFragment(Class<? extends T> fragmentClass) {
        FragmentManager fm = mActivity.getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(fragmentClass.getSimpleName());
        if (!fragmentClass.isInstance(fragment)) {
            return null;
        }
        return (T) fragment;
    }

    /**
     * Takes a given intent and either starts a new ui to handle it (the default behavior),
     * or creates/updates a fragment (in the case of a multi-pane ui) that can handle the
     * intent.
     * <p/>
     * Must be called from the main (UI) thread.
     */
    void openActivityOrFragment(Intent intent) {
        // Default implementation simply calls startActivity
        mActivity.startActivity(intent);
    }
}
