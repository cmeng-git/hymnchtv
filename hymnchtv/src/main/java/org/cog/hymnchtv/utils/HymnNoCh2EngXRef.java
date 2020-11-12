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

import org.apache.http.util.EncodingUtils;
import org.cog.hymnchtv.HymnsApp;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

import static org.cog.hymnchtv.ContentView.LYRICS_TOC;
import static org.cog.hymnchtv.HymnToc.TOC_BB;
import static org.cog.hymnchtv.HymnToc.TOC_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;

/**
 * HymnNoCh2EngXRef translates the hymn lyrics number from Chinese to English hymn No
 *
 * Current the class works only on DB and BB (not NB and ER); and valid for all hymn lyrics
 *
 * @author Eng Chong Meng
 */
public class HymnNoCh2EngXRef
{
    private static final String CH2ENG_FILE = "_ch2eng.txt";

    // Map containing the Chinese to English hymn No cross-reference
    public static final Map<Integer, Integer> xRefCh2EngBb = new HashMap<>();
    public static final Map<Integer, Integer> xRefCh2EngDb = new HashMap<>();

    // Generate the cross-reference table Map
    static {
        generateXRef(LYRICS_TOC + TOC_BB + CH2ENG_FILE, xRefCh2EngBb);
        generateXRef(LYRICS_TOC + TOC_DB + CH2ENG_FILE, xRefCh2EngDb);
    }

    public static Integer hymnNoCh2EngConvert(String hymnType, int hymnNo)
    {
        // default values set as invalid
        Integer hymnNoEng = null;

        switch (hymnType) {
            // 儿童诗歌
            case HYMN_ER:
                break;

            // 新歌颂咏
            case HYMN_XB:
                break;

            // 補充本
            case HYMN_BB:
                hymnNoEng = xRefCh2EngBb.get(hymnNo);
                break;

            // 大本诗歌
            case HYMN_DB:
                hymnNoEng = xRefCh2EngDb.get(hymnNo);
                break;
        }
        return hymnNoEng;
    }

    /**
     * Generate the xRef Map for Chinese to English hymn No
     *
     * @param xRefFile the file to extract info from
     * @param xRefDB the destination of the Map array to fill
     */
    private static void generateXRef(String xRefFile, Map<Integer, Integer> xRefDB)
    {
        String[] xRef;
        try {
            InputStream in2 = HymnsApp.getAppResources().getAssets().open(xRefFile);
            byte[] buffer2 = new byte[in2.available()];
            if (in2.read(buffer2) == -1)
                return;

            String mResult = EncodingUtils.getString(buffer2, "utf-8");
            String[] mList = mResult.split("\r\n|\n");

            int ml = 0;
            while (ml < mList.length) {
                if (mList[ml].matches("[0-9,]+")) {
                    xRef = mList[ml].split(",");
                    xRefDB.put(Integer.parseInt(xRef[0]), Integer.parseInt(xRef[1]));
                }
                ml++;
            }
        } catch (IOException e) {
            Timber.w("Content file access exception: %s", e.getMessage());
        }
    }
}
