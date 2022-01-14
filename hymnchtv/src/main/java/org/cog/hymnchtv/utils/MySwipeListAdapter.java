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
package org.cog.hymnchtv.utils;

import android.content.Context;
import android.view.*;
import android.view.animation.AnimationUtils;
import android.widget.*;

import androidx.annotation.NonNull;

import org.cog.hymnchtv.R;

import java.util.*;

public class MySwipeListAdapter<T> extends ArrayAdapter<T>
{
    private final Context mContext;
    private final LayoutInflater mInflater;

    /**
     * Running state of which positions are currently being selected
     */
    Map<Integer, Boolean> mSelectStates = new HashMap<>();

    public MySwipeListAdapter(@NonNull Context context, @NonNull List<T> objects)
    {
        super(context, R.layout.row_list, objects);
        mContext = context;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent)
    {
        final ViewHolder viewHolder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.row_list, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.viewSwitcher = convertView.findViewById(R.id.list_switcher);
            viewHolder.viewSwitcher.setInAnimation(AnimationUtils.loadAnimation(mContext, R.anim.slide_in_right));
            viewHolder.viewSwitcher.setOutAnimation(AnimationUtils.loadAnimation(mContext, android.R.anim.slide_out_right));

            viewHolder.itemName = convertView.findViewById(R.id.tv_item_name);
            viewHolder.itemName2 = convertView.findViewById(R.id.tv_item_name2);

            viewHolder.deleteItem = convertView.findViewById(R.id.b_delete_in_list);
            viewHolder.deleteItem.setTag(position);

            viewHolder.openItem = convertView.findViewById(R.id.b_open_in_list);
            viewHolder.openItem.setTag(position);

            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final T item = getItem(position);
        if (item != null) {
            viewHolder.itemName.setText(item.toString());
            viewHolder.itemName2.setText(item.toString());

            viewHolder.deleteItem.setOnClickListener(v -> {
                mSelectStates.remove(position);
                remove(item);
            });
            viewHolder.openItem.setOnClickListener(v -> {
                mSelectStates.clear();
                open(item);
            });

            // Timber.d("MySwipeListAdapter %s: %s", position, mSelectStates.containsKey(position));
            // Need to re-init any view / mirror view to its actual selected state
            if ((viewHolder.viewSwitcher.getDisplayedChild() == 0) && mSelectStates.containsKey(position)) {
                viewHolder.viewSwitcher.setDisplayedChild(1);
            }
            else if ((viewHolder.viewSwitcher.getDisplayedChild() != 0) && !mSelectStates.containsKey(position)) {
                viewHolder.viewSwitcher.setDisplayedChild(0);
            }
        }
        return convertView;
    }

    /**
     *
     * @param item Object to be open
     */
    public void open(T item)
    {
    }

    /**
     * Set the current selection state of the ListView item; in order to return to its selected state on user scroll
     *
     * @param idx listView position
     * @param state set to true if AltDisplay is selected, else List item is removed
     */
    public void setSelectState(int idx, boolean state)
    {
        if (state) {
            mSelectStates.put(idx, true);
        }
        else {
            mSelectStates.remove(idx);
        }
    }

    private static class ViewHolder
    {
        ViewSwitcher viewSwitcher;
        TextView itemName;
        TextView itemName2;
        ImageButton deleteItem;
        ImageButton openItem;
    }
}
