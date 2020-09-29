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
package org.cog.hymnchtv.persistance.migrations;

import android.content.Context;
import android.net.Uri;
import android.util.Range;

import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.persistance.FileBackend;
import org.cog.hymnchtv.persistance.FilePathHelper;

import java.io.File;

import timber.log.Timber;

import static org.cog.hymnchtv.ContentView.LYRICS_BB_SCORE;
import static org.cog.hymnchtv.ContentView.LYRICS_DB_SCORE;
import static org.cog.hymnchtv.ContentView.LYRICS_XB_SCORE;
import static org.cog.hymnchtv.MainActivity.rangeBbLimit;
import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;

/**
 * LyricsNoConvert hymns' lyrics file index number to file using actual hymn lyrics  number.
 * The class will work on DB BB and XB for both hymn scores and midi files
 * This is only used once to migrate old content in v1.1.1 to new content for easy reference
 * All new release will be based on this naming convention
 *
 * @author Eng Chong Meng
 */
public class Hymn2SnConvert
{
    private static String HYMN_BB_MIDI = "hymn_bb_midi/";
    private static String HYMN_DB_MIDI = "hymn_db_midi/";

    /* Maximum number of lyrics in DB and start of its supplement */
    private static final int HYMN_DB_MAX = 787; // inclusive of supplements

    /* Maximum number of lyrics in BB */
    private static final int HYMN_BB_MAX = 1006;


    /* Maximum number of lyrics in BB */
    private static final int HYMN_XB_MAX = 157;  // old content max

    /* maximum hymn number used in per 100, 200, 300 etc ranges, use to compute valid hymn number */
    private static final int[] rangeMaxSB = {38, 151, 259, 350, 471, 544, 630, 763, 881, 931};

    /*
     * The max number of current midi item supported within each given 100 range of BB
     * Currently, the midi ResId is in consecutive number and starting @1 i.e. not the hymn number
     * The computation of the actual playback midi ResId is based on these infos. @see actual code below
     */
    private static final int[] midiBbRangeMax = {27, 137, 247, 329, 429, 533, 618, 752, 849, 915, 1005};

    /* Hymn item type for conversion */
    private final static String[] hymns = {HYMN_XB, HYMN_BB, HYMN_DB};
    // private final static String[] hymns = {HYMN_DB};

    public static void startConvert()
    {
        Context ctx = HymnsApp.getGlobalContext();

        int hymnNo;
        int nui;
        int midiIdx = -1;
        String resFileName;

        // ResId offset for toc range, the first index used for bb and db #1 hymn numbering
        int bbSkip = 19;
        int dbSkip = 16;

        for (String hymnType : hymns) {
            switch (hymnType) {
                case HYMN_XB:
                    File lyricsSubDir = FileBackend.getHymnchtvStore(LYRICS_XB_SCORE, true);
                    hymnNo = 1;

                    while (hymnNo < HYMN_XB_MAX) {
                        midiIdx = hymnNo;
                        nui = hymnNo;
                        nui += 4;

                        resFileName = "n" + nui;
                        Uri uriSource = HymnsApp.getDrawableUri(resFileName);

                        // Copy resFileName to the computed destFile
                        String destHymnFN = "xb" + hymnNo + ".png";  // default
                        File destFile = new File(lyricsSubDir, destHymnFN);
                        FilePathHelper.copy(ctx, uriSource, destFile);
                        Timber.w("Translated hymn XB # %s => %s", nui, destHymnFN);

                        // next hymn no to translate
                        hymnNo++;

                    }
                    break;

                case HYMN_BB:
                    lyricsSubDir = FileBackend.getHymnchtvStore(LYRICS_BB_SCORE, true);
                    File midiSubDir = FileBackend.getHymnchtvStore(HYMN_BB_MIDI, true);
                    hymnNo = 1;
                    int nuiNext = bbSkip + 1;

                    while (hymnNo < HYMN_BB_MAX) {
                        // Re-align the loop hymn to a valid value
                        for (int rx = 0; rx < 10; rx++) {
                            if (hymnNo == rangeMaxSB[rx]) {
                                hymnNo = 100 * (rx + 1) + 1;
                                break;
                            }
                        }
                        nui = hymnNo;

                        // Compute the midi playback library / ResId index
                        // see midiBbRangeMax = {27, 137, 247, 329, 429, 533, 618, 752, 849, 915, 1005};
                        int idxUsed = 0;
                        midiIdx = -1;
                        for (int i = 0; i < 11; i++) {
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

                        // Compute bb ResId index for the user input hymn number i.e: hymn #1 => R.drawable.b20;
                        // int[] idxSkip = {0, 61, 110, 150, 201, 225, 270, 350, 386, 405, 471}
                        if (hymnNo > 0 && hymnNo < rangeBbLimit[0]) {
                            for (int xInc : new int[]{9, 32}) {
                                if (hymnNo > xInc) {
                                    nui++;
                                }
                                else {
                                    break;
                                }
                            }
                        }
                        else if (hymnNo > 100 && hymnNo < rangeBbLimit[1]) {
                            if (hymnNo > 148) {
                                nui++;
                            }
                            nui -= 61;
                        }
                        else if (hymnNo > 200 && hymnNo < rangeBbLimit[2]) {
                            if (hymnNo > 257) {
                                nui += 2;
                            }
                            nui -= 110;
                        }
                        else if (hymnNo > 300 && hymnNo < rangeBbLimit[3]) {
                            nui -= 150;
                        }
                        else if (hymnNo > 400 && hymnNo < rangeBbLimit[4]) {
                            for (int xInc : new int[]{440, 449, 451, 453, 468, 469}) {
                                if (hymnNo > xInc) {
                                    nui++;
                                }
                                else {
                                    break;
                                }
                            }
                            nui -= 201;
                        }
                        else if (hymnNo > 500 && hymnNo < rangeBbLimit[5]) {
                            for (int xInc : new int[]{512, 538, 541}) {
                                if (hymnNo > xInc) {
                                    nui++;
                                }
                                else {
                                    break;
                                }
                            }
                            nui -= 225;
                        }
                        else if (hymnNo > 600 && hymnNo < rangeBbLimit[6]) {
                            nui -= 279;
                        }
                        else if (hymnNo > 700 && hymnNo < rangeBbLimit[7]) {
                            if (hymnNo > 702) {
                                nui += 2;
                            }
                            nui -= 350;
                        }
                        else if (hymnNo > 800 && hymnNo < rangeBbLimit[8]) {
                            if (hymnNo > 875) {
                                nui++;
                            }
                            nui -= 386;
                        }
                        else if (hymnNo > 900 && hymnNo < rangeBbLimit[9]) {
                            for (int xInc : new int[]{909, 909, 921, 926}) {
                                if (hymnNo > xInc) {
                                    nui++;
                                }
                                else {
                                    break;
                                }
                            }
                            nui -= 405;
                        }
                        else if (hymnNo > 1000 && hymnNo <= HYMN_BB_MAX) {
                            nui -= 471;
                        }
                        nui += bbSkip;


                        // Check for any multiple pages lyrics for conversation
                        int a = (98 - 1);
                        while (nuiNext < nui) {
                            resFileName = "b" + nuiNext;
                            Uri uriSource = HymnsApp.getDrawableUri(resFileName);

                            // Copy resFileName to the computed destFile
                            String destHymnFN = "bb" + (hymnNo - 1) + (char) a++ + ".png";
                            File destFile = new File(lyricsSubDir, destHymnFN);
                            FilePathHelper.copy(ctx, uriSource, destFile);

                            Timber.w("Translated hymn BB # %s => %s", nuiNext, destHymnFN);
                            nuiNext++;
                        }
                        // use for compute for multiple page lyrics
                        nuiNext = nui + 1;

                        resFileName = "b" + nui;
                        Uri uriSource = HymnsApp.getDrawableUri(resFileName);

                        // Copy resFileName to the computed destFile
                        String destHymnFN = "bb" + hymnNo + ".png";  // default
                        File destFile = new File(lyricsSubDir, destHymnFN);
                        FilePathHelper.copy(ctx, uriSource, destFile);

                        Timber.w("Translated hymn BB # %s => %s", nui, destHymnFN);

                        // Translate midi file if found
                        if (midiIdx != -1) {
                            for (String midiPrefix : new String[]{"bm", "bmc"}) {
                                resFileName = midiPrefix + midiIdx;
                                uriSource = HymnsApp.getRawUri(resFileName);

                                // Copy resFileName to the computed destFile
                                destHymnFN = midiPrefix + hymnNo + ".mid";
                                destFile = new File(midiSubDir, destHymnFN);
                                FilePathHelper.copy(ctx, uriSource, destFile);

                                Timber.w("Translated midi BB # %s => %s", midiIdx, destHymnFN);
                            }
                        }
                        // next hymn no to translate
                        hymnNo++;
                    }
                    break;

                case HYMN_DB:
                    lyricsSubDir = FileBackend.getHymnchtvStore(LYRICS_DB_SCORE, true);
                    midiSubDir = FileBackend.getHymnchtvStore(HYMN_DB_MIDI, true);
                    hymnNo = 1;
                    nuiNext = dbSkip + 1;

                    while (hymnNo < HYMN_DB_MAX) {
                        midiIdx = hymnNo;
                        nui = hymnNo;

                        // Compute db ResId index for the input hymn number that have multiple pages, number give are multiple page
                        for (int xInc : new int[]{128, 152, 152, 152, 152, 188, 310, 316, 388, 388, 465, 465, 469, 717, 758, 776}) {
                            if (hymnNo > xInc) {
                                nui++;
                            }
                            else {
                                break;
                            }
                        }
                        nui += dbSkip;

                        // Check for any multiple pages lyrics for conversation
                        int a = (98 - 1);
                        while (nuiNext < nui) {
                            resFileName = "d" + nuiNext;
                            Uri uriSource = HymnsApp.getDrawableUri(resFileName);

                            // Copy resFileName to the computed destFile
                            String destHymnFN = "db" + (hymnNo - 1) + (char) a++ + ".png";
                            File destFile = new File(lyricsSubDir, destHymnFN);
                            FilePathHelper.copy(ctx, uriSource, destFile);

                            Timber.w("Translated hymn DB # %s => %s", nuiNext, destHymnFN);
                            nuiNext++;
                        }
                        // use for compute for multiple page lyrics
                        nuiNext = nui + 1;

                        resFileName = "d" + nui;
                        Uri uriSource = HymnsApp.getDrawableUri(resFileName);

                        // Copy resFileName to the computed destFile
                        String destHymnFN = "db" + hymnNo + ".png";  // default
                        if (hymnNo > HYMN_DB_MAX) {
                            destHymnFN = "dbs" + (hymnNo - HYMN_DB_MAX) + ".png";
                        }
                        File destFile = new File(lyricsSubDir, destHymnFN);
                        FilePathHelper.copy(ctx, uriSource, destFile);

                        Timber.w("Translated hymn DB # %s => %s", nui, destHymnFN);

                        // Translate midi file if found
                        if (midiIdx != -1) {
                            for (String midiPrefix : new String[]{"dm", "dmc"}) {
                                resFileName = midiPrefix + midiIdx;
                                uriSource = HymnsApp.getRawUri(resFileName);

                                // Copy resFileName to the computed destFile
                                destHymnFN = midiPrefix + hymnNo + ".mid";
                                destFile = new File(midiSubDir, destHymnFN);
                                FilePathHelper.copy(ctx, uriSource, destFile);

                                Timber.w("Translated midi BB # %s => %s", midiIdx, destHymnFN);
                            }
                        }
                        // next hymn number to translate
                        hymnNo++;
                    }
                    break;
            }
        }
    }
}
