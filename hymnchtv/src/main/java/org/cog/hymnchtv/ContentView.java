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
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.cog.hymnchtv.glide.MyGlideApp;
import org.cog.hymnchtv.utils.HymnIdx2NoConvert;

import com.google.android.vending.expansion.zipfile.APKExpansionSupport;
import com.google.android.vending.expansion.zipfile.ZipResourceFile;

import java.io.*;

import timber.log.Timber;

import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;

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
    public static String LYRICS_XB_SCORE = "lyrics_xb_score/";
    public static String LYRICS_BB_SCORE = "lyrics_bb_score/";
    public static String LYRICS_DB_SCORE = "lyrics_db_score/";

    public static String LYRICS_ER_TEXT = "lyrics_er_text/";
    public static String LYRICS_XB_TEXT = "lyrics_xb_text/";
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

    private ZipResourceFile zipResFile;

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

        try {
            zipResFile = APKExpansionSupport.getAPKExpansionZipFile(getContext(), 104000, -1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause()
    {
        unregisterForContextMenu(lyricsView);
        super.onPause();
    }

    /**
     * The lyrics png file has the following formats: HYMN_ER, HYMN_XB, HYMN_BB, HYMN_DB
     * i.e. er, xb, bb, db followed by the hymn number, a, b, c etc for more than one page;
     * The files are stored in asset respective sub-dir e.g. LYRICS_XB_SCORE
     *
     * The content view can support up to 5 pages for user vertical scrolls
     *
     * @param hymnType see below cases
     * @param hymnIndex hymn index provided by the page adapter when user scroll
     */
    private void updateHymnContent(String hymnType, int hymnIndex)
    {
        String resPrefix;
        String resFName = "";

        int[] hymnScoreInfo = HymnIdx2NoConvert.hymnIdx2NoConvert(hymnType, hymnIndex);

        switch (hymnType) {
            case HYMN_ER:
                resPrefix = LYRICS_ER_SCORE + hymnScoreInfo[0];
                resFName = LYRICS_ER_TEXT + "er" + hymnScoreInfo[0] + ".txt";
                break;

            case HYMN_XB:
                resPrefix = LYRICS_XB_SCORE + "xb" + hymnScoreInfo[0];
                resFName = LYRICS_XB_TEXT + "xb" + hymnScoreInfo[0] + ".txt";
                break;

            case HYMN_BB:
                resPrefix = LYRICS_BB_SCORE + "bb" + hymnScoreInfo[0];
                resFName = LYRICS_BBS_TEXT + hymnScoreInfo[0] + ".txt";
                break;

            case HYMN_DB:
                resPrefix = LYRICS_DB_SCORE + "db" + hymnScoreInfo[0];
                resFName = LYRICS_DBS_TEXT + hymnScoreInfo[0] + ".txt";
                break;

            default:
                Timber.e("Unsupported content type: %s", hymnType);
                return;
        }

        // Show Hymn Lyric Scores for the selected hymnNo
        showLyricsScore(resPrefix, hymnScoreInfo);

        // Show Hymn Lyric Text for the selected hymnNo
        if (!TextUtils.isEmpty(resFName))
            showLyricsText(resFName);
    }

    /**
     * Display the selected Hymn Lyric Scores. Scores with multi-pages have suffixed with a, b, c and d.
     * i.e. support a total of 5 pages maximum.
     *
     * @param resPrefix The selected Hymn Lyric scores fileName prefix
     * @param hymnScoreInfo Contain info for the hymnNo and number of pages of the selected Lyric Scores
     */
    private void showLyricsScore(String resPrefix, int[] hymnScoreInfo)
    {
        int pages = hymnScoreInfo[1]; // The number of pages for the current hymn number
        ImageView contentView;

        String resName = resPrefix + ".png";
        // Uri resUri = Uri.fromFile(new File("//android_asset/", resName));
        // MyGlideApp.loadImage(mContentView, resUri);

        MyGlideApp.loadImage(mContentView, resName);

        if (pages > 1) {
            contentView = mConvertView.findViewById(R.id.contentView_a);
            resName = resPrefix + "a.png";
            // resUri = Uri.fromFile(new File("//android_asset/", resName));
            MyGlideApp.loadImage(contentView, resName);
        }
        else {
            return;
        }

        if (pages > 2) {
            contentView = mConvertView.findViewById(R.id.contentView_b);
            resName = resPrefix + "b.png";
            // resUri = Uri.fromFile(new File("//android_asset/", resName));
            MyGlideApp.loadImage(contentView, resName);
        }
        else {
            return;
        }

        if (pages > 3) {
            contentView = mConvertView.findViewById(R.id.contentView_c);
            resName = resPrefix + "c.png";
            // resUri = Uri.fromFile(new File("//android_asset/", resName));
            MyGlideApp.loadImage(contentView, resName);
        }
        else {
            return;
        }

        if (pages > 4) {
            contentView = mConvertView.findViewById(R.id.contentView_d);
            resName = resPrefix + "d.png";
            // resUri = Uri.fromFile(new File("//android_asset/", resName));
            MyGlideApp.loadImage(contentView, resName);
        }
    }

    /**
     * Display the selected hymn lyrics text
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
