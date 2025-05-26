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

import static org.cog.hymnchtv.ContentView.LYRICS_INDEX;
import static org.cog.hymnchtv.ContentView.LYRICS_TYPE;
import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;
import static org.cog.hymnchtv.MainActivity.HYMN_XG;
import static org.cog.hymnchtv.MainActivity.HYMN_YB;
import static org.cog.hymnchtv.MainActivity.ybXTable;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_BB_ITEM_COUNT;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_ITEM_COUNT;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_ER_ITEM_COUNT;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_XB_ITEM_COUNT;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_XG_ITEM_COUNT;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_YB_ITEM_COUNT;

import android.os.Bundle;

import androidx.collection.LongSparseArray;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import org.cog.hymnchtv.utils.HymnNo2IdxConvert;
import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

/**
 * The hymn lyrics implementation for the user page sliding and display update using
 * the latest FragmentStateAdapter to minimize OOM Exception:
 * See https://stackoverflow.com/questions/18747975/what-is-the-difference-between-fragmentpageradapter-and-fragmentstatepageradapte
 * See Reported problem on FragmentStateAdapter: https://issuetracker.google.com/issues/177051960
 *
 * @author Eng Chong Meng
 */
public class MyPagerAdapter extends FragmentStateAdapter {
    private final String mHymnType;

    // Map array of index to ContentView for correct Content reference during access
    public final LongSparseArray<Fragment> mFragments = new LongSparseArray<>();

    public MyPagerAdapter(FragmentActivity fragmentActivity, String hymnType) {
        super(fragmentActivity);
        mHymnType = hymnType;
    }

    @Override
    public @NotNull Fragment createFragment(int index) {
        Bundle bundle = getHymnFragment(index);

        ContentView mContentView = new ContentView();
        mContentView.setArguments(bundle);
        // Save a copy of reference content view for local access
        mFragments.put(index, mContentView);

        return mContentView;
    }

    @Override
    public int getItemCount() {
        switch (mHymnType) {
            case HYMN_ER:
                return HYMN_ER_ITEM_COUNT;

            case HYMN_XB:
                return HYMN_XB_ITEM_COUNT;

            case HYMN_XG:
                return HYMN_XG_ITEM_COUNT;

            case HYMN_YB:
                return HYMN_YB_ITEM_COUNT;

            case HYMN_BB:
                // +1 to load image of non-existence hymn score number
                return HYMN_BB_ITEM_COUNT + 1;

            case HYMN_DB:
                return HYMN_DB_ITEM_COUNT;

            default:
                return 1;
        }
    }

    // get the hymnType and hymnNo from cross reference table for HYMN_YB if any or use the default
    private Bundle getHymnFragment(int index) {
        Bundle bundle = new Bundle();
        String hymnTN = ybXTable.get(index + 1);
        if (HYMN_YB.equals(mHymnType) && hymnTN != null) {
            String hymnType = MainActivity.getHymnType(hymnTN);
            int hymnNo = Integer.parseInt(hymnTN.substring(2));
            int hymnIdx = HymnNo2IdxConvert.hymnNo2IdxConvert(hymnType, hymnNo);

            bundle.putString(LYRICS_TYPE, hymnType);
            bundle.putInt(LYRICS_INDEX, hymnIdx);
            Timber.w("YB Fragment: %s %s %s", index, hymnNo, hymnType);
        }
        else {
            bundle.putString(LYRICS_TYPE, mHymnType);
            bundle.putInt(LYRICS_INDEX, index);
        }
        return bundle;
    }
}

