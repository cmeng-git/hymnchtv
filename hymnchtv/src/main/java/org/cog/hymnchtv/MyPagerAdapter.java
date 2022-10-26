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
package org.cog.hymnchtv;

import android.os.Bundle;

import androidx.collection.LongSparseArray;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import org.jetbrains.annotations.NotNull;

import static org.cog.hymnchtv.ContentView.LYRICS_INDEX;
import static org.cog.hymnchtv.ContentView.LYRICS_TYPE;
import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_BB_INDEX_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_INDEX_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_ER_INDEX_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_XB_INDEX_MAX;

/**
 * The hymn lyrics implementation for the user page sliding and display update using
 * the latest FragmentStateAdapter to minimize OOM Exception:
 * See https://stackoverflow.com/questions/18747975/what-is-the-difference-between-fragmentpageradapter-and-fragmentstatepageradapte
 * See Reported problem on FragmentStateAdapter: https://issuetracker.google.com/issues/177051960
 *
 * @author Eng Chong Meng
 */
public class MyPagerAdapter extends FragmentStateAdapter
{
    private final String mHymnType;

    // Map array of index to ContentView for correct Content reference during access
    public final LongSparseArray<Fragment> mFragments = new LongSparseArray<>();

    public MyPagerAdapter(FragmentActivity fragmentActivity, String hymnType)
    {
        super(fragmentActivity);
        mHymnType = hymnType;
    }

    @Override
    public int getItemCount()
    {
        switch (mHymnType) {
            case HYMN_ER:
                return HYMN_ER_INDEX_MAX;

            case HYMN_XB:
                return HYMN_XB_INDEX_MAX;

            case HYMN_BB:
                return HYMN_BB_INDEX_MAX;

            case HYMN_DB:
                return HYMN_DB_INDEX_MAX;

            default:
                return 1;
        }
    }

    @Override
    public @NotNull Fragment createFragment(int index)
    {
        // Timber.d("Get item fragment index @: %s", index);
        Bundle bundle = new Bundle();
        bundle.putString(LYRICS_TYPE, mHymnType);
        bundle.putInt(LYRICS_INDEX, index);

        ContentView mContentView = new ContentView();
        // Save a copy of reference content view for local access
        mFragments.put(index, mContentView);

        mContentView.setArguments(bundle);
        return mContentView;
    }
}
