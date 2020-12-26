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

import android.util.Range;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_BB_NO_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_TMAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_ER_NO_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_XB_NO_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.rangeBbLimit;
import static org.cog.hymnchtv.utils.HymnNoValidate.rangeErLimit;

/**
 * HymnNo2IdxConvert convert the hymn lyrics number to index for used by pagerAdapter.
 *
 * The class works on DB BB, NB and ER and valid for both hymn lyrics scores and midi files
 * All new releases v1.2.0 and above will be based on hymn lyrics numbering to fetch content files
 *
 * @author Eng Chong Meng
 */
public class HymnNo2IdxConvert
{
    // Valid range for 補充本 per each 100 range
    public static final List<Range<Integer>> rangeBbValid = new ArrayList<>();

    /* 補充本: hymn valid range value in per 100, 200, 300 ranges etc; it is used to compute hymn index */
    // Auto generated valid range for 補充本 based on rangeBbLimit
    static {
        for (int i = 0; i < rangeBbLimit.length; i++) {
            rangeBbValid.add(Range.create((100 * i) + 1, rangeBbLimit[i] - 1));
        }
    }

    // Valid range for 儿童诗歌 per each 100 range
    public static final List<Range<Integer>> rangeErValid = new ArrayList<>();

    /* 儿童诗歌: hymn valid range value in per 100, 200, 300 ranges etc; it is used to compute hymn index */
    // Auto generated valid range for 儿童诗歌 based on rangeErLimit
    static {
        for (int i = 0; i < rangeErLimit.length; i++) {
            rangeErValid.add(Range.create((100 * i) + 1, rangeErLimit[i] - 1));
        }
    }

    /**
     * For verification of the No2Idx conversion (+ limit test)
     *
     * @param hymnType Hymn type to be verify
     * @param maxNo max number for the hymnType
     */
    public static void validateNo2IdxConversion(String hymnType, int maxNo)
    {
        for (int x = 1; x <= (maxNo + 1); x++) {
            hymnNo2IdxConvert(hymnType, x);
        }
    }

    public static int hymnNo2IdxConvert(String hymnType, int hymnNo)
    {
        // default values set as invalid
        int hymnIdx = -1;

        switch (hymnType) {
            // 儿童诗歌
            case HYMN_ER:
                if (hymnNo <= HYMN_ER_NO_MAX) {
                    int idxUnused = 0;
                    for (int rx = 0; rx < rangeErValid.size(); rx++) {
                        // compute the cumulative number of unused indexes for all the previous ranges
                        if (rx > 0) {
                            idxUnused += (100 * rx) - rangeErValid.get(rx - 1).getUpper();
                        }

                        if (rangeErValid.get(rx).contains(hymnNo)) {
                            hymnIdx = hymnNo - idxUnused - 1;
                            break;
                        }
                    }
                }
                break;

            // 新歌颂咏
            case HYMN_XB:
                if (hymnNo <= HYMN_XB_NO_MAX) {
                    hymnIdx = hymnNo - 1;
                }
                break;

            // 補充本
            case HYMN_BB:
                if (hymnNo <= HYMN_BB_NO_MAX) {
                    int idxUnUsed = 0;
                    for (int rx = 0; rx < rangeBbValid.size(); rx++) {
                        // compute the cumulative number of unused indexes for all the previous ranges
                        if (rx > 0) {
                            idxUnUsed += (100 * rx) - rangeBbValid.get(rx - 1).getUpper();
                        }

                        if (rangeBbValid.get(rx).contains(hymnNo)) {
                            hymnIdx = hymnNo - idxUnUsed - 1;
                            break;
                        }
                    }
                }
                break;

            // 大本诗歌
            case HYMN_DB:
                if (hymnNo <= HYMN_DB_NO_TMAX) {
                    hymnIdx = hymnNo - 1;
                }
                break;
        }

        if (hymnIdx == -1) {
            Timber.w("Invalid %s hymnNo: %s => %s", hymnType, hymnNo, hymnIdx);
        }
        else {
            Timber.d("%s number to index: %s => %s", hymnType, hymnNo, hymnIdx);
        }

        return hymnIdx;
    }
}
