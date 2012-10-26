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
import android.view.View;
import android.view.ViewGroup;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockFragmentActivity;

/**
 * @author cristian
 * @version 1.0
 */
public class GuiceBaseActivity extends RoboSherlockFragmentActivity {
    private final BaseActivityDelegate mDelegate = new BaseActivityDelegate(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mDelegate.onCreate();
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int layoutResId) {
        super.setContentView(layoutResId);
        mDelegate.setContentView();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        mDelegate.setContentView();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        mDelegate.setContentView();
    }

    /**
     * Helper method that allows to initialize and add a fragment to activities that usually have
     * just one single fragment. Fragment is added using its class.getName() as tag.
     *
     * @param containerId   resource id of the fragment container (must be created through android resources)
     * @param fragmentClass the class of the fragment to setup
     */
    protected void setupBaseFragment(int containerId, Class<? extends Fragment> fragmentClass) {
        mDelegate.setupBaseFragment(containerId, fragmentClass);
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
        mDelegate.setupBaseFragment(containerId, fragmentClass, args);
    }

    /**
     * Allows to retrieve the instance of a previously added fragment. We
     * use fragmentClass.getName() to find by tag.
     *
     * @param fragmentClass the fragment class
     * @return the fragment or null if it was has not been added
     */
    public <T> T findFragment(Class<? extends T> fragmentClass) {
        return mDelegate.findFragment(fragmentClass);
    }

    /**
     * Takes a given intent and either starts a new ui to handle it (the default behavior),
     * or creates/updates a fragment (in the case of a multi-pane ui) that can handle the
     * intent.
     * <p/>
     * Must be called from the main (UI) thread.
     */
    public void openActivityOrFragment(Intent intent) {
        mDelegate.openActivityOrFragment(intent);
    }
}
