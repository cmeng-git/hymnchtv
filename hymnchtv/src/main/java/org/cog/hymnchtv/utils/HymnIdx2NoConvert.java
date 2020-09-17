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

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_BB_NO_MAX;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB_NO_TMAX;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_ER_NO_MAX;
import static org.cog.hymnchtv.MainActivity.HYMN_NB;
import static org.cog.hymnchtv.MainActivity.HYMN_NB_NO_MAX;

import static org.cog.hymnchtv.MainActivity.rangeBbLimit;
import static org.cog.hymnchtv.MainActivity.rangeErLimit;

/**
 * This class converts the give hymn index and hymnType to the actual hymn lyrics number.
 * It checks for converted hymn lyrics number is within the supported ranges.
 *
 * Currently, the HymnTypes supported are 新歌颂咏, 新歌颂咏 and 大本詩歌
 *
 * The returned result is used to create the reference to the actual content files
 *
 * @author Eng Chong Meng
 */
public class HymnIdx2NoConvert
{
    /* Maximum number of lyrics in DB and start of its supplement */
    //    private static final int HYMN_DBS_START = 780;
    //    private static final int HYMN_DB_MAX = 787;
    //
    //    /* Maximum number of lyrics in BB */
    //    private static final int HYMN_BB_MAX = 1005;
    //
    //    /* Maximum number of lyrics in BB */
    //    private static final int HYMN_NB_MAX = 157;

    private static final Map<Integer, Integer> hymn_pages_bb = new HashMap<Integer, Integer>()
    {{
        put(9, 2);
        put(148, 2);
        put(257, 2);
        put(512, 2);
        put(702, 3);
        put(909, 3);
    }};

    private static final Map<Integer, Integer> hymn_pages_db = new HashMap<Integer, Integer>()
    {{
        put(128, 2);
        put(152, 5);
        put(188, 2);
        put(310, 2);
        put(316, 2);
        put(388, 3);
        put(465, 3);
        put(469, 2);
        put(717, 2);
        put(758, 2);
        put(776, 2);
    }};

    /* 补充本: hymn maximum number in per 100, 200, 300 ranges etc; it is used to compute valid hymn number */
    // Auto generated valid range for based 補充本 on rangeBbLimit
    private static final int[] rangeMaxBB = new int[rangeBbLimit.length];
    static {
        for (int i = 0; i< rangeBbLimit.length; i++){
            rangeMaxBB[i] = rangeBbLimit[i] - 1;
        }
    }

    /* 儿童诗歌: hymn maximum number in per 100, 200, 300 ranges etc; it is used to compute valid hymn number */
    // Auto generated valid range for based 儿童诗歌 on rangeBbLimit
    private static final int[] rangeMaxER = new int[rangeErLimit.length];
    static {
        for (int i = 0; i< rangeErLimit.length; i++){
            rangeMaxER[i] = rangeErLimit[i] - 1;
        }
    }

    /* Hymn item type for conversion */
    private final static String[] hymns = {HYMN_ER, HYMN_NB, HYMN_BB, HYMN_DB};
    // private final static String[] hymns = {HYMN_NB, HYMN_DB};

    /**
     * For testing only
     */
    public static void testRange()
    {
        for (int x = 0; x < HYMN_BB_NO_MAX; x++) {
            hymnIdx2NoConvert(HYMN_NB, x);
        }
    }

    /**
     * Convert the give hymn index and hymnType to the actual hymn lyrics number.
     * It checks for converted hymn lyrics number is within the supported ranges.
     *
     * @param hymnType Hymn type
     * @param index Index to convert. Index is usually the index from pageAdapter
     * @return Result of the translated hymn lyrics number if valid, else returen "-1,0"
     */
    public static int[] hymnIdx2NoConvert(String hymnType, int index)
    {
        /* Result to be returned i.e. {hymnNo, pageCount} */
        int[] hymnNo_page;

        /* hymnNo always start @ 1 */
        int hymnNo = index + 1;

        /* Cumulative of previous ranges unused index; use as start of next range index */
        int idxUnused;

        /* The number of pages for current hymnNo */
        Integer pageCount;

        switch (hymnType) {
            // 儿童诗歌
            case HYMN_ER:
                idxUnused = 0;
                for (int i = 0; i < rangeMaxER.length; i++) {
                    int idxMax = rangeMaxER[i];
                    int idxRangeStart = 100 * i;

                    // compute the number of unused index for all the previous ranges
                    if (i > 0) {
                        idxUnused += (rangeMaxER[i - 1] - 100 * (i - 1));
                    }

                    hymnNo = idxRangeStart + (index - idxUnused) + 1;
                    if (hymnNo <= idxMax) {
                        break;
                    }
                }

                if (hymnNo > HYMN_ER_NO_MAX) {
                    Timber.w("Hymn number not available: %s", hymnNo);
                    return new int[]{-1, 0};
                }

                hymnNo_page = new int[]{hymnNo, 1};
                Timber.d("Index to ER Hymn number %s => %s", index, hymnNo_page);
                break;

            // 新歌颂咏
            case HYMN_NB:
                if (hymnNo > HYMN_NB_NO_MAX) {
                    Timber.w("Hymn NB number not available: %s", hymnNo);
                    return new int[]{-1, 0};
                }

                hymnNo_page = new int[]{hymnNo, 1};
                // Timber.d("Index to NB Hymn number %s => %s", index, hymnNo_page);
                break;

            // 补充本
            case HYMN_BB:
                idxUnused = 0;
                for (int i = 0; i < rangeMaxBB.length; i++) {
                    int idxMax = rangeMaxBB[i];
                    int idxRangeStart = 100 * i;

                    // compute the number of unused index for all the previous ranges
                    if (i > 0) {
                        idxUnused += (rangeMaxBB[i - 1] - 100 * (i - 1));
                    }

                    hymnNo = idxRangeStart + (index - idxUnused) + 1;
                    if (hymnNo <= idxMax) {
                        break;
                    }
                }

                if (hymnNo > HYMN_BB_NO_MAX) {
                    Timber.w("Hymn number not available: %s", hymnNo);
                    return new int[]{-1, 0};
                }

                pageCount = hymn_pages_bb.get(hymnNo);
                hymnNo_page = new int[]{hymnNo, (pageCount == null) ? 1 : pageCount};

                // Timber.d("Index to BB Hymn number %s => %s", index, hymnNo_page);
                break;

            // 大本詩歌
            case HYMN_DB:
                if (hymnNo > HYMN_DB_NO_TMAX) {
                    Timber.w("Hymn DB number not available: %s", hymnNo);
                    return new int[]{-1, 0};
                }

                pageCount = hymn_pages_db.get(hymnNo);
                hymnNo_page = new int[]{hymnNo, (pageCount == null) ? 1 : pageCount};

                // Timber.d("Index to DB Hymn number %s => %s", index, hymnNo_page);
                break;

            default:
                return new int[]{-1, 0};
        }

        return hymnNo_page;
    }
}
