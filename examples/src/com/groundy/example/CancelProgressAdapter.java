/**
 * Copyright Telly, Inc. and other Groundy contributors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
