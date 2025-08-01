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

import static org.cog.hymnchtv.ContentView.LYRICS_BB_DIR;
import static org.cog.hymnchtv.ContentView.SCORE_BB_DIR;
import static org.cog.hymnchtv.ContentView.LYRICS_DB_DIR;
import static org.cog.hymnchtv.ContentView.SCORE_DB_DIR;
import static org.cog.hymnchtv.ContentView.LYRICS_ER_DIR;
import static org.cog.hymnchtv.ContentView.SCORE_ER_DIR;
import static org.cog.hymnchtv.ContentView.LYRICS_XB_DIR;
import static org.cog.hymnchtv.ContentView.SCORE_XB_DIR;
import static org.cog.hymnchtv.ContentView.LYRICS_XG_DIR;
import static org.cog.hymnchtv.ContentView.SCORE_XG_DIR;
import static org.cog.hymnchtv.ContentView.LYRICS_YB_DIR;
import static org.cog.hymnchtv.MainActivity.ATTR_AUTO_PLAY;
import static org.cog.hymnchtv.MainActivity.ATTR_ENGLISH_NO;
import static org.cog.hymnchtv.MainActivity.ATTR_HYMN_NUMBER;
import static org.cog.hymnchtv.MainActivity.ATTR_HYMN_TYPE;
import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;
import static org.cog.hymnchtv.MainActivity.HYMN_XG;
import static org.cog.hymnchtv.MainActivity.HYMN_YB;
import static org.cog.hymnchtv.MainActivity.PREF_MENU_SHOW;
import static org.cog.hymnchtv.MainActivity.PREF_SETTINGS;
import static org.cog.hymnchtv.MainActivity.ybXTable;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_BB_DUMMY;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_TMAX;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.PopupWindow;

import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.http.util.EncodingUtils;
import org.apache.http.util.TextUtils;
import org.cog.hymnchtv.mediaconfig.MediaConfig;
import org.cog.hymnchtv.mediaconfig.MediaRecord;
import org.cog.hymnchtv.mediaconfig.NotionRecord;
import org.cog.hymnchtv.mediaconfig.QQRecord;
import org.cog.hymnchtv.mediaconfig.ShareWith;
import org.cog.hymnchtv.persistance.DatabaseBackend;
import org.cog.hymnchtv.persistance.FileBackend;
import org.cog.hymnchtv.utils.DepthPageTransformer;
import org.cog.hymnchtv.utils.HymnIdx2NoConvert;
import org.cog.hymnchtv.utils.HymnNo2IdxConvert;
import org.cog.hymnchtv.utils.HymnNoCh2EngXRef;
import org.cog.hymnchtv.webview.WebViewFragment;
import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

/**
 * The class handles the actual content source address decoding for the user selected hymn
 * The MainActivity (parent) must use SingleTask instead of SingleInstance.
 * SingleTask will call ContentHandler#onDestroy() when launch by user.
 * Otherwise ContentHandler#onDestroy() will not get call when user exits via HOME button.
 * New hymnchtv launch via MainActivity will skip this.onCreate(), and all user selections
 * are ignored; hence previous lyrics being displayed instead.
 *
 * @author Eng Chong Meng
 */
public class ContentHandler extends BaseActivity {
    public static String HYMNCHTV_FAQ_PLAYBACK = "https://cmeng-git.github.io/hymnchtv/faq.html#hymnch_0050";

    // sub-directory for various media type
    public static String MEDIA_MEDIA = "/media_media/";
    public static String MEDIA_JIAOCHANG = "/media_jiaochang/";
    public static String MEDIA_CHANGSHI = "/media_changshi/";
    public static String MEDIA_BANZOU = "/media_banzou/";

    public static final String MIDI_BB = "bm";
    public static final String MIDI_BBC = "bmc";
    public static final String MIDI_DB = "dm";
    public static final String MIDI_DBC = "dmc";

    public final DatabaseBackend mDB = DatabaseBackend.getInstance(HymnsApp.getGlobalContext());
    private MediaContentHandler mMediaContentHandler;

    private boolean isShowPlayerUi;

    // True if either youtube or exoPlayer is playing
    private boolean isMediaPlayerUi = false;

    // Hymn Type and number selected by user
    private boolean mAutoPlay = false;
    public boolean mAutoEnglish = false;
    public String mHymnType;
    private int mHymnNo;
    private int hymnIdx = -1;

    // Null if there is no corresponding English lyrics
    private Integer mHymnNoEng = null;
    private String mWebUrl = null;
    private String mHymnInfo = null;
    private String mHymnSearch;

    /**
     * 大本诗歌 MP3 file naming is a mess, so attempt to use lyricsPhrase; may not match all the times
     */
    private String lyricsPhrase;

    public enum UrlType {
        onlineHelp,
        englishLyrics,
        hymnGoogleSearch,
        hymnYoutubeSearch,
        hymnNotionSearch,
        hymnQqSearch
    }

    private MyPagerAdapter mPagerAdapter;
    private ViewPager2 mPager;

    public PopupWindow pop;
    public SharedPreferences sPreference;

    /**
     * The media controller used to handle the playback of the user selected hymn.
     */
    private MediaGuiController mMediaGuiController;
    private MediaDownloadHandler mMediaDownloadHandler;

    private View mWebView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        // getWindow().setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN); // will hide android notification bar
        setContentView(R.layout.content_main);
        registerForContextMenu(findViewById(R.id.linear));

        // Attach the media controller player UI; Reuse the fragment if found;
        // do not create/add new, otherwise playerUi setVisibility is no working
        mMediaGuiController = (MediaGuiController) getSupportFragmentManager().findFragmentById(R.id.mediaPlayer);
        if (mMediaGuiController == null) {
            mMediaGuiController = new MediaGuiController();
            getSupportFragmentManager().beginTransaction().replace(R.id.mediaPlayer, mMediaGuiController).commit();
        }
        mMediaContentHandler = MediaContentHandler.getInstance(this);
        isMediaPlayerUi = false;

        // Attach the File Transfer GUI; Use single instance created in HymnApp;
        // do not create/add new, otherwise GUI display is no working properly
        mMediaDownloadHandler = HymnsApp.mMediaDownloadHandler;
        getSupportFragmentManager().beginTransaction().replace(R.id.filexferGui, mMediaDownloadHandler).commit();

        mWebView = findViewById(R.id.webView);
        mWebView.setVisibility(View.INVISIBLE);

        // Always start with UiPlayer hidden if in landscape mode
        sPreference = getSharedPreferences(PREF_SETTINGS, 0);
        isShowPlayerUi = sPreference.getBoolean(PREF_MENU_SHOW, true);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mHymnType = bundle.getString(ATTR_HYMN_TYPE);
            mHymnNo = bundle.getInt(ATTR_HYMN_NUMBER);
            mHymnNoEng = HymnNoCh2EngXRef.hymnNoCh2EngConvert(mHymnType, mHymnNo);

            mAutoPlay = bundle.getBoolean(ATTR_AUTO_PLAY, false);
            int tmpNo = bundle.getInt(ATTR_ENGLISH_NO, -1);
            if (tmpNo != -1) {
                mAutoEnglish = true;
                mHymnNoEng = tmpNo;
            }
        }

        switch (mHymnType) {
            // Convert the user input hymn number i.e: hymn #1 => #0 i.e.index number
            case HYMN_ER:
            case HYMN_XB:
            case HYMN_XG:
            case HYMN_YB:
            case HYMN_BB:
            case HYMN_DB:
                hymnIdx = HymnNo2IdxConvert.hymnNo2IdxConvert(mHymnType, mHymnNo);
                break;
        }

        // The pager adapter, which provides the pages to the view pager widget.
        mPagerAdapter = new MyPagerAdapter(this, mHymnType);

        // Instantiate a ViewPager2 and a PagerAdapter.
        mPager = findViewById(R.id.viewPager);
        // FragmentStatePagerAdapter default seems to create only 2, so omit this statement, otherwise 9 items get created
        // FragmentStateAdapter default created 9, setOffscreenPageLimit has no effect
        // mPager.setOffscreenPageLimit(1);
        // mPager.setCurrentItem(hymnIdx, false) will force it to load only user selected page
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageTransformer(new DepthPageTransformer());

        // Set the viewPager to the user selected hymn number, no transform animation; this also fixed incorrect page being displayed
        // see https://issuetracker.google.com/issues/177051960
        if (hymnIdx != -1)
            mPager.setCurrentItem(hymnIdx, false);
        else
            mPager.setCurrentItem(mHymnNo, false);

        mPager.registerOnPageChangeCallback(initOnPageChangeCallback());
    }

    @Override
    protected void onResume() {
        super.onResume();
        showPlayerUi(isShowPlayerUi && HymnsApp.isPortrait);
    }

    public void showPlayerUi(boolean show) {
        mMediaGuiController.initPlayerUi(show);
    }

    public void showMediaPlayerUi() {
        mMediaGuiController.initPlayerUi(false);
        isMediaPlayerUi = true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (pop != null) {
                pop.dismiss();
                pop = null;
            }
            isShowPlayerUi = !isShowPlayerUi;
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mWebView.isShown()) {
                mWebView.setVisibility(View.INVISIBLE);
            }
            else if (isMediaPlayerUi) {
                if (mMediaContentHandler.isPlayerVisible()) {

                    mMediaContentHandler.releasePlayer();
                    // Must do this only after mMediaContentHandler.releasePlayer()
                    mMediaGuiController.initPlaybackSpeed();
                    isMediaPlayerUi = false;

                    // Restore the default MediaGuiController UI
                    getSupportFragmentManager().beginTransaction().replace(R.id.mediaPlayer, mMediaGuiController).commit();

                    // Need some delay for Transaction()
                    runOnUiThread(() -> new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        showPlayerUi(isShowPlayerUi && HymnsApp.isPortrait);
                    }, 100));
                }
                else {
                    mMediaContentHandler.setPlayerVisible(true);
                }
            }
            else if (mMediaGuiController.isPlaying()) {
                mMediaGuiController.stopPlay();
            }
            else {
                backToHome();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // Do this only in PagerView Fragment, otherwise contextMenu is duplicated (display twice)
    // public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    // {
    //     super.onCreateContextMenu(menu, v, menuInfo);
    //     getMenuInflater().inflate(R.menu.content_menu, menu);
    // }

    public boolean onContextItemSelected(MenuItem item) {
        SharedPreferences.Editor editor = sPreference.edit();
        ContentView contentView = (ContentView) mPagerAdapter.mFragments.get(mPager.getCurrentItem());

        switch (item.getItemId()) {
            case R.id.alwayshow:
                isShowPlayerUi = true;
                editor.putBoolean(PREF_MENU_SHOW, true);
                editor.apply();
                showPlayerUi(true);
                return true;

            case R.id.alwayhide:
                isShowPlayerUi = false;
                editor.putBoolean(PREF_MENU_SHOW, false);
                editor.apply();
                showPlayerUi(false);
                return true;

            case R.id.menutoggle:
                isShowPlayerUi = !(isShowPlayerUi && mMediaGuiController.isShown());
                showPlayerUi(isShowPlayerUi);
                return true;

            case R.id.scoreColorChange:
                if (contentView != null)
                    contentView.toggleScoreColor();
                return true;

            case R.id.lyrcsTextSizeInc:
            case R.id.lyrcsTextSizeDec:
                if (contentView != null)
                    contentView.setLyricsTextSize(item.getItemId() == R.id.lyrcsTextSizeInc);
                return true;

            case R.id.lyrcsEnglish:
                if (mHymnNoEng == null) {
                    HymnsApp.showToastMessage(R.string.error_english_lyrics_null, mHymnNo);
                    return true;
                }
                initWebView(UrlType.englishLyrics);
                return true;

            case R.id.lyrcsEnglishDelete:
                mDB.deleteLyricsEng(mHymnNoEng);
                return true;

            case R.id.lyrcsShare:
                lyricsShare();
                return true;

            case R.id.help:
                // About.hymnUrlAccess(this, HYMNCHTV_FAQ_PLAYBACK);
                initWebView(UrlType.onlineHelp);
                return true;

            case R.id.home:
                backToHome();
                return true;

            default:
                return false;
        }
    }

    private void backToHome() {
        mMediaGuiController.stopPlay();
        finish();
    }

    /**
     * Sharing of both the score png and lyrics text files via e.g. whatsapp
     */
    private void lyricsShare() {
        String resPrefix = "";
        String resFName = "";

        switch (mHymnType) {
            case HYMN_ER:
                resPrefix = SCORE_ER_DIR + mHymnNo;
                resFName = LYRICS_ER_DIR + "er" + mHymnNo;
                break;

            case HYMN_XB:
                resPrefix = SCORE_XB_DIR + "xb" + mHymnNo;
                resFName = LYRICS_XB_DIR + "xb" + mHymnNo;
                break;

            case HYMN_XG:
                resPrefix = SCORE_XG_DIR + "xg" + mHymnNo;
                resFName = LYRICS_XG_DIR + "xg" + mHymnNo;
                break;

            case HYMN_YB:
                resPrefix = SCORE_XB_DIR + "yb" + mHymnNo;
                resFName = LYRICS_XB_DIR + "yb" + mHymnNo;
                break;

            case HYMN_BB:
                resPrefix = SCORE_BB_DIR + "bb" + mHymnNo;
                resFName = LYRICS_BB_DIR + "bb" + mHymnNo;
                break;

            case HYMN_DB:
                resPrefix = SCORE_DB_DIR + "db" + mHymnNo;
                resFName = LYRICS_DB_DIR + "db" + mHymnNo;
                break;
        }

        String fnScore = resPrefix + ".png";
        File fileScore = new File(FileBackend.getHymnchtvStore(FileBackend.TMP, true), fnScore.split("/")[1]);

        String fnLyrics = resFName + ".txt";
        File fileLyrics = new File(FileBackend.getHymnchtvStore(FileBackend.TMP, true), fnLyrics.split("/")[1]);

        try {
            InputStream inputStream = getResources().getAssets().open(fnScore);
            FileOutputStream outputStream = new FileOutputStream(fileScore);
            FileBackend.copy(inputStream, outputStream);
            inputStream.close();
            outputStream.close();

            inputStream = getResources().getAssets().open(fnLyrics);
            outputStream = new FileOutputStream(fileLyrics);
            FileBackend.copy(inputStream, outputStream);
            inputStream.close();
            outputStream.close();

            ArrayList<Uri> imageUris = new ArrayList<>();
            imageUris.add(FileBackend.getUriForFile(this, fileScore));
            imageUris.add(FileBackend.getUriForFile(this, fileLyrics));
            ShareWith.share(this, getMediaUrl(), imageUris);
        } catch (IOException e) {
            Timber.e("lyrics shared: %s", e.getMessage());
        }
    }

    /**
     * Get the current mediaRecord URL link
     *
     * @return urlLink if available else null
     */
    private String getMediaUrl() {
        String urlLink = null;
        boolean isFu = mHymnType.equals(HYMN_DB) && (mHymnNo > HYMN_DB_NO_MAX);
        MediaRecord mediaRecord = new MediaRecord(mHymnType, mHymnNo, isFu, MediaType.HYMN_MEDIA);

        if (mDB.getMediaRecord(mediaRecord, true) && (mediaRecord.getMediaUri() != null)) {
            urlLink = mediaRecord.toString();
        }
        return urlLink;
    }

    /**
     * Callback interface for responding to changing state of the selected page.
     */
    private OnPageChangeCallback initOnPageChangeCallback() {
        return new OnPageChangeCallback() {
            /**
             * This method will be invoked when a new page becomes selected. Animation is not necessarily complete.
             *
             * @param position Position index of the new selected page.
             */
            @Override
            public void onPageSelected(int position) {
                int tmp = HymnIdx2NoConvert.hymnIdx2NoConvert(mHymnType, position)[0];
                if (tmp != mHymnNo) {
                    mHymnNo = tmp;
                    updateMediaPlayerInfo();

                    ContentView contentView = (ContentView) mPagerAdapter.mFragments.get(mPager.getCurrentItem());
                    if (contentView != null)
                        contentView.setLyricsTextScale();

                }
            }
        };
    }

    /**
     * Update all the required media info base on the current selected hymnType and hymnNo i.e.
     * a. English hymn number or null if none
     * b. The media player hymn title info
     * c. The text color of the Button Media
     */
    public void updateMediaPlayerInfo() {
        // Update both mHymnType and mHymnNo for share auto-fill
        MainActivity.setHymnTypeNo(mHymnType, mHymnNo);
        if (mHymnNo != HYMN_BB_DUMMY) {
            mHymnNoEng = HymnNoCh2EngXRef.hymnNoCh2EngConvert(mHymnType, mHymnNo);
        }

        // Check to see if all the mediaTypes are defined/available for the current user selected HymnType/HymnNo
        boolean[] isAvailable = getHymnMediaState();
        mHymnInfo = getHymnInfo();
        mMediaGuiController.initHymnInfo(mHymnInfo, isAvailable);
    }

    /**
     * Start to play after the file is downloaded. Call from mediaHandler.
     */
    public void startPlay() {
        mMediaGuiController.startPlay();
    }

    /**
     * Start playing the user selected hymn upon MediaGuiController init. Call from MediaGuiController.
     * Must reset to prevent multiple autoplay after exited from an external player.
     */
    public boolean isAutoPlay(boolean reset) {
        if (mAutoPlay && reset) {
            mAutoPlay = false;
            return true;
        }
        else
            return mAutoPlay;
    }

    public void onError(String statusText) {
        mMediaGuiController.playbackPlay.setImageResource(R.drawable.ic_play_stop);
        HymnsApp.showToastMessage(statusText);
    }

    /**
     * For testing of the getPlayHymn algorithms for the specified media Type
     * and proceed to download if proceedDownload is true;
     */
    public void da_link_test(MediaType mediaType, boolean proceedDownLoad) {
        for (int hymnIdx = 1; hymnIdx <= HYMN_DB_NO_TMAX; hymnIdx++) {
            int[] hymnNoPage = HymnIdx2NoConvert.hymnIdx2NoConvert(mHymnType, hymnIdx);
            mHymnNo = hymnNoPage[0];
            if (mHymnNo != -1) {
                getPlayHymn(mediaType, proceedDownLoad);
            }
        }
    }

    // 第112首 神生命的种子 http://g.cgbr.org/music/x/media/112x.mp3
    // http://g.cgbr.org/music/x/media/139.mp3

    /**
     * First priority: fetch the user defined DB media links/contents for the selected hymnType/hymnNo.
     * To save local storage space; the media url link is played via streaming using YoutubePlayer,
     * or ExoPlayer without downloading the file.
     * <p>
     * If none found, then fetch the required playback media resources from local directory if available.
     * Otherwise, fetch from online sites with the predefined link if available and if proceedDownLoad is true;
     * else drop to next mediaType search for playback
     *
     * @param mediaType media Type for the playback i.e. hymnType ER, JIAOCHANG, CHANGSHI or BANZOU
     * @param proceedDownLoad download from the specified dnLink if true;
     *
     * @return list of media resource to playback. Usually only one item, two for midi resources
     */
    public List<Uri> getPlayHymn(MediaType mediaType, boolean proceedDownLoad) {
        List<Uri> uriList = new ArrayList<>();
        /*
         * Fetch the user defined DB media links/contents for the selected hymnType/hymnNo;
         * an empty uriList is returned when it has been handled/played in the above process.
         *
         * Proceed to other media handlers if is not handled in getMediaUris i.e. not defined in DB
         * A media audio link or download link is returned. The media audio content can be
         * in mp3, mid, midi format.
         */
        if (mMediaContentHandler.getMediaUris(mHymnType, mHymnNo, mediaType, uriList)) {
            return uriList;
        }

        String dir = null;
        String fbLink = null;
        String fileName = mHymnNo + getHymnTitle();
        // http://mana.stmn1.com/

        switch (mHymnType) {
            case HYMN_ER:
                switch (mediaType) {
                    case HYMN_MEDIA:
                        dir = mHymnType + MEDIA_MEDIA;
                        if (isFileExist(dir, mHymnNo, uriList)) break;

                    case HYMN_JIAOCHANG:
                        dir = mHymnType + MEDIA_JIAOCHANG;
                        if (isFileExist(dir, mHymnNo, uriList)) break;

                    case HYMN_CHANGSHI:
                        dir = mHymnType + MEDIA_CHANGSHI;
                        if (isFileExist(dir, mHymnNo, uriList)) break;

                        if (proceedDownLoad) {
                            fileName = "C" + fileName + ".mp3";
                            // fbLink = String.format(Locale.US, "http://www.lightinnj.org/mp3/k-mp3/C%04d.mp3", mHymnNo);
                            fbLink = String.format(Locale.US, "http://mana.stmn1.com/sg/er/mp3/er%d.mp3", mHymnNo);
                            break;
                        }

                    case HYMN_BANZOU:
                        dir = mHymnType + MEDIA_BANZOU;
                        if (isFileExist(dir, mHymnNo, uriList)) break;
                }
                break;

            case HYMN_XB:
                switch (mediaType) {
                    case HYMN_MEDIA:
                        dir = mHymnType + MEDIA_MEDIA;
                        if (isFileExist(dir, mHymnNo, uriList)) break;

                    case HYMN_JIAOCHANG:
                        dir = mHymnType + MEDIA_JIAOCHANG;
                        if (isFileExist(dir, mHymnNo, uriList)) break;

                    case HYMN_CHANGSHI:
                        dir = mHymnType + MEDIA_CHANGSHI;
                        if (isFileExist(dir, mHymnNo, uriList)) break;

                        if (proceedDownLoad) {
                            fileName = "X" + fileName + ".mp3";
                            // fbLink = String.format(Locale.US, "http://g.cgbr.org/music/x/media/%03d.mp3", mHymnNo);
                            // fbLink = String.format(Locale.US, "http://mana.stmn1.com/sg/xin/mp3/X%d.mp3", mHymnNo);
                            fbLink = String.format(Locale.US, "http://four.soqimp.com/sg/xin/mp3/X%d.mp3", mHymnNo);
                            break;
                        }

                    case HYMN_BANZOU:
                        dir = mHymnType + MEDIA_BANZOU;
                        if (isFileExist(dir, mHymnNo, uriList)) break;
                }
                break;

            case HYMN_XG:
                switch (mediaType) {
                    case HYMN_MEDIA:
                        dir = mHymnType + MEDIA_MEDIA;
                        if (isFileExist(dir, mHymnNo, uriList)) break;

                    case HYMN_JIAOCHANG:
                        dir = mHymnType + MEDIA_JIAOCHANG;
                        if (isFileExist(dir, mHymnNo, uriList)) break;

                    case HYMN_CHANGSHI:
                        dir = mHymnType + MEDIA_CHANGSHI;
                        if (isFileExist(dir, mHymnNo, uriList)) break;

                        if (proceedDownLoad) {
                            fileName = "xg" + fileName + ".mp3";
                            // http://mana.stmn1.com/sg/csr/mp3/csr20.mp3
                            // fbLink = String.format(Locale.US, "http://mana.stmn1.com/sg/csr/mp3/csr%d.mp3", mHymnNo);
                            fbLink = String.format(Locale.US, "http://four.soqimp.com/sg/csr/mp3/csr%d.mp3", mHymnNo);
                            break;
                        }

                    case HYMN_BANZOU:
                        dir = mHymnType + MEDIA_BANZOU;
                        if (isFileExist(dir, mHymnNo, uriList)) break;
                }
                break;

            case HYMN_YB:
                switch (mediaType) {
                    case HYMN_MEDIA:
                        dir = mHymnType + MEDIA_MEDIA;
                        if (isFileExist(dir, mHymnNo, uriList)) break;

                    case HYMN_JIAOCHANG:
                        dir = mHymnType + MEDIA_JIAOCHANG;
                        if (isFileExist(dir, mHymnNo, uriList)) break;

                    case HYMN_CHANGSHI:
                        dir = mHymnType + MEDIA_CHANGSHI;
                        if (isFileExist(dir, mHymnNo, uriList)) break;

                        if (proceedDownLoad) {
                            fileName = "xg" + fileName + ".mp3";
                            // fbLink = String.format(Locale.US, "http://mana.stmn1.com/sg/yb/mp3/csr%d.mp3", mHymnNo);
                            break;
                        }

                    case HYMN_BANZOU:
                        dir = mHymnType + MEDIA_BANZOU;
                        if (isFileExist(dir, mHymnNo, uriList)) break;
                }
                break;

            case HYMN_BB:
                switch (mediaType) {
                    case HYMN_MEDIA:
                        dir = mHymnType + MEDIA_MEDIA;
                        if (isFileExist(dir, mHymnNo, uriList)) break;

                    case HYMN_JIAOCHANG:
                        dir = mHymnType + MEDIA_JIAOCHANG;
                        if (isFileExist(dir, mHymnNo, uriList)) break;

                    case HYMN_CHANGSHI:
                        dir = mHymnType + MEDIA_CHANGSHI;
                        if (isFileExist(dir, mHymnNo, uriList)) break;

                        if (proceedDownLoad) {
                            fileName = "B" + fileName + ".mp3";
                            // https://www.hymnal.net/Hymns/Chinese/mp3/ch_0048_vocal.mp3
                            // fbLink = String.format(Locale.US, "https://www.hymnal.net/cn/hymn/ts/%d/f=sing", mHymnNo);
                            // fbLink = String.format(Locale.US, "http://four.soqimp.com/sg/bu/mp3/B%d.mp3", mHymnNo);
                            fbLink = String.format(Locale.US, "http://mana.stmn1.com/sg/bu/mp3/B%d.mp3", mHymnNo);
                            break;
                        }

                    case HYMN_BANZOU:
                        // proceed to use HYMN_BANZOU if no midi files available
                        if (HymnsApp.getFileResId(MIDI_BB + mHymnNo, "raw") != 0) {
                            uriList.add(HymnsApp.getRawUri(MIDI_BB + mHymnNo));
                            uriList.add(HymnsApp.getRawUri(MIDI_BBC + mHymnNo));
                            return uriList;
                        }

                        dir = mHymnType + MEDIA_BANZOU;
                        if (isFileExist(dir, mHymnNo, uriList)) break;

                        if (proceedDownLoad) {
                            fileName = "B" + fileName + ".mid";
                            // fbLink = String.format(Locale.US, "https://www.hymnal.net/cn/hymn/ts/%d/f=mid", mHymnNo);
                            // https://www.hymnal.net/Hymns/ChineseTS/midi/tunes/ts0014_tune.midi
                            fbLink = String.format(Locale.US, "https://www.hymnal.net/Hymns/ChineseTS/midi/tunes/ts%04d_tune.midi", mHymnNo);
                            break;
                        }
                }
                break;

            case HYMN_DB:
                switch (mediaType) {
                    case HYMN_MEDIA:
                        dir = mHymnType + MEDIA_MEDIA;
                        if (isFileExist(dir, mHymnNo, uriList)) break;

                    case HYMN_JIAOCHANG:
                        dir = mHymnType + MEDIA_JIAOCHANG;
                        if (isFileExist(dir, mHymnNo, uriList)) break;

                    case HYMN_CHANGSHI:
                        dir = mHymnType + MEDIA_CHANGSHI;
                        if (isFileExist(dir, mHymnNo, uriList)) break;

                        if (proceedDownLoad) {
                            fileName = "D" + fileName + ".mp3";
                            // http://g.cgbr.org/music/d/media/48m.mp3
                            // fbLink = String.format(Locale.US, "https://www.hymnal.net/cn/hymn/ch/%d/f=sing", mHymnNo);
                            fbLink = String.format(Locale.US, "http://mana.stmn1.com/sg/da/Dmp3/D%d.mp3", mHymnNo);
                            break;
                        }

                    case HYMN_BANZOU:
                        // proceed to use HYMN_BANZOU if no midi files available
                        if (HymnsApp.getFileResId(MIDI_DB + mHymnNo, "raw") != 0) {
                            uriList.add(HymnsApp.getRawUri(MIDI_DB + mHymnNo));
                            uriList.add(HymnsApp.getRawUri(MIDI_DBC + mHymnNo));
                            return uriList;
                        }

                        dir = mHymnType + MEDIA_BANZOU;
                        if (isFileExist(dir, mHymnNo, uriList)) break;

                        if (proceedDownLoad) {
                            fileName = "D" + fileName + ".mid";
                            fbLink = String.format(Locale.US, "https://www.hymnal.net/cn/hymn/ch/%d/f=mid", mHymnNo);
                            break;
                        }
                }
                break;
        }

        if (!TextUtils.isEmpty(fbLink) && !TextUtils.isEmpty(fileName)) {
            Timber.d("FileName = %s%s; fbLink = %s", dir, fileName, fbLink);
            mMediaDownloadHandler.initHttpFileDownload(fbLink, dir, fileName);
            return uriList;
        }
        return mMediaContentHandler.playIfVideo(uriList);
    }

    /**
     * Search local Hymn media directory for wildCard media file for exact match of hymnNo or prefix with #0.
     * init the local media file URI path for play back if any else return false
     *
     * @param dir the media local dir
     * @param hymnNo the hymn No
     * @param uriList the media URI list
     *
     * @return true if local media file is found else false
     */
    public static boolean isFileExist(String dir, int hymnNo, List<Uri> uriList) {
        File hymnDir = FileBackend.getHymnchtvStore(dir, true);
        if (hymnDir != null) {
            final Pattern pattern = Pattern.compile("[^1-9]" + hymnNo + "[^0-9]");
            File[] fileList = hymnDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return pattern.matcher(name).find();
                }
            });

            if (fileList != null && fileList.length != 0) {
                if (uriList != null) {
                    uriList.add(Uri.fromFile(fileList[0]));
                }
                Timber.d("Media file found: %s; %s", fileList.length, fileList[0]);
                return true;
            }
        }
        return false;
    }

    public static boolean isFileExist(MediaRecord mediaRecord) {
        if (mediaRecord != null) {
            String dir = mediaRecord.getHymnType() + MediaConfig.mediaDir.get(mediaRecord.getMediaType());
            return isFileExist(dir, mediaRecord.getHymnNo(), null);
        }
        return false;
    }

    /**
     * Get the local availability of the hymn media content for all mediaType
     *
     * @return array of media content availability for all mediaType
     */
    private boolean[] getHymnMediaState() {
        boolean[] isAvailable = {false, false, false, false};

        // Check to see if HYMN_MEDIA is available for the current selected HymnType/HymnNo
        boolean isFu = mHymnType.equals(HYMN_DB) && (mHymnNo > HYMN_DB_NO_MAX);

        switch (mHymnType) {
            case HYMN_ER:
            case HYMN_XB:
            case HYMN_XG:
            case HYMN_YB:
                break;

            case HYMN_BB:
                isAvailable[3] = HymnsApp.getFileResId(MIDI_BB + mHymnNo, "raw") != 0;
                break;

            case HYMN_DB:
                isAvailable[3] = HymnsApp.getFileResId(MIDI_DB + mHymnNo, "raw") != 0;
                break;
        }

        String dir;
        MediaType[] mediaTypes = MediaType.values();
        for (int i = 0; i < mediaTypes.length; i++) {
            MediaType mediaType = mediaTypes[i];
            MediaRecord mediaRecord = new MediaRecord(mHymnType, mHymnNo, isFu, mediaType);

            // Skip to next if state is already evaluated to true i.e. defined in DB media link
            if ((isAvailable[i] |= mDB.getMediaRecord(mediaRecord, false)))
                continue;

            switch (mediaType) {
                case HYMN_MEDIA:
                    dir = mHymnType + MEDIA_MEDIA;
                    isAvailable[0] = isFileExist(dir, mHymnNo, null);
                    break;

                case HYMN_JIAOCHANG:
                    dir = mHymnType + MEDIA_JIAOCHANG;
                    isAvailable[1] = isFileExist(dir, mHymnNo, null);
                    break;

                case HYMN_CHANGSHI:
                    dir = mHymnType + MEDIA_CHANGSHI;
                    isAvailable[2] = isFileExist(dir, mHymnNo, null);
                    break;

                case HYMN_BANZOU:
                    dir = mHymnType + MEDIA_BANZOU;
                    isAvailable[3] |= isFileExist(dir, mHymnNo, null);
                    break;
            }
        }
        return isAvailable;
    }

    /**
     * Generate the hymn fileName (remove all punctuation marks), and the lyricsPhrase
     * Currently use in  MP3 media fileName is: ? + hymnNo + hymnTitle + ".mp3"
     */
    private String getHymnTitle() {
        String pattern = "[，、‘’！：；。？]";
        String hymnTitle = getHymnInfo().split(":\\s|？|（")[1].replaceAll(pattern, "");
        // Strip off the hymn category prefix
        int idx = hymnTitle.lastIndexOf("－");
        if (idx != -1) {
            hymnTitle = hymnTitle.substring(idx + 1);
        }
        return hymnTitle;
    }

    /**
     * Get the hymn information for the media controller.
     * It always try to generate the possible lyricsPhrase for media download link
     *
     * @return the hymn info for display
     */
    public String getHymnInfo() {
        String fileName = "";
        String hymnTitle = "";
        String hymnInfo = "";
        Resources res = getResources();

        if (mHymnNo == HYMN_BB_DUMMY) {
            return String.format(Locale.CHINA, "英文 #%d: 这首英文诗歌没有匹配的中文歌词", mHymnNoEng);
        }

        switch (mHymnType) {
            case HYMN_ER:
                fileName = LYRICS_ER_DIR + "er" + mHymnNo + ".txt";
                break;

            case HYMN_XG:
                fileName = LYRICS_XG_DIR + "xg" + mHymnNo + ".txt";
                break;

            case HYMN_XB:
                fileName = LYRICS_XB_DIR + "xb" + mHymnNo + ".txt";
                break;

            case HYMN_YB:
                String hymnTN = ybXTable.get(mHymnNo);
                if (hymnTN != null) {
                    fileName = getHymnDir(hymnTN) + hymnTN + ".txt";
                }
                else {
                    fileName = LYRICS_YB_DIR + "yb" + mHymnNo + ".txt";
                }
                break;

            case HYMN_BB:
                fileName = LYRICS_BB_DIR + "bb" + mHymnNo + ".txt";
                break;

            case HYMN_DB:
                fileName = LYRICS_DB_DIR + "db" + mHymnNo + ".txt";
                break;
        }

        try {
            InputStream in2 = getResources().getAssets().open(fileName);
            byte[] buffer2 = new byte[in2.available()];
            if (in2.read(buffer2) == -1)
                return hymnInfo;

            String mResult = EncodingUtils.getString(buffer2, "utf-8");
            String[] mList = mResult.split("\r\n|\n");

            // fetch the hymn title with the category intact
            hymnTitle = mList[1];

            // Check the third line for additional info e.g.（诗篇二篇）（英1094）
            int idx = mList[2].indexOf("（");
            if (idx != -1) {
                hymnTitle = hymnTitle + mList[2].substring(idx);
            }

            // Do the best guess to find the first phrase from lyrics
            idx = 4;
            String tmp = "";
            while (tmp.length() < 6 && idx < mList.length) {
                tmp = mList[idx++];
            }

            lyricsPhrase = "";
            mList = tmp.split("[，、‘’！：；。？]");
            for (String s : mList) {
                if (lyricsPhrase.length() < 6) {
                    lyricsPhrase += s;
                }
            }
        } catch (IOException e) {
            Timber.w("Error getting info for hymn %s: %s", fileName, e.getMessage());
            hymnTitle += getString(R.string.error_file_not_found, fileName);
        }

        int resId = -1;
        switch (mHymnType) {
            case HYMN_ER:
                resId = R.string.hymn_title_mc_er;
                break;
            case HYMN_XB:
                resId = R.string.hymn_title_mc_xb;
                break;
            case HYMN_XG:
                resId = R.string.hymn_title_mc_xg;
                break;
            case HYMN_YB:
                resId = R.string.hymn_title_mc_yb;
                break;
            case HYMN_BB:
                resId = R.string.hymn_title_mc_bb;
                break;
            case HYMN_DB:
                resId = (mHymnNo > HYMN_DB_NO_MAX) ? R.string.hymn_title_mc_dbs : R.string.hymn_title_mc_db;
                break;
        }

        mHymnSearch = res.getString(resId, mHymnNo, lyricsPhrase);
        hymnInfo = res.getString(resId, mHymnNo, hymnTitle);
        return hymnInfo;
    }

    public Integer getHymnNoEng() {
        return mHymnNoEng;
    }

    public static String getHymnDir(String hymnTN) {
        if (hymnTN.startsWith("er")) {
            return LYRICS_ER_DIR;
        }
        if (hymnTN.startsWith("xb")) {
            return LYRICS_XB_DIR;
        }
        else if (hymnTN.startsWith("xg")) {
            return LYRICS_XG_DIR;
        }
        else if (hymnTN.startsWith("yb")) {
            return LYRICS_YB_DIR;
        }
        else {
            return hymnTN.startsWith("db") ? LYRICS_DB_DIR : LYRICS_BB_DIR;
        }
    }

    public void showNotionSite() {
        initWebView(UrlType.hymnNotionSearch, NotionRecord.getNotionSite(mHymnType, mHymnNo));
    }

    /**
     * Use android default browser for all web url access except for 'englishLyrics', avoid reload webPage for
     * englishLyrics if use access to the same english hymn no.
     * <p>
     * WebView UI is not user friendly, and offers limited share links for youtube.com/google.com string search.
     * i.e. The webView does not offer all the share app, and excluded hymnchtv for user selection.
     *
     * @param type UrlType enum type
     */
    public void initWebView(UrlType type, String... url) {
        switch (type) {
            case onlineHelp:
                mWebUrl = HYMNCHTV_FAQ_PLAYBACK;
                break;
            case englishLyrics:
                String HymnalLink = "https://www.hymnal.net/en/hymn/h/";
                mWebUrl = (mHymnNoEng == null) ? null : HymnalLink + mHymnNoEng;
                break;
            case hymnGoogleSearch:
                mWebUrl = (mHymnInfo == null) ? null : "https://www.google.com/search?q=" + mHymnSearch;
                break;
            case hymnYoutubeSearch:
                mWebUrl = (mHymnInfo == null) ? null : "https://m.youtube.com/results?search_query=" + mHymnSearch;
                break;
            case hymnNotionSearch:
                mWebUrl = ((url.length < 1) || (url[0] == null)) ? NotionRecord.HYMNCHTV_NOTION : url[0];
                break;
            case hymnQqSearch:
                mWebUrl = ((url.length < 1) || (url[0] == null)) ? QQRecord.HYMNCHTV_QQ_MAIN : url[0];
                break;
            default:
                mWebUrl = null;
        }

        // Timber.d("Web URL link: %s", mWebUrl);
        if (mWebUrl == null) {
            HymnsApp.showToastMessage(R.string.error_media_url_invalid, type.toString());
            return;
        }

        // Proceed to use android default browser if it is not englishLyrics access
        if (UrlType.englishLyrics != type) {
            About.hymnUrlAccess(this, mWebUrl);
            return;
        }

        // Not actually being used in current implementation
        WebViewFragment mWebFragment = (WebViewFragment) getSupportFragmentManager().findFragmentById(R.id.webView);
        if (mWebFragment == null) {
            mWebFragment = new WebViewFragment();
        }
        else {
            mWebFragment.initWebView();
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.webView, mWebFragment).commit();
        mWebView.setVisibility(View.VISIBLE);
    }

    /**
     * fetch the link for the current selected hymn corresponding English lyrics
     *
     * @return the webLink for the English lyrics
     */
    public String getWebUrl() {
        return mWebUrl;
    }

    /*
     * This method handles the display of PlayerGui when screen orientation is rotated
     * Override onConfigurationChanged() so that media playback is smooth when device is rotated
     */
    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ContentView contentView = (ContentView) mPagerAdapter.mFragments.get(mPager.getCurrentItem());
        if (contentView != null)
            contentView.setLyricsTextScale();

        if (HymnsApp.isPortrait) {
            showPlayerUi(!isMediaPlayerUi && isShowPlayerUi);
        }
        else {
            showPlayerUi(false);
        }
    }
}