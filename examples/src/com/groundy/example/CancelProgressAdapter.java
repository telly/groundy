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
import com.telly.groundy.adapter.Layout;
import com.telly.groundy.adapter.ListBaseAdapter;
import com.telly.groundy.adapter.ResourceId;
import com.telly.groundy.example.R;

public class CancelProgressAdapter extends ListBaseAdapter<ProgressItem, CancelProgressAdapter.ViewHolder> {

  public CancelProgressAdapter(Context context) {
    super(context, ViewHolder.class);
  }

  @Override
  public void populateHolder(int position, View view, ViewGroup parent, ProgressItem item,
                             ViewHolder holder) {
    holder.progressBar.setProgress(item.getProgress());
    switch (item.getState()) {
      case ProgressItem.CANCELLED:
        holder.estimated.setText(getString(R.string.task_didnt_run));
        break;
      case ProgressItem.INTERRUPTED:
        holder.estimated.setText(R.string.task_interrupted);
        break;
      case ProgressItem.DONE:
        holder.estimated.setText(R.string.task_completed);
        break;
      default:
        holder.estimated.setText(getString(R.string.will_work_for, item.getEstimated()));
    }
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).getId();
  }

  @Override
  public int getItemViewType(int position) {
    return getItem(position).getColor();
  }

  @Override
  protected int getHolderLayoutIdFor(int position) {
    switch (getItemViewType(position)) {
      case CancelTaskExample.BLUE_TASKS:
        return R.layout.progress_row;
      default:
      case CancelTaskExample.ORANGE_TASKS:
        return R.layout.progress_row_orange;
    }
  }

  @Layout(ids = {R.layout.progress_row, R.layout.progress_row_orange})
  public static class ViewHolder {
    @ResourceId(R.id.lbl_estimated) TextView estimated;
    @ResourceId(R.id.progress) ProgressBar progressBar;
  }
}
