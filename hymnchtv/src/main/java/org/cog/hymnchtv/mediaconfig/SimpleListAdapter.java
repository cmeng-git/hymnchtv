/*
 * hymnchtv: COG hymns' lyrics viewer and player client
 * Copyright 2020 Eng Chong Meng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cog.hymnchtv.mediaconfig;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;

import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.R;

import java.util.List;
import java.util.Map;

/**
 * SimpleListAdapter provides the highlight of the user selected item
 */
public class SimpleListAdapter extends SimpleAdapter
{
    private int mItemIndex = -1;
    private View lastSelectedView = null;

    public SimpleListAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to)
    {
        super(context, data, resource, from, to);
    }

    /* (non-Javadoc)
     * @see android.widget.SimpleAdapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view = super.getView(position, convertView, parent);
        view.setBackgroundColor(HymnsApp.getAppResources().getColor(R.color.background_dark));
        if (position == mItemIndex) {
            view.setBackgroundColor(Color.TRANSPARENT);
            lastSelectedView = view;
        }
        return view;
    }

    /**
     * Reverted the last selected view to its background color
     * Highlight the newly selected view;
     *
     * @param view user selected new view
     * @param index The selected view index, need this in getView (Recycle View)
     */
    public void setSelectItem(int index, View view)
    {
        if (lastSelectedView != null) {
            lastSelectedView.setBackgroundColor(HymnsApp.getAppResources().getColor(R.color.background_dark));
        }
        mItemIndex = index;
        lastSelectedView = view;
        view.setBackgroundColor(Color.TRANSPARENT);
    }
}