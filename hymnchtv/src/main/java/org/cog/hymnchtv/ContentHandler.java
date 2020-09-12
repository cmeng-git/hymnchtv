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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Range;
import android.view.*;
import android.widget.ImageView;
import android.widget.PopupWindow;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.apache.http.util.EncodingUtils;
import org.cog.hymnchtv.utils.DepthPageTransformer;
import org.cog.hymnchtv.utils.DialogActivity;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static org.cog.hymnchtv.MainActivity.ATTR_NUMBER;
import static org.cog.hymnchtv.MainActivity.ATTR_PAGE;
import static org.cog.hymnchtv.MainActivity.ATTR_SELECT;
import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_NB;
import static org.cog.hymnchtv.MainActivity.PREF_MENU_SHOW;
import static org.cog.hymnchtv.MainActivity.PREF_SETTINGS;
import static org.cog.hymnchtv.MainActivity.TOC_BB;
import static org.cog.hymnchtv.MainActivity.TOC_DB;
import static org.cog.hymnchtv.MainActivity.TOC_ER;
import static org.cog.hymnchtv.MainActivity.TOC_NB;
import static org.cog.hymnchtv.MainActivity.rangeLM;

/**
 * The class handles the actual content source address decoding for the user selected hymn
 *
 * @author Eng Chong Meng
 */
@SuppressLint("NonConstantResourceId")
public class ContentHandler extends FragmentActivity
{
    public static final String PAGE_CONTENT = "content";
    public static final String PAGE_MAIN = "main";
    public static final String PAGE_SEARCH = "search";

    public static final String MIDI_DB = "dm";
    public static final String MIDI_DBC = "dmc";

    public static final String MIDI_BB = "bm";
    public static final String MIDI_BBC = "bmc";

    /*
     * The max number of midi item supported within each given 100 range of BB
     * Currently, the midi ResId is in consecutive number and starting @1 i.e. not the hymn number
     * The computation of the actual playback midi ResId is based on these infos. @see actual code below
    */
    private static final int[] midiBbRangeMax = {27, 137, 247, 329, 429, 533, 618, 752, 849, 915, 1005};

    private ViewPager mPager;
    private View playerUi;
    private ImageView btnPlay;

    public boolean isShowMenu;

    // Hymn stored resource index for midi playback
    private int midiIndex = -1;

    // Hymn number selected by user
    private int hymnNo;

    private String mPage = PAGE_CONTENT;
    private String mPlayMode = "";
    private String mSelect;

    public PopupWindow pop;

    public SharedPreferences sPreference;

    /**
     * The media controller used to handle the playback of the user selected hymn.
     */
    private MediaController mMediaController;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(1);
        getWindow().setFlags(1024, 1024);
        setContentView(R.layout.content_main);
        registerForContextMenu(findViewById(R.id.linear));

        sPreference = getSharedPreferences(PREF_SETTINGS, 0);
        isShowMenu = sPreference.getBoolean(PREF_MENU_SHOW, true);

        Bundle bundle = getIntent().getExtras();
        mSelect = bundle.getString(ATTR_SELECT);
        mPage = bundle.getString(ATTR_PAGE);

        int nui = bundle.getInt(ATTR_NUMBER);
        hymnNo = nui;
        midiIndex = -1;

        // ResId offset for toc range 
        int bbSkip = 19;
        int dbskip = 16;

        switch (mSelect) {
            // #TODO
            case HYMN_ER:
                mPlayMode = "";
                nui += 1;
                break;

            case HYMN_NB:
                mPlayMode = "";
                nui += 4;
                break;

            case HYMN_BB:
                // Compute the midi playback library / ResId index
                // see midiBbRangeMax = {27, 137, 247, 329, 429, 533, 618, 752, 849, 915, 1005};
                mPlayMode = "p";
                int idxUsed = 0;
                for (int i = 0; i < 11; i++) {
                    int idxMax = midiBbRangeMax[i];
                    int idxRangeStart = 100 * i;

                    // compute the number of used index for all the previous ranges
                    if (i > 0) {
                        idxUsed += (midiBbRangeMax[i - 1] - 100 * (i - 1));
                    }

                    Range<Integer> rangeX = new Range<>(idxRangeStart + 1, idxRangeStart + 100);
                    if (rangeX.contains(nui)) {
                        // Compute the midi ResId
                        if (hymnNo <= idxMax) {
                            midiIndex = (nui - idxRangeStart) + idxUsed;
                        }
                        break;
                    }
                }
                if (midiIndex == -1) {
                    mPlayMode = "n";
                }

                // Compute bb ResId index for the user input hymn number i.e: hymn #1 => R.drawable.b20;
                // current idx pages for specific hymn no:
                // {9, 32} {148}, {257, 257}, {440, 449, 451 453 468, 469, 2},
                // {512, 538, 541}, {702, 702}, {875}, {909, 909}, {921, 926}
                // int[] multiPageHymn = {9, 32};
                // int[] idxSkip = {0, 61, 110, 150, 201, 225, 270, 350, 386, 405, 471}
                if (hymnNo > 0 && hymnNo < rangeLM[0]) {
                    if (hymnNo > 9) {
                        nui++;
                    }
                    if (hymnNo > 32) {
                        nui++;
                    }
                }
                else if (hymnNo > 100 && hymnNo < rangeLM[1]) {
                    if (hymnNo > 148) {
                        nui++;
                    }
                    nui -= 61;
                }
                else if (hymnNo > 200 && hymnNo < rangeLM[2]) {
                    if (hymnNo > 257) {
                        nui += 2;
                    }
                    nui -= 110;
                }
                else if (hymnNo > 300 && hymnNo < rangeLM[3]) {
                    nui -= 150;
                }
                else if (hymnNo > 400 && hymnNo < rangeLM[4]) {
                    if (hymnNo > 440) {
                        nui++;
                    }
                    if (hymnNo > 449) {
                        nui++;
                    }
                    if (hymnNo > 451) {
                        nui++;
                    }
                    if (hymnNo > 453) {
                        nui++;
                    }
                    if (hymnNo > 468) {
                        nui++;
                    }
                    if (hymnNo > 469) {
                        nui++;
                    }
                    nui -= 201;
                }
                else if (hymnNo > 500 && hymnNo < rangeLM[5]) {
                    if (hymnNo > 512) {
                        nui++;
                    }
                    if (hymnNo > 538) {
                        nui++;
                    }
                    if (hymnNo > 541) {
                        nui++;
                    }
                    nui -= 225;
                }
                else if (hymnNo > 600 && hymnNo < rangeLM[6]) {
                    nui -= 279;
                }
                else if (hymnNo > 700 && hymnNo < rangeLM[7]) {
                    if (hymnNo > 702) {
                        nui += 2;
                    }
                    nui -= 350;
                }
                else if (hymnNo > 800 && hymnNo < rangeLM[8]) {
                    if (hymnNo > 875) {
                        nui++;
                    }
                    nui -= 386;
                }
                else if (hymnNo > 900 && hymnNo < rangeLM[9]) {
                    if (hymnNo > 909) {
                        nui += 2;
                    }
                    if (hymnNo > 921) {
                        nui++;
                    }
                    if (hymnNo > 926) {
                        nui++;
                    }
                    nui -= 405;
                }
                else if (hymnNo > 1000 && hymnNo <= MainActivity.BB_MAX) {
                    nui -= 471;
                }
                nui += bbSkip;
                break;

            case HYMN_DB:
                mPlayMode = "p";
                midiIndex = nui;

                // Compute db ResId index for the user input hymn number
                if (hymnNo > 128 && hymnNo < 153) {
                    nui++;
                }
                else if (hymnNo > 152 && hymnNo < 189) {
                    nui += 5;
                }
                else if (hymnNo > 188 && hymnNo < 311) {
                    nui += 6;
                }
                else if (hymnNo > 310 && hymnNo < 317) {
                    nui += 7;
                }
                else if (hymnNo > 316 && hymnNo < 389) {
                    nui += 8;
                }
                else if (hymnNo > 388 && hymnNo < 466) {
                    nui += 10;
                }
                else if (hymnNo > 465 && hymnNo < 470) {
                    nui += 12;
                }
                else if (hymnNo > 469 && hymnNo < 718) {
                    nui += 13;
                }
                else if (hymnNo > 717 && hymnNo < 759) {
                    nui += 14;
                }
                else if (hymnNo > 758 && hymnNo < 777) {
                    nui += 15;
                }
                else if (hymnNo > 776 && hymnNo < 787) {
                    nui += 16;
                }
                nui += dbskip;
                break;

            case TOC_ER:
                mPlayMode = "e";
                nui = 1;
                break;

            case TOC_NB:
                mPlayMode = "t";
                nui = 1;
                break;

            case TOC_BB:
                mSelect = HYMN_BB;
                mPlayMode = "m";
                break;

            case TOC_DB:
                mSelect = HYMN_DB;
                mPlayMode = "m";
                break;
        }

        // The pager adapter, which provides the pages to the view pager widget.
        FragmentManager fragmentManager = getSupportFragmentManager();
        PagerAdapter mPagerAdapter = new MyPagerAdapter(fragmentManager, this, mSelect);

        mPager = findViewById(R.id.viewPager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageTransformer(true, new DepthPageTransformer());

        // Set the viewPager to the user selected hymn number
        mPager.setCurrentItem(nui);

        // Attach the media controller player UI
        mMediaController = new MediaController();
        getSupportFragmentManager().beginTransaction().add(R.id.mediaPlayer, mMediaController).commit();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // Must only do this in OnResume, otherwise null
        playerUi = findViewById(R.id.playerUi);
        btnPlay = findViewById(R.id.playback_play);
        showPlayerUi(isShowMenu);
    }

    private void showPlayerUi(boolean show)
    {
        if (playerUi != null) {
            btnPlay.setAlpha(mPlayMode.equals("p") ? 1.0f : 0.3f);
            playerUi.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (pop != null) {
                pop.dismiss();
                pop = null;
            }
            isShowMenu = !isShowMenu;
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            backToHome();
        }
        return super.onKeyDown(keyCode, event);
    }

    // Do this only in PagerView Fragment, otherwise contextMenu is duplicated (display twice)
    // public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    // {
    //     super.onCreateContextMenu(menu, v, menuInfo);
    //     getMenuInflater().inflate(R.menu.content_menu, menu);
    // }

    public boolean onContextItemSelected(MenuItem item)
    {
        SharedPreferences.Editor editor = sPreference.edit();
        switch (item.getItemId()) {
            case R.id.alwayshow:
                isShowMenu = true;
                editor.putBoolean(PREF_MENU_SHOW, true);
                editor.apply();
                showPlayerUi(true);
                return true;

            case R.id.alwayhide:
                isShowMenu = false;
                editor.putBoolean(PREF_MENU_SHOW, false);
                editor.apply();
                showPlayerUi(false);
                return true;

            case R.id.menutoggle:
                isShowMenu = !isShowMenu;
                showPlayerUi(isShowMenu);
                return true;

            case R.id.help:
                DialogActivity.showDialog(this, R.string.help, R.string.content_help);
                return true;

            case R.id.home:
                backToHome();
                return true;

            default:
                return false;
        }
    }

    private void backToHome()
    {
        mMediaController.stopPlay();

        if (PAGE_MAIN.equals(mPage)) {
            Intent intent = new Intent();
            intent.setClass(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
        else if (PAGE_SEARCH.equals(mPage)) {
            finish();
        }
    }

    public void updateHymnNo(int idx)
    {
        hymnNo = idx;
    }

    public List<Uri> getPlayHymn(MediaType mediaType)
    {
        List<Uri> uriList = new ArrayList<>();
        if (midiIndex == -1)
            return uriList;

        switch (mSelect) {
            case HYMN_ER:
                HymnsApp.showToastMessage(R.string.hymn_info_er_media_none);
                break;

            case HYMN_NB:
                HymnsApp.showToastMessage(R.string.hymn_info_nb_media_none);
                break;

            case HYMN_BB:
                uriList.add(HymnsApp.getRawUri(MIDI_BB + midiIndex));
                uriList.add(HymnsApp.getRawUri(MIDI_BBC + midiIndex));
                break;

            case HYMN_DB:
                uriList.add(HymnsApp.getRawUri(MIDI_DB + midiIndex));
                uriList.add(HymnsApp.getRawUri(MIDI_DBC + midiIndex));
                break;
        }
        return uriList;
    }

    public String getHymnInfo()
    {
        String fName = "";
        String hymnInfo = "";
        String hymnTitle = "";
        Resources res = getResources();

        switch (mSelect) {
            case HYMN_ER:
                return res.getString(R.string.hymn_title_er, hymnNo, hymnTitle);

            case HYMN_NB:
                return res.getString(R.string.hymn_title_nb, hymnNo, hymnTitle);

            case HYMN_BB:
                fName = "sb/" + hymnNo + ".txt";
                break;

            case HYMN_DB:
                fName = "sdb/" + hymnNo + ".txt";
                break;
        }

        try {
            InputStream in2 = getResources().getAssets().open(fName);
            byte[] buffer2 = new byte[in2.available()];
            in2.read(buffer2);

            String mResult = EncodingUtils.getString(buffer2, "utf-8");
            String[] mList = mResult.split("\r\n");
            hymnTitle = mList[1];

            // Check the next two lines for additional info e.g. （诗篇二篇）（英1094）
            int idx = mList[2].indexOf("（");
            if (idx != -1) {
                hymnTitle = hymnTitle + mList[2].substring(idx);
            }
            else {
                idx = mList[3].indexOf("（");
                if (idx != -1) {
                    hymnTitle = hymnTitle + mList[3].substring(idx);
                }
            }
        } catch (Exception e) {
            Timber.w("Error getting info for hymn %s: %s", fName, e.getMessage());
            hymnTitle = hymnTitle + HymnsApp.getResString(R.string.gui_error_file_not_found, fName);
        }

        switch (mSelect) {
            case HYMN_BB:
                hymnInfo = res.getString(R.string.hymn_title_xb, hymnNo, hymnTitle);
                break;
            case HYMN_DB:
                if (hymnNo > 780) {
                    hymnInfo = res.getString(R.string.hymn_title_dbs, hymnNo - 780, hymnTitle);
                }
                else {
                    hymnInfo = res.getString(R.string.hymn_title_db, hymnNo, hymnTitle);
                }
                break;

        }
        return hymnInfo;
    }

}

