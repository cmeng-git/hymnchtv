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

import static org.cog.hymnchtv.ContentView.LYRICS_BB_DIR;
import static org.cog.hymnchtv.ContentView.LYRICS_DB_DIR;
import static org.cog.hymnchtv.ContentView.LYRICS_ER_DIR;
import static org.cog.hymnchtv.ContentView.LYRICS_XB_DIR;
import static org.cog.hymnchtv.ContentView.LYRICS_XG_DIR;
import static org.cog.hymnchtv.ContentView.LYRICS_YB_DIR;
import static org.cog.hymnchtv.MainActivity.ATTR_SEARCH;
import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;
import static org.cog.hymnchtv.MainActivity.HYMN_XG;
import static org.cog.hymnchtv.MainActivity.HYMN_YB;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_BB_NO_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_TMAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_ER_NO_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_XB_NO_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_XG_NO_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_YB_NO_TMAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.rangeBbLimit;
import static org.cog.hymnchtv.utils.HymnNoValidate.rangeErLimit;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import androidx.activity.OnBackPressedCallback;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.util.EncodingUtils;
import org.apache.http.util.TextUtils;

import timber.log.Timber;

/**
 * ContentSearch: search and display the matched results based on uer input text string.
 * Only the simplified Chinese lyrics has full contents for the hymns.
 * Currently, block Traditional Chinese search as CG cause HymnApp comes to a halt.
 *
 * @author Eng Chong Meng
 * @author wayfarer
 */
public class ContentSearch extends BaseActivity {
    /* Allowable maximum matched items for display */
    private static final int HYMN_COUNT_MAX = 100;

    /* Length of matched text to display*/
    private static final int RESULT_MAX_LENGTH = 64;

    /* running matching count number */
    private int mCount = 0;

    // Array of matched hymnNo - used as a reference and index to find the hymntype to display the hymn lyrics
    private final int[] mHymnNo = new int[HYMN_COUNT_MAX];

    /* Map array of hymnNo to hymnType pairs that contain the matched text */
    private final Map<Integer, String> mHmynNoType = new LinkedHashMap<>();

    /**
     * Iterate all the defined hymn categories to search for user defined search string.
     * Display the found result in list view, for user select and enter to the hymn lyrics display
     *
     * @param savedInstanceState bundle
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String searchString = getIntent().getExtras().getString(ATTR_SEARCH);
        if (TextUtils.isEmpty((searchString)))
            return;

        // The search matched contents for display and user selection
        List<Map<String, Object>> data = new ArrayList<>();

        int temp;
        String fname;
        String result;

        // 大本詩歌: Simplified Chinese entry search in LYRICS_DBS_TEXT
        int hymnNo = 1;
        while (hymnNo <= HYMN_DB_NO_TMAX) {
            fname = LYRICS_DB_DIR + "db" + hymnNo + ".txt";
            result = getMatchResult(fname, searchString);
            if (result != null) {
                mHymnNo[mCount] = hymnNo;
                mHmynNoType.put(hymnNo, HYMN_DB);

                Map<String, Object> item_dbs = new HashMap<>();
                if (hymnNo > HYMN_DB_NO_MAX) {
                    temp = hymnNo - HYMN_DB_NO_MAX;
                    item_dbs.put("match", getString(R.string.hymn_match_db_sp, temp, result));
                }
                else {
                    item_dbs.put("match", getString(R.string.hymn_match_db, hymnNo, result));
                }
                data.add(item_dbs);

                mCount++;
                if (mCount >= HYMN_COUNT_MAX) {
                    break;
                }
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

                fname = LYRICS_BB_DIR + "bb" + hymnNo + ".txt";
                result = getMatchResult(fname, searchString);
                if (result != null) {
                    mHymnNo[mCount] = hymnNo;
                    mHmynNoType.put(hymnNo, HYMN_BB);

                    Map<String, Object> item_bbs = new HashMap<>();
                    item_bbs.put("match", getString(R.string.hymn_match_bb, hymnNo, result));
                    data.add(item_bbs);

                    mCount++;
                    if (mCount >= HYMN_COUNT_MAX) {
                        break;
                    }
                }
                hymnNo++;
            }
        }

        // 新歌颂咏: Simplified Chinese text entry search in LYRICS_XB_TEXT
        if (mCount < HYMN_COUNT_MAX) {
            hymnNo = 1;
            while (hymnNo <= HYMN_XB_NO_MAX) {
                fname = LYRICS_XB_DIR + "xb" + hymnNo + ".txt";
                result = getMatchResult(fname, searchString);
                if (result != null) {
                    mHymnNo[mCount] = hymnNo;
                    mHmynNoType.put(hymnNo, HYMN_XB);

                    Map<String, Object> item_xb = new HashMap<>();
                    item_xb.put("match", getString(R.string.hymn_match_xb, hymnNo, result));
                    data.add(item_xb);

                    mCount++;
                    if (mCount >= HYMN_COUNT_MAX) {
                        break;
                    }
                }
                hymnNo++;
            }
        }

        // 新詩歌本: Simplified Chinese text entry search in LYRICS_XG_TEXT
        if (mCount < HYMN_COUNT_MAX) {
            hymnNo = 1;
            while (hymnNo <= HYMN_XG_NO_MAX) {
                fname = LYRICS_XG_DIR + "xg" + hymnNo + ".txt";
                result = getMatchResult(fname, searchString);
                if (result != null) {
                    mHymnNo[mCount] = hymnNo;
                    mHmynNoType.put(hymnNo, HYMN_XG);

                    Map<String, Object> item_xg = new HashMap<>();
                    item_xg.put("match", getString(R.string.hymn_match_xg, hymnNo, result));
                    data.add(item_xg);

                    mCount++;
                    if (mCount >= HYMN_COUNT_MAX) {
                        break;
                    }
                }
                hymnNo++;
            }
        }

        // 青年诗歌: Simplified Chinese text entry search in LYRICS_XB_TEXT
        if (mCount < HYMN_COUNT_MAX) {
            hymnNo = 1;
            while (hymnNo <= HYMN_YB_NO_TMAX) {
                fname = LYRICS_YB_DIR + "yb" + hymnNo + ".txt";
                result = getMatchResult(fname, searchString);
                if (result != null) {
                    mHymnNo[mCount] = hymnNo;
                    mHmynNoType.put(hymnNo, HYMN_YB);

                    Map<String, Object> item_yb = new HashMap<>();
                    item_yb.put("match", getString(R.string.hymn_match_yb, hymnNo, result));
                    data.add(item_yb);

                    mCount++;
                    if (mCount >= HYMN_COUNT_MAX) {
                        break;
                    }
                }
                hymnNo++;
            }
        }

        // 儿童诗歌: Simplified Chinese text entry search in LYRICS_ER_TEXT
        if (mCount < HYMN_COUNT_MAX) {
            hymnNo = 1;
            while (hymnNo <= HYMN_ER_NO_MAX) {
                for (int rx = 0; rx < rangeErLimit.length; rx++) {
                    if (hymnNo == rangeErLimit[rx]) {
                        hymnNo = 100 * (rx + 1) + 1;
                        break;
                    }
                }

                fname = LYRICS_ER_DIR + "er" + hymnNo + ".txt";
                result = getMatchResult(fname, searchString);
                if (result != null) {
                    mHymnNo[mCount] = hymnNo;
                    mHmynNoType.put(hymnNo, HYMN_ER);

                    Map<String, Object> item_xb = new HashMap<>();
                    item_xb.put("match", getString(R.string.hymn_match_er, hymnNo, result));
                    data.add(item_xb);

                    mCount++;
                    if (mCount >= HYMN_COUNT_MAX) {
                        break;
                    }
                }
                hymnNo++;
            }
        }
        showResult(data);
        getOnBackPressedDispatcher().addCallback(backPressedCallback);
    }

    /**
     * Display the matched contents for user defined search string
     *
     * @param data The full search matched contents for display and user selection
     */
    private void showResult(List<Map<String, Object>> data) {
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

        // Show the lyrics of the user picked hymnNo.
        listView.setOnItemClickListener((adapterView, view, pos, id) -> {
            int hymnNo = mHymnNo[pos];
            String hymnType = mHmynNoType.get(hymnNo);
            MainActivity.showContent(this, hymnType, hymnNo, false);
        });
    }

    /**
     * Search the content of the given file for the specified search string.
     * return result if found, else null
     *
     * @param fName The name of file to search
     * @param sString the matching string
     *
     * @return matching string if found, else null
     */
    private String getMatchResult(String fName, String sString) {
        byte[] buffer;
        try {
            InputStream inStream = getResources().getAssets().open(fName);
            buffer = new byte[inStream.available()];
            if (inStream.read(buffer) == 0) {
                return null;
            }
        } catch (IOException e) {
            Timber.w("Content search error: %s", e.getMessage());
            return null;
        }

        String result = EncodingUtils.getString(buffer, "utf-8");
        result = result.substring(4);

        int matchIdx = result.indexOf(sString);
        if (matchIdx != -1) {
            // find the start of the line for display
            matchIdx = result.lastIndexOf("\n", matchIdx) + 1;

            result = result.substring(matchIdx);
            if (result.length() > RESULT_MAX_LENGTH) {
                result = result.substring(0, RESULT_MAX_LENGTH);
            }
            return result;
        }
        return null;
    }

    /**
     * Return to the search result display screen on BackKey press.
     */
    OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            setResult(-1, getIntent());
            finish();
        }
    };
}
