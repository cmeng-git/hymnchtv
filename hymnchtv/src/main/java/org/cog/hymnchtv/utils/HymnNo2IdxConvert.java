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

import android.net.Uri;
import android.util.Range;

import org.cog.hymnchtv.HymnsApp;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_BB_NO_MAX;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB_NO_MAX;
import static org.cog.hymnchtv.MainActivity.HYMN_DB_NO_TMAX;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_ER_NO_MAX;
import static org.cog.hymnchtv.MainActivity.HYMN_NB;
import static org.cog.hymnchtv.MainActivity.HYMN_NB_NO_MAX;
import static org.cog.hymnchtv.MainActivity.rangeBbLimit;
import static org.cog.hymnchtv.MainActivity.rangeErLimit;

/**
 * LyricsNoConvert hymns' lyrics file index number to file using actual hymn lyrics  number.
 * The class will work on DB BB and NB for both hymn score and midi files
 * This is only used once to migrate old content in v1.1.1 to new content for easy reference
 * All new release will be based on this naming convention
 *
 * @author Eng Chong Meng
 */
public class HymnNo2IdxConvert
{
    // Valid range for 補充本 per each 100 range
    public static final List<Range<Integer>> rangeBbValid = new ArrayList<>();

    static {
        for (int i = 0; i < rangeBbLimit.length; i++) {
            rangeBbValid.add(Range.create((100 * i) + 1, rangeBbLimit[i] - 1));
        }
    }

    /*
     * The max number of current midi item supported within each given 100 range of BB
     * Currently, the midi ResId is in consecutive number and starting @1 i.e. not the hymn number
     * The computation of the actual playback midi ResId is based on these infos. @see actual code below
     */
    private static final int[] midiBbRangeMax = {27, 137, 247, 329, 429, 533, 618, 752, 849, 915, 1005};

    // Valid range for 儿童诗歌 per each 100 range
    public static final List<Range<Integer>> rangeErValid = new ArrayList<>();

    static {
        for (int i = 0; i < rangeErLimit.length; i++) {
            rangeErValid.add(Range.create((100 * i) + 1, rangeErLimit[i] - 1));
        }
    }

    public static void testIdxRange()
    {
        for (int x = 1; x <= HYMN_BB_NO_MAX; x++) {
            hymnNo2IdxConvert(HYMN_BB, x);
        }
    }

    public static int hymnNo2IdxConvert(String hymnType, int hymnNo)
    {
        int nuiNext = 1000;
        String resFileName;

        // default values set as invalid
        int hymnIdx = -1;
        int midiIdx = -1;

        switch (hymnType) {
            // 儿童诗歌
            case HYMN_ER:
                if (hymnNo < HYMN_ER_NO_MAX) {
                    hymnIdx = hymnNo - 1;
                    int idxUsed = 0;
                    for (int rx = 0; rx < rangeErValid.size(); rx++) {
                        // compute the cumulative number of unused indexes for all the previous ranges
                        if (rx > 0) {
                            idxUsed += (100 * rx) - rangeErValid.get(rx - 1).getUpper();
                        }

                        if (rangeErValid.get(rx).contains(hymnNo)) {
                            hymnIdx = hymnNo - idxUsed - 1;
                            break;
                        }
                    }
                }
                break;

            case HYMN_NB:
                if (hymnNo < HYMN_NB_NO_MAX) {
                    hymnIdx = hymnNo - 1;
                    midiIdx = hymnNo;

                    // Copy resFileName to the computed destFile
                    String destHymnFN = "nb" + hymnNo + ".png";  // default
                    Timber.w("Translated hymn NB # %s => %s", hymnIdx, destHymnFN);
                }
                break;

            // 補充本 Conversion
            case HYMN_BB:
                if (hymnNo < HYMN_BB_NO_MAX) {
                    // Compute the midi playback library / ResId index
                    // see midiBbRangeMax = {27, 137, 247, 329, 429, 533, 618, 752, 849, 915, 1005};
                    int idxUsed = 0;
                    midiIdx = -1;
                    for (int i = 0; i < midiBbRangeMax.length; i++) {
                        int idxMax = midiBbRangeMax[i];
                        int idxRangeStart = 100 * i;

                        // compute the number of used index for all the previous ranges
                        if (i > 0) {
                            idxUsed += (midiBbRangeMax[i - 1] - 100 * (i - 1));
                        }

                        Range<Integer> rangeX = new Range<>(idxRangeStart + 1, idxRangeStart + 100);
                        if (rangeX.contains(hymnNo)) {
                            // Compute the midi ResId
                            if (hymnNo <= idxMax) {
                                midiIdx = (hymnNo - idxRangeStart) + idxUsed;
                            }
                            break;
                        }
                    }
                    //int[] rangeLM = {38, 151, 259, 350, 471, 544, 630, 763, 881, 931, 1006};
                    //                    idxUsed = 0;
                    //                    for (int i = 0; i < rangeLM.length; i++) {
                    //                        int idxMax = rangeLM[i];
                    //                        int idxRangeEnd = 100 * (i + 1);
                    //
                    //                        // compute the number of used index for all the previous ranges
                    //                        if (i > 0) {
                    //                            idxUsed += idxRangeEnd - idxMax + 1;
                    //                        }
                    //                        if (hymnIdx < idxMax) {
                    //                            break;
                    //                        }
                    //                    }

                    idxUsed = 0;
                    for (int rx = 0; rx < rangeBbValid.size(); rx++) {
                        // compute the cumulative number of unused indexes for all the previous ranges
                        if (rx > 0) {
                            idxUsed += (100 * rx) - rangeBbValid.get(rx - 1).getUpper();
                        }

                        if (rangeBbValid.get(rx).contains(hymnNo)) {
                            hymnIdx = hymnNo - idxUsed - 1;
                            break;
                        }
                    }
                    if (hymnIdx == -1)
                        break;

                    // Check for any multiple pages lyrics for conversation
                    //                    int a = (98 - 1);
                    //                    while (nuiNext < hymnIdx) {
                    //                        String destHymnFN = "bb" + (hymnNo - 1) + (char) a++ + ".png";
                    //                        Timber.w("Translated hymn  BB # %s => %s", nuiNext, destHymnFN);
                    //                    }

                    // use for compute for multiple page lyrics
                    nuiNext = hymnIdx + 1;

                    // Copy resFileName to the computed destFile
                    String destHymnFN = "bb" + hymnNo + ".png";  // default
                    Timber.w("Translated hymn BB # %s => %s", hymnIdx, destHymnFN);

                    if (midiIdx != -1) {
                        for (String midiPrefix : new String[]{"bm", "bmc"}) {
                            destHymnFN = midiIdx + midiIdx + ".mid";
                            // Timber.w("Translated midi BB # %s => %s", midiIdx, destHymnFN);
                        }
                    }
                }
                break;

            // 大本诗歌 Conversion
            case HYMN_DB:
                if (hymnNo < HYMN_DB_NO_TMAX) {
                    hymnIdx = hymnNo - 1;
                    midiIdx = hymnNo;

                    // Check for any multiple pages lyrics for conversation
                    int a = (98 - 1);
                    while (nuiNext < hymnIdx) {
                        resFileName = "d" + nuiNext;
                        Uri uriSource = HymnsApp.getDrawableUri(resFileName);

                        // Copy resFileName to the computed destFile
                        String destHymnFN = "db" + (hymnNo - 1) + (char) a++ + ".png";

                        Timber.w("Translated hymn DB # %s => %s", nuiNext, destHymnFN);
                        nuiNext++;
                    }
                    // use for compute for multiple page lyrics
                    nuiNext = hymnIdx + 1;

                    // Copy resFileName to the computed destFile
                    String destHymnFN = "db" + hymnNo + ".png";  // default
                    if (hymnNo > HYMN_DB_NO_MAX) {
                        destHymnFN = "dbs" + (hymnNo - HYMN_DB_NO_MAX) + ".png";
                    }

                    Timber.d("Translated hymn DB # %s => %s", hymnIdx, destHymnFN);
                    // Translate midi file if found
                    if (midiIdx != -1) {
                        for (String midiPrefix : new String[]{"dm", "dmc"}) {
                            destHymnFN = midiPrefix + midiIdx + ".mid";
                            Timber.w("Translated midi BB # %s => %s", midiIdx, destHymnFN);
                        }
                    }
                    break;
                }
        }
        return hymnIdx;
    }
}
