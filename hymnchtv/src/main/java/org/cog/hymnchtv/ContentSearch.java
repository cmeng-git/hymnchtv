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
import static org.cog.hymnchtv.ContentView.LYRICS_DB_TEXT;
import static org.cog.hymnchtv.ContentView.LYRICS_NB_TEXT;
import static org.cog.hymnchtv.MainActivity.ATTR_NUMBER;
import static org.cog.hymnchtv.MainActivity.ATTR_PAGE;
import static org.cog.hymnchtv.MainActivity.ATTR_SEARCH;
import static org.cog.hymnchtv.MainActivity.ATTR_SELECT;
import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_BB_NO_MAX;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB_NO_MAX;
import static org.cog.hymnchtv.MainActivity.HYMN_DB_NO_TMAX;
import static org.cog.hymnchtv.MainActivity.HYMN_NB;
import static org.cog.hymnchtv.MainActivity.HYMN_NB_NO_MAX;
import static org.cog.hymnchtv.MainActivity.rangeBbLimit;

/**
 * ContentSearch: HymnApp search and diplay the the searched results based on uer input text
 * Only the simplified Chinese lyrics has all the contents for the hymns
 *
 * @author Eng Chong Meng
 * @author wayfarer
 */
public class ContentSearch extends FragmentActivity
{
    /* Allowable maximum matched items for display */
    private static final int HYMN_COUNT_MAX = 100;

    /* Length of matched text to display */
    private static final int RESULT_LENGTH = 100;

    // Traditional Chinese text files in LYRICS_DB_TEXT, range max per 100 (partial hymn list) i.e. partial only
    private static final int[] rangeMaxBB = {28, 138, 248, 330, 430, 534, 619, 753, 850, 916, 1006};

    /* running matched count number */
    private int mCount = 0;

    /* Array of hymnNo and hymnType pairs with the matched text */
    private final int[] mHymnNo = new int[HYMN_COUNT_MAX];
    private final Map<Integer, String> mHmynNoType = new LinkedHashMap<>();

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        String searchString = getIntent().getExtras().getString(ATTR_SEARCH);
        if (TextUtils.isEmpty((searchString)))
            return;

        List<Map<String, Object>> data = new ArrayList<>();

        InputStream inStream;
        byte[] buffer;

        int matchIdx;
        int temp;

        String fname;
        String result;

        // 大本詩歌: Simplified Chinese entry search in LYRICS_DBS_TEXT
        int hymnNo = 1;
        while (hymnNo <= HYMN_DB_NO_TMAX) {
            try {
                fname = LYRICS_DBS_TEXT + hymnNo + ".txt";
                inStream = getResources().getAssets().open(fname);
                buffer = new byte[inStream.available()];
                if (inStream.read(buffer) == 0)
                    continue;

                result = EncodingUtils.getString(buffer, "utf-8");
                result = result.substring(4);

                matchIdx = result.indexOf(searchString);
                if (matchIdx != -1) {
                    // find the start of the line
                    matchIdx = result.lastIndexOf("\n", matchIdx) + 1;

                    mHymnNo[mCount] = hymnNo;
                    mHmynNoType.put(hymnNo, HYMN_DB);

                    if (result.length() - matchIdx < RESULT_LENGTH) {
                        result = result.substring(matchIdx);
                    }
                    else {
                        result = result.substring(matchIdx, matchIdx + RESULT_LENGTH);
                    }
                    Map<String, Object> item_dbs = new HashMap<>();
                    if (hymnNo > HYMN_DB_NO_MAX) {
                        temp = hymnNo - HYMN_DB_NO_MAX;
                        item_dbs.put("match", getString(R.string.hymn_match_db_sp, temp, result));
                    }
                    else {
                        item_dbs.put("match", getString(R.string.hymn_match_db, hymnNo, result));
                    }
                    data.add(item_dbs);

                    if (mCount > HYMN_COUNT_MAX) {
                        break;
                    }
                    mCount++;
                }
            } catch (Exception e) {
                Timber.w("Content search error: %s", e.getMessage());
            }
            hymnNo++;
        }

        // 補充本詩歌: Simplified Chinese text entry search in LYRICS_BBS_TEXT
        if (mCount < HYMN_COUNT_MAX) {
            hymnNo = 1;
            while (hymnNo <= HYMN_BB_NO_MAX) {
                for (int rx = 0; rx < rangeBbLimit.length; rx++) {
                    if (hymnNo == rangeBbLimit[rx]) {
                        hymnNo = 100 * (rx + 1) + 1;
                        break;
                    }
                }

                try {
                    fname = LYRICS_BBS_TEXT + hymnNo + ".txt";
                    inStream = getResources().getAssets().open(fname);
                    buffer = new byte[inStream.available()];
                    if (inStream.read(buffer) == 0)
                        continue;

                    result = EncodingUtils.getString(buffer, "utf-8");
                    result = result.substring(3);

                    matchIdx = result.indexOf(searchString);
                    if (matchIdx != -1) {
                        // find the start of the line
                        matchIdx = result.lastIndexOf("\n", matchIdx) + 1;

                        mHymnNo[mCount] = hymnNo;
                        mHmynNoType.put(hymnNo, HYMN_BB);

                        if (result.length() - matchIdx < RESULT_LENGTH) {
                            result = result.substring(matchIdx);
                        }
                        else {
                            result = result.substring(matchIdx, matchIdx + RESULT_LENGTH);
                        }
                        Map<String, Object> item_bbs = new HashMap<>();
                        item_bbs.put("match", getString(R.string.hymn_match_bb, hymnNo, result));
                        data.add(item_bbs);

                        if (mCount > HYMN_COUNT_MAX) {
                            break;
                        }
                        mCount++;
                    }
                } catch (Exception e) {
                    Timber.w("Content search error: %s", e.getMessage());
                }
                hymnNo++;
            }
        }

        // 新歌颂咏: Simplified Chinese text entry search in LYRICS_NB_TEXT
        if (mCount < HYMN_COUNT_MAX) {
            hymnNo = 1;
            while (hymnNo <= HYMN_NB_NO_MAX) {
                try {
                    fname = LYRICS_NB_TEXT + "nb" + hymnNo + ".txt";
                    inStream = getResources().getAssets().open(fname);
                    buffer = new byte[inStream.available()];
                    if (inStream.read(buffer) == 0)
                        continue;

                    result = EncodingUtils.getString(buffer, "utf-8");
                    result = result.substring(4);

                    matchIdx = result.indexOf(searchString);
                    if (matchIdx != -1) {
                        // find the start of the line
                        matchIdx = result.lastIndexOf("\n", matchIdx) + 1;

                        mHymnNo[mCount] = hymnNo;
                        mHmynNoType.put(hymnNo, HYMN_NB);

                        if (result.length() - matchIdx < RESULT_LENGTH) {
                            result = result.substring(matchIdx);
                        }
                        else {
                            result = result.substring(matchIdx, matchIdx + RESULT_LENGTH);
                        }
                        Map<String, Object> item_nb = new HashMap<>();
                        item_nb.put("match", getString(R.string.hymn_match_nb, hymnNo, result));
                        data.add(item_nb);

                        if (mCount > HYMN_COUNT_MAX) {
                            break;
                        }
                        mCount++;
                    }
                } catch (Exception e) {
                    Timber.w("Content search error: %s", e.getMessage());
                }
                hymnNo++;
            }
        }

         // === Continue the search with the Traditional Chinese for any miss out items in 大本詩歌 and 補充本詩歌 === //
        // 大本詩歌: Traditional Chinese text entry search in LYRICS_DB_TEXT
        if (mCount < HYMN_COUNT_MAX) {
            hymnNo = 1;
            while (hymnNo <= HYMN_DB_NO_TMAX) {
                try {
                    fname = LYRICS_DB_TEXT + hymnNo + ".txt";
                    inStream = getResources().getAssets().open(fname);
                    buffer = new byte[inStream.available()];
                    if (inStream.read(buffer) == 0)
                        continue;

                    result = EncodingUtils.getString(buffer, "utf-8");
                    result = result.substring(4);

                    matchIdx = result.indexOf(searchString);
                    if (matchIdx != -1) {
                        // Skip if it is already found in simplified Chinese search
                        if (HYMN_DB.equals(mHmynNoType.get(hymnNo))) {
                            continue;
                        }

                        // find the start of the line
                        matchIdx = result.lastIndexOf("\n", matchIdx) + 1;

                        mHymnNo[mCount] = hymnNo;
                        mHmynNoType.put(hymnNo, HYMN_DB);

                        if (result.length() - matchIdx < RESULT_LENGTH) {
                            result = result.substring(matchIdx);
                        }
                        else {
                            result = result.substring(matchIdx, matchIdx + RESULT_LENGTH);
                        }
                        Map<String, Object> item_db = new HashMap<>();
                        if (hymnNo > HYMN_DB_NO_MAX) {
                            temp = hymnNo - HYMN_DB_NO_MAX;
                            item_db.put("match", getString(R.string.hymn_match_db_sp, temp, result));
                        }
                        else {
                            item_db.put("match", getString(R.string.hymn_match_db, hymnNo, result));
                        }
                        data.add(item_db);

                        if (mCount > HYMN_COUNT_MAX) {
                            break;
                        }
                        mCount++;
                    }
                } catch (Exception e) {
                    Timber.w("Content search error: %s", e.getMessage());
                }
                hymnNo++;
            }
        }

        // 補充本詩歌: Traditional Chinese text entry search in LYRICS_BB_TEXT
        if (mCount < HYMN_COUNT_MAX) {
            hymnNo = 1;
            while (hymnNo <= HYMN_BB_NO_MAX) {
                for (int rx = 0; rx < rangeMaxBB.length; rx++) {
                    if (hymnNo == rangeMaxBB[rx]) {
                        hymnNo = 100 * (rx + 1) + 1;
                        break;
                    }
                }

                try {
                    fname = LYRICS_BB_TEXT + hymnNo + ".txt";
                    inStream = getResources().getAssets().open(fname);
                    buffer = new byte[inStream.available()];
                    if (inStream.read(buffer) == 0)
                        continue;

                    result = EncodingUtils.getString(buffer, "utf-8");
                    result = result.substring(3);

                    matchIdx = result.indexOf(searchString);
                    if (matchIdx != -1) {
                        // Skip if it is already found in simplified Chinese search
                        if (HYMN_BB.equals(mHmynNoType.get(hymnNo))) {
                            continue;
                        }

                        // find the start of the line
                        matchIdx = result.lastIndexOf("\n", matchIdx) + 1;

                        mHymnNo[mCount] = hymnNo;
                        mHmynNoType.put(hymnNo, HYMN_BB);

                        if (result.length() - matchIdx < RESULT_LENGTH) {
                            result = result.substring(matchIdx);
                        }
                        else {
                            result = result.substring(matchIdx, matchIdx + RESULT_LENGTH);
                        }
                        Map<String, Object> item_bb = new HashMap<>();
                        item_bb.put("match", getString(R.string.hymn_match_bb, hymnNo, result));
                        data.add(item_bb);

                        if (mCount > HYMN_COUNT_MAX) {
                            break;
                        }
                        mCount++;
                    }
                } catch (Exception e) {
                    Timber.w("Content search error: %s", e.getMessage());
                }
                hymnNo++;
            }
        }
        showResult(data);
    }

    private void showResult(List<Map<String, Object>> data)
    {
        /*
         * Display the search result to the user
         */
        SimpleAdapter adapter = new SimpleAdapter(this, data, R.layout.search_result, new String[]{"match"},
                new int[]{R.id.textRow});

        if (mCount != 0) {
            setTitle(getString(R.string.hymn_match, mCount));
        }
        else {
            setTitle(R.string.hymn_match_none);
        }

        ListView listView = new ListView(this);
        listView.setAdapter(adapter);
        setContentView(listView);

        listView.setOnItemClickListener((adapterView, view, pos, id) -> {
            int hymnNo = mHymnNo[pos];

            Intent intent = new Intent();
            intent.setClass(this, ContentHandler.class);
            Bundle bundle = new Bundle();
            bundle.putInt(ATTR_NUMBER, hymnNo);
            bundle.putString(ATTR_SELECT, mHmynNoType.get(hymnNo));
            bundle.putString(ATTR_PAGE, PAGE_SEARCH);
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
