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

import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;
import static org.cog.hymnchtv.MainActivity.HYMN_XG;
import static org.cog.hymnchtv.MainActivity.HYMN_YB;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_BB_NO_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_TMAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_ER_NO_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_XB_NO_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_XG_NO_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_YB_NO_TMAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.rangeBbLimit;
import static org.cog.hymnchtv.utils.HymnNoValidate.rangeErLimit;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * This class converts the give hymn index and hymnType to the actual hymn lyrics number.
 * It also checks for converted hymn lyrics number is within the supported ranges.
 * Currently, the HymnTypes supported are 儿童诗歌, 新歌颂咏, 新詩歌本, 补充本 and 大本詩歌
 * The returned result is used by the caller to create the reference and fetch the actual content file
 *
 * @author Eng Chong Meng
 */
public class HymnIdx2NoConvert {
    /**
     * Lyric scores with multi-pages for the specified 补充本 hymn number
     */
    private static final Map<Integer, Integer> hymn_pages_bb = new HashMap<Integer, Integer>() {{
        put(9, 2);
        put(148, 2);
        put(257, 2);
        put(512, 2);
        put(702, 3);
        put(909, 3);
    }};

    /**
     * Lyric scores with multi-pages for the specified 大本詩歌 hymn number
     */
    private static final Map<Integer, Integer> hymn_pages_db = new HashMap<Integer, Integer>() {{
        put(128, 2);
        put(152, 5);
        put(164, 1);
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
    // Auto generated valid range for 補充本 based on rangeBbLimit
    private static final int[] rangeMaxBB = new int[rangeBbLimit.length];

    static {
        for (int i = 0; i < rangeBbLimit.length; i++) {
            rangeMaxBB[i] = rangeBbLimit[i] - 1;
        }
    }

    /* 儿童诗歌: hymn maximum number in per 100, 200, 300 ranges etc; it is used to compute valid hymn number */
    // Auto generated valid range for 儿童诗歌 based on rangeErLimit
    private static final int[] rangeMaxER = new int[rangeErLimit.length];

    static {
        for (int i = 0; i < rangeErLimit.length; i++) {
            rangeMaxER[i] = rangeErLimit[i] - 1;
        }
    }

    // hymnIdx >= HYMN_IDX__XB_MAX uses table for translation
    public static final int HYMN_IDX_XB_MAX = 167;
    private static final int[] hymnXbValid = new int[]{171};

    /**
     * For verification of the conversion (+ limit test)
     *
     * @param hymnType Hymn type to be verify
     * @param indexMax max index for the hymnType
     */
    public static void validateIdx2NoConversion(String hymnType, int indexMax) {
        for (int x = 0; x < (indexMax + 1); x++) {
            hymnIdx2NoConvert(hymnType, x);
        }
    }

    /**
     * Convert the give hymn index and hymnType to the actual hymn lyrics number.
     * It checks for converted hymn lyrics number is within the supported ranges.
     *
     * @param hymnType Hymn type
     * @param hymnIdx Index to convert. Index is usually the index from pageAdapter
     * @return Result of the translated hymn lyrics number if valid, else return "{-1,0}"
     */
    public static int[] hymnIdx2NoConvert(String hymnType, int hymnIdx) {
        /* Result to be returned i.e. {hymnNo, pageCount}; default ot invalid on each call */
        int[] hymn_No_Page = new int[]{-1, 0};

        /* hymnNo always start @ #1 */
        int hymnNo = hymnIdx + 1;

        /* Cumulative of previous ranges unused index; use as start of the next 100 range index */
        int idxUnused;

        /* The number of lyrics scores pages for computed hymnNo */
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

                    hymnNo = idxRangeStart + (hymnIdx - idxUnused) + 1;
                    if (hymnNo <= idxMax) {
                        break;
                    }
                }

                if (hymnNo <= HYMN_ER_NO_MAX) {
                    hymn_No_Page = new int[]{hymnNo, 1};
                }
                break;

            // 新歌颂咏; will generate invalid hymnNo for hymnIdx > 166; so fetch from translated table
            case HYMN_XB:
                if (hymnIdx >= HYMN_IDX_XB_MAX) {
                    hymnNo = hymnXbValid[hymnIdx - HYMN_IDX_XB_MAX];
                }
                // Timber.d("HYMN_XB: %s => %s", hymnIdx, hymnNo);
                if (hymnNo <= HYMN_XB_NO_MAX) {
                    hymn_No_Page = new int[]{hymnNo, 1};
                }
                break;

            // 新詩歌本
            case HYMN_XG:
                if (hymnNo <= HYMN_XG_NO_MAX) {
                    hymn_No_Page = new int[]{hymnNo, 1};
                }
                break;

            // 青年诗歌
            case HYMN_YB:
                if (hymnNo <= HYMN_YB_NO_TMAX) {
                    hymn_No_Page = new int[]{hymnNo, 1};
                }
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

                    hymnNo = idxRangeStart + (hymnIdx - idxUnused) + 1;
                    if (hymnNo <= idxMax) {
                        break;
                    }
                }

                if (hymnNo <= HYMN_BB_NO_MAX) {
                    pageCount = hymn_pages_bb.get(hymnNo);
                    hymn_No_Page = new int[]{hymnNo, (pageCount == null) ? 1 : pageCount};
                }
                break;

            // 大本詩歌
            case HYMN_DB:
                if (hymnNo <= HYMN_DB_NO_TMAX) {
                    pageCount = hymn_pages_db.get(hymnNo);
                    hymn_No_Page = new int[]{hymnNo, (pageCount == null) ? 1 : pageCount};
                }
                break;
        }

        if (hymn_No_Page[0] == -1) {
            Timber.w("Computed %s number exceeded hymnNo max: %s => %s", hymnType, hymnIdx, hymnNo);
        }
        // else {
        //    Timber.d("Conversion %s for index %s => %s", hymnType, hymnIdx, hymnNo_page[0]);
        // }

        return hymn_No_Page;
    }
}
