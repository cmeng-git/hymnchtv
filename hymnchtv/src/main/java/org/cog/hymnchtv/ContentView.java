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

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.cog.hymnchtv.utils.HymnIdx2NoConvert;

import java.io.*;

import timber.log.Timber;

import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_NB;
import static org.cog.hymnchtv.MainActivity.TOC_BB;
import static org.cog.hymnchtv.MainActivity.TOC_DB;
import static org.cog.hymnchtv.MainActivity.TOC_ER;
import static org.cog.hymnchtv.MainActivity.TOC_NB;

/**
 * The class displays the hymn lyrics content selected by user;
 * It is a part of the whole Hymn UI display
 *
 * @author Eng Chong Meng
 */
@SuppressLint("NonConstantResourceId")
public class ContentView extends Fragment
{
    public static String LYRICS_ER_SCORE = "lyrics_er_score/";
    public static String LYRICS_NB_SCORE = "lyrics_nb_score/";
    public static String LYRICS_BB_SCORE = "lyrics_bb_score/";
    public static String LYRICS_DB_SCORE = "lyrics_db_score/";

    public static String LYRICS_NB_TEXT = "lyrics_nb_text/";

    public static String LYRICS_BBS_TEXT = "lyrics_bbs_text/";
    public static String LYRICS_DBS_TEXT = "lyrics_dbs_text/";

    public static String LYRICS_BB_TEXT = "lyrics_bb_text/";
    public static String LYRICS_DB_TEXT = "lyrics_db_text/";

    public static String LYRICS_TOC = "lyrics_toc/";

    public final static String LYRICS_TYPE = "lyricsType";
    public final static String LYRICS_INDEX = "lyricsIndex";

    private View lyricsView;
    private View mConvertView;
    private ImageView mContentView = null;

    // Need this to prevent crash on rotation if there are other constructors implementation
    // public ContentView()
    // {
    // }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        mConvertView = inflater.inflate(R.layout.content_lyrics, container, false);
        lyricsView = mConvertView.findViewById(R.id.lyricsView);
        mContentView = mConvertView.findViewById(R.id.contentView);

        String lyricsType = getArguments().getString(LYRICS_TYPE);
        int lyricsIndex = getArguments().getInt(LYRICS_INDEX);

        if (!TextUtils.isEmpty(lyricsType)) {
            updateHymnContent(lyricsType, lyricsIndex);
        }
        return mConvertView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        registerForContextMenu(lyricsView);
    }

    @Override
    public void onPause()
    {
        unregisterForContextMenu(lyricsView);
        super.onPause();
    }

    /**
     * The lyrics png file has the following formats: HYMN_ER, HYMN_NB, HYMN_BB, HYMN_DB
     * i.e. er, nb, bb, db followed by the hymn number, a, b, c etc for more than one page;
     * The files are stored in asset respective sub-dir e.g. LYRICS_NB_SCORE
     *
     * The content view can support up to 5 pages for user vertical scrolling
     *
     * @param lyricsType see below cases
     * @param index hymn index provided by the page adapter when use scroll
     */
    private void updateHymnContent(String lyricsType, int index)
    {
        String resPrefix;
        String resFName = "";

        int[] hymnInfo = HymnIdx2NoConvert.hymnIdx2NoConvert(lyricsType, index);

        switch (lyricsType) {
            case HYMN_ER:
                resPrefix = LYRICS_ER_SCORE + hymnInfo[0];
                break;

            case HYMN_NB:
                resPrefix = LYRICS_NB_SCORE + "nb" + hymnInfo[0];
                resFName = LYRICS_NB_TEXT + "nb" + hymnInfo[0] + ".txt";
                break;

            case HYMN_BB:
                resPrefix = LYRICS_BB_SCORE + "bb" + hymnInfo[0];
                resFName = LYRICS_BBS_TEXT + hymnInfo[0] + ".txt";
                break;

            case HYMN_DB:
                resPrefix = LYRICS_DB_SCORE + "db" + hymnInfo[0];
                resFName = LYRICS_DBS_TEXT + hymnInfo[0] + ".txt";
                break;

            case TOC_ER:
                resPrefix = LYRICS_TOC + "er_toc";
                break;

            case TOC_NB:
                resPrefix = LYRICS_TOC + "nb_toc";
                break;

            case TOC_BB:
                resPrefix = LYRICS_TOC + "bb_toc";
                break;

            case TOC_DB:
                resPrefix = LYRICS_TOC + "db_toc";
                break;

            default: //if (HYMN_ER.equals(mSelect)) {
                resPrefix = LYRICS_ER_SCORE + "er" + hymnInfo[0];
        }

        // Show Hymn TOC content and return
        if (resPrefix.startsWith(LYRICS_TOC)) {
            showLyricsToc(resPrefix, index);
            return;
        }

        // Show Hymn Lyric Scores for the selected hymnNo
        showLyricsScore(resPrefix, hymnInfo);

        // Show Hymn Lyric Text for the selected hymnNo
        if (!TextUtils.isEmpty(resFName))
            showLyricsText(resFName);
    }

    /**
     * Display the selected Hymn TOC page
     *
     * @param resPrefix TOC fileName prefix
     * @param index the TOC index page
     */
    private void showLyricsToc(String resPrefix, int index)
    {
        if (resPrefix.startsWith(LYRICS_TOC)) {
            String resName = resPrefix + index + ((index > 19) ? ".jpg" : ".png");
            Uri resUri = Uri.fromFile(new File("//android_asset/", resName));
            MyGlideApp.loadImage(mContentView, resUri);
        }
    }

    /**
     * Display the selected Hymn Lyric Scores
     *
     * @param resPrefix The selected Hymn Lyric scores fileName prefix
     * @param hymnInfo Contain info the hymnNo and number of pages of the selected hymn Lyric Scores
     */
    private void showLyricsScore(String resPrefix, int[] hymnInfo)
    {
        int pages = hymnInfo[1]; // The number of pages for the current hymn number
        ImageView contentView;

        String resName = resPrefix + ".png";
        Uri resUri = Uri.fromFile(new File("//android_asset/", resName));
        MyGlideApp.loadImage(mContentView, resUri);

        if (pages > 1) {
            contentView = mConvertView.findViewById(R.id.contentView_a);
            resName = resPrefix + "a.png";
            resUri = Uri.fromFile(new File("//android_asset/", resName));
            MyGlideApp.loadImage(contentView, resUri);
        }
        else {
            return;
        }

        if (pages > 2) {
            contentView = mConvertView.findViewById(R.id.contentView_b);
            resName = resPrefix + "b.png";
            resUri = Uri.fromFile(new File("//android_asset/", resName));
            MyGlideApp.loadImage(contentView, resUri);
        }
        else {
            return;
        }

        if (pages > 3) {
            contentView = mConvertView.findViewById(R.id.contentView_c);
            resName = resPrefix + "c.png";
            resUri = Uri.fromFile(new File("//android_asset/", resName));
            MyGlideApp.loadImage(contentView, resUri);
        }
        else {
            return;
        }

        if (pages > 4) {
            contentView = mConvertView.findViewById(R.id.contentView_d);
            resName = resPrefix + "d.png";
            resUri = Uri.fromFile(new File("//android_asset/", resName));
            MyGlideApp.loadImage(contentView, resUri);
        }
    }

    /**
     * Display the selecte hymn lyrics text
     *
     * @param resFName Lyrics text resource fileName
     */
    private void showLyricsText(String resFName)
    {
        TextView lyricsView = mConvertView.findViewById(R.id.contentView_txt);
        lyricsView.setTextSize(HymnsApp.isPortrait ? 20 : 35);

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().getAssets().open(resFName)));
            StringBuilder lyrics = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                lyrics.append(line);
                lyrics.append('\n');
            }
            lyricsView.setText(lyrics);
        } catch (IOException e) {
            Timber.w("Error reading file: %s", resFName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        getActivity().getMenuInflater().inflate(R.menu.content_menu, menu);
    }
}
