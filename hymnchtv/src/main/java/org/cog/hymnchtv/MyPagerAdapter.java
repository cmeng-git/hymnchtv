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
import android.view.ViewGroup;

import androidx.fragment.app.*;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static org.cog.hymnchtv.ContentView.HYMN_RESID;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_NB;
import static org.cog.hymnchtv.MainActivity.HYMN_BB;

/**
 * The hymn lyrics implementation for the user page sliding and display update
 *
 * @author Eng Chong Meng
 */
public class MyPagerAdapter extends FragmentPagerAdapter
{
    // The number of hymns for each category
    private static final int HYMN_ER_SIZE = 600;
    private static final int HYMN_XB_SIZE = 567;
    private static final int HYMN_DB_SIZE = 819;
    private static final int HYMN_NB_SIZE = 161;

    /**
     * A map reference to find the FragmentPagerAdapter's fragmentTag (String) by a given position (Integer)
     */
    private static final Map<Integer, String> mFragmentTags = new HashMap<>();
    private static FragmentManager mFragmentManager;


    private final ContentHandler mContentHandler;
    private final String mSelect;

    public MyPagerAdapter(FragmentManager fm, ContentHandler contentHandler, String sele)
    {
        // Must use BEHAVIOR_SET_USER_VISIBLE_HINT to see conference list on first slide to conference view
        // super(fm, BEHAVIOR_SET_USER_VISIBLE_HINT); not valid anymore after change to BaseChatRoomListAdapte
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mFragmentManager = fm;
        mContentHandler = contentHandler;
        mSelect = sele;
    }

    @Override
    public int getCount()
    {
        if (mSelect.equals(HYMN_BB)) {
            return HYMN_XB_SIZE;
        }
        if (mSelect.equals(HYMN_DB)) {
            return HYMN_DB_SIZE;
        }
        return HYMN_NB_SIZE;
    }

    public Fragment getItem(int index)
    {
        // The drawable id name has the following formats: HYMN_ER, HYMN_XB, HYMN_DB, HYMN_NB
        // i.e. R.drawable.???, R.drawable.n0, R.drawable.b0, R.drawable.d0 etc
        String resIdName;

        if (HYMN_NB.equals(mSelect)) {
            resIdName = "n" + index;
        }
        else if (HYMN_BB.equals(mSelect)) {
            resIdName = "b" + index;
        }
        else if (HYMN_DB.equals(mSelect)) {
            resIdName = "d" + index;
        }
        else { //if (HYMN_ER.equals(mSelect)) {
            resIdName = "C" + index;
        }

        // mContentHandler.updateHymnNo(index);

        Bundle bundle = new Bundle();
        bundle.putString(HYMN_RESID, resIdName);
        ContentView objContentView = new ContentView();
        objContentView.setArguments(bundle);
        return objContentView;
    }

    /**
     * Save the reference of position to FragmentPagerAdapter fragmentTag in mFragmentTags
     *
     * @param container The viewGroup
     * @param position The pager position
     * @return Fragment object at the specific location
     */
    @NotNull
    @Override
    public Object instantiateItem(@NotNull ViewGroup container, int position)
    {
        Object obj = super.instantiateItem(container, position);
        if (obj instanceof Fragment) {
            Fragment f = (Fragment) obj;
            assert f.getTag() != null;
            mFragmentTags.put(position, f.getTag());
        }
        return obj;
    }


    /**
     * Get the fragment reference for the given position in pager
     *
     * @param position position in the mFragmentTags
     * @return the requested fragment for the specified postion or null
     */
    public static Fragment getFragment(int position)
    {
        String tag = mFragmentTags.get(position);
        return (mFragmentManager != null) ? mFragmentManager.findFragmentByTag(tag) : null;
    }
}
