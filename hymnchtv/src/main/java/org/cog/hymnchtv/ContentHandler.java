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
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.PopupWindow;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.apache.http.util.EncodingUtils;
import org.cog.hymnchtv.utils.*;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static org.cog.hymnchtv.ContentView.LYRICS_BBS_TEXT;
import static org.cog.hymnchtv.ContentView.LYRICS_DBS_TEXT;
import static org.cog.hymnchtv.ContentView.LYRICS_NB_TEXT;
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

/**
 * The class handles the actual content source address decoding for the user selected hymn
 *
 * @author Eng Chong Meng
 */
@SuppressLint("NonConstantResourceId")
public class ContentHandler extends FragmentActivity implements ViewPager.OnPageChangeListener
{
    public static final String PAGE_CONTENT = "content";
    public static final String PAGE_MAIN = "main";
    public static final String PAGE_SEARCH = "search";

    public static final String MIDI_ER = "em";
    public static final String MIDI_NB = "md";

    public static final String MIDI_BB = "bm";
    public static final String MIDI_BBC = "bmc";

    public static final String MIDI_DB = "dm";
    public static final String MIDI_DBC = "dmc";

    public static final String PM_PLAY = "play";
    private static final String PM_NONE = "none";

    private ViewPager mPager;

    public boolean isShowMenu;

    // Hymn number selected by user
    private int hymnNo;
    private int hymnIdx = -1;

    // private String hymnFileName = "test";

    private String mPage = PAGE_CONTENT;
    private String mPlayMode = PM_NONE;
    private String mSelect;

    public PopupWindow pop;

    public SharedPreferences sPreference;

    /**
     * The media controller used to handle the playback of the user selected hymn.
     */
    private MediaController mMediaController;
    private MediaHandler mMediaHandler;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(1);
        getWindow().setFlags(1024, 1024);
        setContentView(R.layout.content_main);
        registerForContextMenu(findViewById(R.id.linear));

        // Attach the media controller player UI; Reuse the fragment is found;
        // do not create/add new, otherwise playerUi setVisibility is no working
        mMediaController = (MediaController) getSupportFragmentManager().findFragmentById(R.id.mediaPlayer);
        if (mMediaController == null) {
            mMediaController = new MediaController();
            getSupportFragmentManager().beginTransaction().add(R.id.mediaPlayer, mMediaController).commit();
        }

        // Always start with UiPlayer hidden if in landscape mode
        sPreference = getSharedPreferences(PREF_SETTINGS, 0);
        isShowMenu = sPreference.getBoolean(PREF_MENU_SHOW, true) && HymnsApp.isPortrait;

        Bundle bundle = getIntent().getExtras();
        mSelect = bundle.getString(ATTR_SELECT);
        mPage = bundle.getString(ATTR_PAGE);

        hymnNo = bundle.getInt(ATTR_NUMBER);

        switch (mSelect) {
            // #TODO
            case HYMN_ER:
                // Check for playable midi file existence
                int resId = HymnsApp.getFileResId(MIDI_ER + hymnNo, "raw");
                mPlayMode = (resId == 0) ? PM_NONE : PM_PLAY;

                // Convert the user input hymn number i.e: hymn #1 => #0 i.e.index number
                hymnIdx = HymnNo2IdxConvert.hymnNo2IdxConvert(mSelect, hymnNo);
                break;

            case HYMN_NB:
                // Check for playable midi file existence
                resId = HymnsApp.getFileResId(MIDI_NB + hymnNo, "raw");
                mPlayMode = (resId == 0) ? PM_NONE : PM_PLAY;

                // Convert the user input hymn number i.e: hymn #1 => #0 i.e.index number
                hymnIdx = HymnNo2IdxConvert.hymnNo2IdxConvert(mSelect, hymnNo);
                break;

            case HYMN_BB:
                // Check for playable midi file existence
                resId = HymnsApp.getFileResId(MIDI_BB + hymnNo, "raw");
                mPlayMode = (resId == 0) ? PM_NONE : PM_PLAY;

                // try {
                //     hymnFileName = HYMN_BB_ + "midi/bm" + hymnNo + ".mid";
                //     InputStream in = getResources().getAssets().open(hymnFileName);
                //     mPlayMode = PM_PLAY;
                //     in.close();
                // } catch (Exception e) {
                //     mPlayMode = PM_NONE;
                // }

                // Convert the user input hymn number i.e: hymn #1 => #0 i.e.index number
                hymnIdx = HymnNo2IdxConvert.hymnNo2IdxConvert(mSelect, hymnNo);
                break;

            case HYMN_DB:
                // Check for playable midi file existence
                resId = HymnsApp.getFileResId(MIDI_DB + hymnNo, "raw");
                mPlayMode = (resId == 0) ? PM_NONE : PM_PLAY;

                // Convert the user input hymn number i.e: hymn #1 => #0 i.e.index number
                hymnIdx = HymnNo2IdxConvert.hymnNo2IdxConvert(mSelect, hymnNo);
                break;

            case TOC_ER:
                mPlayMode = PM_NONE;
                break;

            case TOC_NB:
                mPlayMode = PM_NONE;
                break;

            case TOC_BB:
                mPlayMode = PM_NONE;
                break;

            case TOC_DB:
                mPlayMode = PM_NONE;
                break;
        }

        // The pager adapter, which provides the pages to the view pager widget.
        FragmentManager fragmentManager = getSupportFragmentManager();
        PagerAdapter mPagerAdapter = new MyPagerAdapter(fragmentManager, mSelect);

        mPager = findViewById(R.id.viewPager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageTransformer(true, new DepthPageTransformer());
        // Set the viewPager to the user selected hymn number
        if (hymnIdx != -1)
            mPager.setCurrentItem(hymnIdx);
        else
            mPager.setCurrentItem(hymnNo);

        mPager.addOnPageChangeListener(this);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Attach the File Transfer GUI; Reuse the fragment is found;
        // do not create/add new, otherwise setVisibility is no working
        mMediaHandler = (MediaHandler) getSupportFragmentManager().findFragmentById(R.id.filexferGui);
        if (mMediaHandler == null) {
            mMediaHandler = new MediaHandler();
            getSupportFragmentManager().beginTransaction().add(R.id.filexferGui, mMediaHandler).commit();
        }

        showPlayerUi(isShowMenu && !mSelect.startsWith("toc_"));
    }

    private void showPlayerUi(boolean show)
    {
        mMediaController.initPlayerUi(show, PM_PLAY.equals(mPlayMode));
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

    @Override
    public void onPageSelected(int pos)
    {
        if (mSelect.startsWith("toc_")) {
            return;
        }

        int tmp = HymnIdx2NoConvert.hymnIdx2NoConvert(mSelect, pos)[0];
        if (tmp != hymnNo) {
            hymnNo = tmp;
            mMediaController.initHymnInfo(getHymnInfo());
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
    {

    }

    @Override
    public void onPageScrollStateChanged(int state)
    {

    }

    public List<Uri> getPlayHymn(MediaType mediaType)
    {
        List<Uri> uriList = new ArrayList<>();

        if (!PM_PLAY.equals(mPlayMode)) {
            return uriList;
        }

        String resName = mSelect + mediaType.getSubDir(mediaType).replace("%s", "dm" + hymnNo);
        Uri resUridm = Uri.fromFile(new File("//android_asset/", resName));

        String rescName = mSelect + mediaType.getSubDir(mediaType).replace("%s", "dmc" + hymnNo);
        Uri resUridmc = Uri.fromFile(new File("//android_asset/", rescName));

        switch (mSelect) {
            case HYMN_ER:
                HymnsApp.showToastMessage(R.string.hymn_info_media_none);
                break;

            case HYMN_NB:
                HymnsApp.showToastMessage(R.string.hymn_info_media_none);
                uriList.add(HymnsApp.getRawUri(MIDI_NB + hymnNo));
                break;

            case HYMN_BB:
                uriList.add(HymnsApp.getRawUri(MIDI_BB + hymnNo));
                uriList.add(HymnsApp.getRawUri(MIDI_BBC + hymnNo));

                break;

            case HYMN_DB:
                uriList.add(HymnsApp.getRawUri(MIDI_DB + hymnNo));
                uriList.add(HymnsApp.getRawUri(MIDI_DBC + hymnNo));
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
                fName = LYRICS_NB_TEXT + "nb" + hymnNo + ".txt";
                break;

            case HYMN_BB:
                fName = LYRICS_BBS_TEXT + hymnNo + ".txt";
                break;

            case HYMN_DB:
                fName = LYRICS_DBS_TEXT + hymnNo + ".txt";
                break;
        }

        try {
            InputStream in2 = getResources().getAssets().open(fName);
            byte[] buffer2 = new byte[in2.available()];
            in2.read(buffer2);

            String mResult = EncodingUtils.getString(buffer2, "utf-8");
            String[] mList = mResult.split("\n"); // applicable for "\r\n"
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
            case HYMN_NB:
                hymnInfo = res.getString(R.string.hymn_title_nb, hymnNo, hymnTitle);
                break;
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
