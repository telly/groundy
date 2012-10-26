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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.FrameLayout;

/**
 * A {@link BaseActivity} that simply contains a single fragment. The intent used to invoke this
 * ui is forwarded to the fragment as arguments during fragment instantiation. Derived
 * activities should only need to implement
 * {@link BaseSinglePaneActivity#onCreatePane()}.
 */
public abstract class BaseSinglePaneActivity extends BaseActivity {

    public static final int ID = 4876438;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View rootView = getRootView();
        if ((getRootResource() != Integer.MIN_VALUE || rootView != null) && getRootResourceId() == ID) {
            throw new IllegalStateException("When overriding root view, you must also override getRootResourceId()" +
                    " passing the ID of the view group where the content returned view onCreatePane will" +
                    " be placed.");
        }
        if (getRootResource() != Integer.MIN_VALUE) {
            setContentView(getRootResource());
        } else if (rootView != null) {
            setContentView(rootView);
        } else {
            FrameLayout frameLayout = new FrameLayout(this);
            frameLayout.setId(getRootResourceId());
            setContentView(frameLayout);
        }
        setSupportProgressBarIndeterminateVisibility(false);

        if (savedInstanceState == null) {
            Fragment fragment = onCreatePane();
            fragment.setArguments(intentToFragmentArguments(getIntent()));

            String tag = fragment.getClass().getName();
            getSupportFragmentManager().beginTransaction()
                    .add(getRootResourceId(), fragment, tag)
                    .commit();
        }
    }

    protected int getRootResource() {
        return Integer.MIN_VALUE;
    }

    protected int getRootResourceId() {
        return ID;
    }

    protected View getRootView() {
        return null;
    }

    /**
     * Called in <code>onCreate</code> when the fragment constituting this ui is needed.
     * The returned fragment's arguments will be set to the intent used to invoke this ui.
     */
    protected abstract Fragment onCreatePane();
}
