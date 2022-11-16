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
package org.cog.hymnchtv.hymnhistory;

import android.content.res.Resources;

import org.apache.http.util.EncodingUtils;
import org.apache.http.util.TextUtils;
import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.R;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import timber.log.Timber;

import static org.cog.hymnchtv.ContentView.LYRICS_BBS_TEXT;
import static org.cog.hymnchtv.ContentView.LYRICS_DBS_TEXT;
import static org.cog.hymnchtv.ContentView.LYRICS_ER_TEXT;
import static org.cog.hymnchtv.ContentView.LYRICS_XB_TEXT;
import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_MAX;

/**
 * The class provide handlers for the HistoryRecord
 *
 * The format of the history record consists of: hymnType, HymnNo, isFu, hymnTitle, timeStamp
 * a. hymnType: HYMN_BB HYMN_DB, HYMN_ER, HYMN_XB
 * b. HymnNo: Hymn number
 * c. isFu: true if the hymnNo if Fu
 * d. hymnTile: extract from the lyrics file
 * e. timeStamp: Timestamp of user last access to the hymnType/hymnNo
 *
 * @author Eng Chong Meng
 */
public class HistoryRecord
{
    public static final int NUMBER_OF_RECORDS_IN_HISTORY = 200;

    public static final String TABLE_NAME = "hymnHistory";
    public static final String HYMN_TYPE = "hymnType";
    public static final String HYMN_NO = "hymnNo";
    public static final String HYMN_FU = "isFu"; // set to 1 if fu else 0
    public static final String HYMN_TITLE = "hymnTitle";
    public static final String TIME_STAMP = "timeStamp";

    private final String mHymnType;
    private final int mHymnNo;
    private final boolean mIsFu;
    private final String mHymnTitle;
    private final Long mTimeStamp;

    public HistoryRecord(String hymnType, int hymnNo, boolean isFu)
    {
        this(hymnType, hymnNo, isFu, null, -1);
    }

    public HistoryRecord(String hymnType, int hymnNo, boolean isFu, String title, long timeStamp)
    {
        if (TextUtils.isEmpty(title))
            title = getHymnInfoFromFile(hymnType, hymnNo);

        if (timeStamp == -1)
            timeStamp = new Date().getTime();

        mHymnType = hymnType;
        mHymnNo = hymnNo;
        mIsFu = isFu;
        mHymnTitle = title;
        mTimeStamp = timeStamp;
    }

    public String getHymnType()
    {
        return mHymnType;
    }

    public int getHymnNo()
    {
        return mHymnNo;
    }

    public boolean isFu()
    {
        return mIsFu;
    }

    public String getHymnTitle()
    {
        return mHymnTitle;
    }

    public Long getTimeStamp()
    {
        return mTimeStamp;
    }

    public String getHymnNoFu()
    {
        String hymnNo = Integer.toString(mHymnNo);
        if (mIsFu && mHymnType.equals(HYMN_DB)) {
            hymnNo = "附" + (mHymnNo - HYMN_DB_NO_MAX);
        }
        return hymnNo;
    }

    /**
     * Fetch the hymn tile from the given hymnType and hymnNo for the lyrics file
     * The lyrics filename is based on the two given parameters
     *
     * @param hymnType The given hymnType
     * @param hymnNo The given hymnNo
     * @return the hymn title with category stripped off
     */
    private String getHymnInfoFromFile(String hymnType, int hymnNo)
    {
        String fileName = "";
        String hymnTitle = "";
        String lyricsPhrase = "";

        switch (hymnType) {
            case HYMN_DB:
                fileName = LYRICS_DBS_TEXT + hymnNo + ".txt";
                break;

            case HYMN_BB:
                fileName = LYRICS_BBS_TEXT + hymnNo + ".txt";
                break;

            case HYMN_XB:
                fileName = LYRICS_XB_TEXT + "xb" + hymnNo + ".txt";
                break;

            case HYMN_ER:
                fileName = LYRICS_ER_TEXT + "er" + hymnNo + ".txt";
                break;
        }

        try {
            InputStream in2 = HymnsApp.getAppResources().getAssets().open(fileName);
            byte[] buffer2 = new byte[in2.available()];
            if (in2.read(buffer2) == -1)
                return lyricsPhrase;

            String mResult = EncodingUtils.getString(buffer2, "utf-8");
            String[] mList = mResult.split("\r\n|\n");

            // fetch the hymn title with the category stripped off
            hymnTitle = mList[1];
            int idx = hymnTitle.lastIndexOf("－");
            if (idx != -1) {
                hymnTitle = hymnTitle.substring(idx + 1);
            }

            // Do the best guess to find the first phrase from lyrics
            idx = 4;
            String tmp = "";
            while (tmp.length() < 6) {
                tmp = mList[idx++];
            }

            mList = tmp.split("[，、‘’！：；。？]");
            for (String s : mList) {
                if (lyricsPhrase.length() < 6) {
                    lyricsPhrase += s;
                }
            }
        } catch (IOException e) {
            Timber.w("Content search error: %s", e.getMessage());
        }
        return lyricsPhrase;
    }

    /**
     * Convert the database history record info to user-friendly display List record
     *
     * @return HistoryRecord in user readable String
     */
    public @NotNull String toString()
    {
        Resources res = HymnsApp.getAppResources();
        String hymnInfo = "";

        switch (mHymnType) {
            case HYMN_ER:
                hymnInfo = res.getString(R.string.hymn_title_mc_er, mHymnNo, mHymnTitle);
                break;
            case HYMN_XB:
                hymnInfo = res.getString(R.string.hymn_title_mc_xb, mHymnNo, mHymnTitle);
                break;
            case HYMN_BB:
                hymnInfo = res.getString(R.string.hymn_title_mc_bb, mHymnNo, mHymnTitle);
                break;
            case HYMN_DB:
                if (mHymnNo > HYMN_DB_NO_MAX) {
                    hymnInfo = res.getString(R.string.hymn_title_mc_dbs, mHymnNo - HYMN_DB_NO_MAX, mHymnTitle);
                }
                else {
                    hymnInfo = res.getString(R.string.hymn_title_mc_db, mHymnNo, mHymnTitle);
                }
                break;
        }
        return hymnInfo;
    }

    /**
     * Convert a given string to the HistoryRecord
     *
     * @param historyString history record string with defined seperator
     * @return the derived History record.
     */
    public static HistoryRecord toRecord(String historyString)
    {
        historyString = historyString.trim().replaceAll("[ ]*[,\t][ ]*", ",");
        String[] recordItem = historyString.split(",");

        // Fu hymnNo is added to HYMN_DB_NO_MAX before storing in DB
        boolean isFu = "1".equals(recordItem[2]);
        int hymnNo = Integer.parseInt(recordItem[1]);
        if (isFu && (hymnNo <= HYMN_DB_NO_MAX)) {
            hymnNo += HYMN_DB_NO_MAX;
        }

        return new HistoryRecord(recordItem[0], hymnNo, isFu, null, -1);
    }
}
