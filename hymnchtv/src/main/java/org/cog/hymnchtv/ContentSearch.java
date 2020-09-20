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

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import androidx.fragment.app.FragmentActivity;

import org.apache.http.util.EncodingUtils;
import org.apache.http.util.TextUtils;

import java.io.InputStream;
import java.util.*;

import timber.log.Timber;

import static org.cog.hymnchtv.ContentHandler.PAGE_SEARCH;
import static org.cog.hymnchtv.ContentView.LYRICS_BBS_TEXT;
import static org.cog.hymnchtv.ContentView.LYRICS_BB_TEXT;
import static org.cog.hymnchtv.ContentView.LYRICS_DBS_TEXT;
import static org.cog.hymnchtv.ContentView.LYRICS_DB_SCORE;
import static org.cog.hymnchtv.MainActivity.ATTR_NUMBER;
import static org.cog.hymnchtv.MainActivity.ATTR_PAGE;
import static org.cog.hymnchtv.MainActivity.ATTR_SEARCH;
import static org.cog.hymnchtv.MainActivity.ATTR_SELECT;
import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;

/**
 * ContentSearch: HymnApp search and diplay the the searched results based on uer input text
 * Only the simplified Chinese lyrics has all the contents for the hymns
 *
 * @author Eng Chong Meng
 * @author wayfarer
 */
public class ContentSearch extends FragmentActivity
{
    private static final int HYMN_DB_MAX = 787;
    private static final int HYMN_DBS_START = 780;
    private static final int HYMN_BB_MAX = 1006;
    private static final int HYMN_COUNT_MAX = 50;
    private static final int RESULT_LENGTH = 50;

    // Simplified Chinese search in sb, range max per 100 (complete hymn list)
    private static final int[] rangeMaxSB = {38, 151, 259, 350, 471, 544, 630, 763, 881, 931};

    // Traditional Chinese search in b, range max per 100 (partial hymn list)
    private static final int[] rangeMaxB = {28, 138, 248, 330, 430, 534, 619, 753, 850, 916};

    private int mCount = 0;
    private int fCount;

    private String hymnDB = HYMN_DB;
    private final String mPage = PAGE_SEARCH;

    private final int[] mItems = new int[HYMN_COUNT_MAX];

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        String searchString = getIntent().getExtras().getString(ATTR_SEARCH);
        if (TextUtils.isEmpty((searchString)))
            return;

        List<Map<String, Object>> data = new ArrayList<>();
        int hymnNo = 1;
        int matchIdx;
        int temp;
        String fname;
        String result;

        // Simplified Chinese text entry search in sdb
        while (hymnNo < HYMN_DB_MAX) {
            try {
                fname = LYRICS_DBS_TEXT + hymnNo + ".txt";
                InputStream in = getResources().getAssets().open(fname);
                byte[] buffer = new byte[in.available()];
                if (in.read(buffer) == 0)
                    continue;

                result = EncodingUtils.getString(buffer, "utf-8");
                result = result.substring(4);

                matchIdx = result.indexOf(searchString);
                if (matchIdx != -1) {
                    mItems[mCount] = hymnNo;
                    Map<String, Object> item = new HashMap<>();
                    if (result.length() - matchIdx < RESULT_LENGTH) {
                        result = result.substring(matchIdx);
                    }
                    else {
                        result = result.substring(matchIdx, matchIdx + RESULT_LENGTH);
                    }
                    if (hymnNo > HYMN_DBS_START) {
                        temp = hymnNo - HYMN_DBS_START;
                        item.put("match", getString(R.string.hymn_match_db_sp, temp, result));
                    }
                    else {
                        item.put("match", getString(R.string.hymn_match_db, hymnNo, result));
                    }
                    data.add(item);

                    if (mCount > HYMN_COUNT_MAX) {
                        break;
                    }
                    mCount++;
                }
                hymnNo++;
            } catch (Exception e) {
                Timber.w("Content search error: %s", e.getMessage());
            }
        }

        // Simplified Chinese text entry search in sb
        fCount = mCount;
        if (mCount < HYMN_COUNT_MAX) {
            hymnNo = 1;
            while (hymnNo < HYMN_BB_MAX) {
                for (int rx = 0; rx < 10; rx++) {
                    if (hymnNo == rangeMaxSB[rx]) {
                        hymnNo = 100 * (rx + 1) + 1;
                        break;
                    }
                }

                try {
                    fname = LYRICS_BBS_TEXT + hymnNo + ".txt";
                    InputStream in2 = getResources().getAssets().open(fname);
                    byte[] buffer2 = new byte[in2.available()];
                    if (in2.read(buffer2) == 0)
                        continue;
                    result = EncodingUtils.getString(buffer2, "utf-8");
                    result = result.substring(3);

                    matchIdx = result.indexOf(searchString);
                    if (matchIdx != -1) {
                        mItems[mCount] = hymnNo;
                        Map<String, Object> item2 = new HashMap<>();
                        if (result.length() - matchIdx < RESULT_LENGTH) {
                            result = result.substring(matchIdx);
                        }
                        else {
                            result = result.substring(matchIdx, matchIdx + RESULT_LENGTH);
                        }
                        item2.put("match", getString(R.string.hymn_match_bb, hymnNo, result));
                        data.add(item2);

                        if (mCount > HYMN_COUNT_MAX) {
                            break;
                        }
                        mCount++;
                    }
                    hymnNo++;
                } catch (Exception e) {
                    Timber.w("Content search error: %s", e.getMessage());
                }
            }
        }

        // Traditional Chinese text entry search in db
        if (mCount == 0) {
            hymnNo = 1;
            while (hymnNo < HYMN_DB_MAX) {
                try {
                    fname = LYRICS_DB_SCORE + hymnNo + ".txt";
                    InputStream in3 = getResources().getAssets().open(fname);
                    byte[] buffer3 = new byte[in3.available()];
                    if (in3.read(buffer3) == 0)
                        continue;
                    result = EncodingUtils.getString(buffer3, "utf-8");
                    result = result.substring(4);

                    matchIdx = result.indexOf(searchString);
                    if (matchIdx != -1) {
                        mItems[mCount] = hymnNo;
                        Map<String, Object> item3 = new HashMap<>();
                        if (result.length() - matchIdx < RESULT_LENGTH) {
                            result = result.substring(matchIdx);
                        }
                        else {
                            result = result.substring(matchIdx, matchIdx + RESULT_LENGTH);
                        }
                        if (hymnNo > HYMN_DBS_START) {
                            temp = hymnNo - HYMN_DBS_START;
                            item3.put("match", getString(R.string.hymn_match_db_sp, temp, result));
                        }
                        else {
                            item3.put("match", getString(R.string.hymn_match_db, hymnNo, result));
                        }
                        data.add(item3);

                        if (mCount > HYMN_COUNT_MAX) {
                            break;
                        }
                        mCount++;
                    }
                    hymnNo++;
                } catch (Exception e) {
                    Timber.w("Content search error: %s", e.getMessage());
                }
            }

            // Traditional Chinese text entry search in b
            fCount = mCount;
            if (mCount < HYMN_COUNT_MAX) {
                hymnNo = 1;
                while (hymnNo < HYMN_BB_MAX) {
                    for (int rx = 0; rx < 10; rx++) {
                        if (hymnNo == rangeMaxB[rx]) {
                            hymnNo = 100 * (rx + 1) + 1;
                            break;
                        }
                    }

                    try {
                        fname = LYRICS_BB_TEXT + hymnNo + ".txt";
                        InputStream in4 = getResources().getAssets().open(fname);
                        byte[] buffer4 = new byte[in4.available()];
                        if (in4.read(buffer4) == 0)
                            continue;
                        result = EncodingUtils.getString(buffer4, "utf-8");
                        result = result.substring(3);

                        matchIdx = result.indexOf(searchString);
                        if (matchIdx != -1) {
                            mItems[mCount] = hymnNo;
                            Map<String, Object> item4 = new HashMap<>();
                            if (result.length() - matchIdx < RESULT_LENGTH) {
                                result = result.substring(matchIdx);
                            }
                            else {
                                result = result.substring(matchIdx, matchIdx + RESULT_LENGTH);
                            }
                            item4.put("match", getString(R.string.hymn_match_bb, hymnNo, result));
                            data.add(item4);

                            if (mCount > HYMN_COUNT_MAX) {
                                break;
                            }
                            mCount++;
                        }
                        hymnNo++;
                    } catch (Exception e) {
                        Timber.w("Content search error: %s", e.getMessage());
                    }
                }
            }
        }

        /**
         * Display the search result to the user
         */
        SimpleAdapter adapter = new SimpleAdapter(this, data, R.layout.search_result, new String[]{"match"}, new int[]{R.id.textRow});
        if (mCount != 0) {
            setTitle(getString(R.string.hymn_match, mCount));
        }
        else {
            setTitle(R.string.hymn_match_none);
        }

        ListView listView = new ListView(this);
        listView.setAdapter(adapter);
        setContentView(listView);

        listView.setOnItemClickListener((adapterView, view, position, id) -> {
            if (fCount == 0) {
                hymnDB = HYMN_BB;
            }
            else if (position <= fCount) {
                hymnDB = HYMN_DB;
            }
            else {
                hymnDB = HYMN_BB;
            }

            Intent intent = new Intent();
            intent.setClass(this, ContentHandler.class);
            Bundle bundle = new Bundle();
            bundle.putInt(ATTR_NUMBER, mItems[position]);
            bundle.putString(ATTR_SELECT, hymnDB);
            bundle.putString(ATTR_PAGE, mPage);
            intent.putExtras(bundle);
            startActivityForResult(intent, -1);
        });
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            setResult(-1, getIntent());
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
