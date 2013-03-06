/*
 * Copyright 2013 Telly Inc.
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

package com.groundy.example;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.telly.groundy.example.R;
import com.telly.groundy.adapter.Layout;
import com.telly.groundy.adapter.ListBaseAdapter;
import com.telly.groundy.adapter.ResourceId;

/**
 * @author Cristian Castiblanco <cristian@elhacker.net>
 */
public class ProgressAdapter extends ListBaseAdapter<ProgressItem, ProgressAdapter.ViewHolder> {

  public ProgressAdapter(Context context) {
    super(context, ViewHolder.class);
  }

  @Override
  public void populateHolder(int position, View view, ViewGroup parent, ProgressItem item, ViewHolder holder) {
    holder.label.setText(String.valueOf(item.getCount()));
    holder.progressBar.setProgress(item.getProgress());
    holder.estimated.setText(getContext().getString(R.string.will_work_for, item.getEstimated()));
  }

  @Layout(R.layout.progress_row)
  public static class ViewHolder {
    @ResourceId(R.id.lbl_id)
    TextView label;
    @ResourceId(R.id.lbl_estimated)
    TextView estimated;
    @ResourceId(R.id.progress)
    ProgressBar progressBar;
  }
}
