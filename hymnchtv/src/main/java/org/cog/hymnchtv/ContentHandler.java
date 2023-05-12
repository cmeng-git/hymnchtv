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

import static org.cog.hymnchtv.ContentView.LYRICS_BBS_TEXT;
import static org.cog.hymnchtv.ContentView.LYRICS_BB_SCORE;
import static org.cog.hymnchtv.ContentView.LYRICS_DBS_TEXT;
import static org.cog.hymnchtv.ContentView.LYRICS_DB_SCORE;
import static org.cog.hymnchtv.ContentView.LYRICS_ER_SCORE;
import static org.cog.hymnchtv.ContentView.LYRICS_ER_TEXT;
import static org.cog.hymnchtv.ContentView.LYRICS_XB_SCORE;
import static org.cog.hymnchtv.ContentView.LYRICS_XB_TEXT;
import static org.cog.hymnchtv.HymnToc.hymnCategoryBb;
import static org.cog.hymnchtv.HymnToc.hymnCategoryDb;
import static org.cog.hymnchtv.HymnToc.hymnCategoryEr;
import static org.cog.hymnchtv.HymnToc.hymnCategoryXb;
import static org.cog.hymnchtv.MainActivity.ATTR_AUTO_PLAY;
import static org.cog.hymnchtv.MainActivity.ATTR_HYMN_NUMBER;
import static org.cog.hymnchtv.MainActivity.ATTR_HYMN_TYPE;
import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;
import static org.cog.hymnchtv.MainActivity.PREF_MENU_SHOW;
import static org.cog.hymnchtv.MainActivity.PREF_SETTINGS;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_TMAX;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.PopupWindow;

import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;

import org.apache.http.util.EncodingUtils;
import org.apache.http.util.TextUtils;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

/**
 * The class handles the actual content source address decoding for the user selected hymn
 *
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
    public static String MEDIA_MIDI = "/media_midi/";
    public static String MEDIA_JIAOCHANG = "/media_jiaochang/";
    public static String MEDIA_BANZOU = "/media_banzou/";
    public static String MEDIA_CHANGSHI = "/media_changshi/";

    public static final String MIDI_BB = "bm";
    public static final String MIDI_BBC = "bmc";

    public static final String MIDI_DB = "dm";
    public static final String MIDI_DBC = "dmc";

    public final DatabaseBackend mDB = DatabaseBackend.getInstance(HymnsApp.getGlobalContext());
    private MediaContentHandler mMediaContentHandler;

    public boolean isHFAvailable = false;
    private boolean isShowPlayerUi;

    // True if either youtube or exoPlayer is playing
    private boolean isMediaPlayerUi = false;

    // Hymn Type and number selected by user
    private boolean mAutoPlay = false;
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
        checkHFAvailability();

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
        mHymnType = bundle.getString(ATTR_HYMN_TYPE);
        mHymnNo = bundle.getInt(ATTR_HYMN_NUMBER);
        mHymnNoEng = HymnNoCh2EngXRef.hymnNoCh2EngConvert(mHymnType, mHymnNo);
        mAutoPlay = bundle.getBoolean(ATTR_AUTO_PLAY, false);

        switch (mHymnType) {
            // Convert the user input hymn number i.e: hymn #1 => #0 i.e.index number
            case HYMN_ER:
            case HYMN_XB:
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
        // FragmentStateAdapter default created 9, setOffscreenPageLimit has no effet
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

    /**
     * Check to see if heavenlyfood.cn site is accessible with 3s timeout;
     * Need to execute on new thread for network access
     */
    private void checkHFAvailability() {
        String host = "https://heavenlyfood.cn/";
        new Thread() {
            public void run() {
                try {
                    isHFAvailable = InetAddress.getByName(host).isReachable(3000);
                } catch (IOException e) {
                    Timber.w("URL Exception: %s", e.getMessage());
                }
            }
        }.start();
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
                mMediaContentHandler.releasePlayer();
                // Must do this only after mMediaContentHandler.releasePlayer()
                mMediaGuiController.initPlaybackSpeed();

                // Restore the default MediaGuiController UI
                getSupportFragmentManager().beginTransaction().replace(R.id.mediaPlayer, mMediaGuiController).commit();
                showPlayerUi(isShowPlayerUi && HymnsApp.isPortrait);

                isMediaPlayerUi = false;
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
                    HymnsApp.showToastMessage(R.string.gui_error_english_lyrics_null, mHymnNo);
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
            case HYMN_DB:
                resPrefix = LYRICS_DB_SCORE + "db" + mHymnNo;
                resFName = LYRICS_DBS_TEXT + "db" + mHymnNo;
                break;

            case HYMN_BB:
                resPrefix = LYRICS_BB_SCORE + "bb" + mHymnNo;
                resFName = LYRICS_BBS_TEXT + "bb" + mHymnNo;
                break;

            case HYMN_XB:
                resPrefix = LYRICS_XB_SCORE + "xb" + mHymnNo;
                resFName = LYRICS_XB_TEXT + "xb" + mHymnNo;
                break;

            case HYMN_ER:
                resPrefix = LYRICS_ER_SCORE + mHymnNo;
                resFName = LYRICS_ER_TEXT + "er" + mHymnNo;
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
            e.printStackTrace();
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
        mHymnNoEng = HymnNoCh2EngXRef.hymnNoCh2EngConvert(mHymnType, mHymnNo);

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

    // DB MP3 links non-standard naming conventions
    private static final Map<Integer, String> DB_Links = new HashMap<>();

    static {
        DB_Links.put(8, "D8父阿你是万灵之");
        DB_Links.put(13, "D13父阿在你并无动的影儿");
        DB_Links.put(49, "D49父阿我们在你面前拜");
        DB_Links.put(65, "D65耶稣大名");
        DB_Links.put(75, "D75永活的故事");
        DB_Links.put(90, "D90赞美不尽赞美赞美救主恩深");
        DB_Links.put(91, "D91祂不能救自己必须死髑髅地");
        DB_Links.put(113, "D113何等权柄耶稣的名");
        DB_Links.put(143, "D143最美的耶稣");
        DB_Links.put(147, "D147荣耀归与我主");
        DB_Links.put(178, "D178看这神圣筵席");
        DB_Links.put(183, "D183诅咒他受祝福我享");
        DB_Links.put(190, "D190赞他赞他赞美耶稣");
        DB_Links.put(192, "D192主惟有你");
        DB_Links.put(193, "D193但愿尊贵荣耀丰富");
        DB_Links.put(199, "D199荣耀的主");
        DB_Links.put(202, "D202在旷野加底斯");
        DB_Links.put(207, "D207神赐祂有能力");
        DB_Links.put(210, "D210主求你向我吹圣");
        DB_Links.put(212, "D212圣灵的大风");
        DB_Links.put(222, "D222有福的事实");
        DB_Links.put(223, "D223主我来就你");
        DB_Links.put(245, "D245路途遥远");
        DB_Links.put(247, "D247惊人恩典");
        DB_Links.put(251, "D251我是个罪人蒙主");
        DB_Links.put(277, "D277请进,哦请进");
        DB_Links.put(302, "D302主阿何等奥秘你灵在我灵");
        DB_Links.put(340, "D340完全地交出");
        DB_Links.put(346, "D346我今愿跟随救主");
        DB_Links.put(350, "D350谁是在主这边谁要跟随主");
        DB_Links.put(374, "D374基督是万有唯一的实际");
        DB_Links.put(388, "D388_1我已得到宇宙至宝");
        DB_Links.put(395, "D395唯有耶稣是我题目");
        DB_Links.put(402, "D402不是字句律法乃是生命主");
        DB_Links.put(413, "D413无别声音破此寂静");
        DB_Links.put(435, "D435在亚当里罪死是我所有");
        DB_Links.put(447, "D447何等奥妙父子灵乃是一神");
        DB_Links.put(467, "D467你怎能没有伤痕");
        DB_Links.put(470, "D470唯有常出代价愿背十字架");
        DB_Links.put(476, "D476活在生命光中不断与主交通");
        DB_Links.put(479, "D479要思想耶稣");
        DB_Links.put(485, "D485莫在世界仍留恋");
        DB_Links.put(491, "D491我必与你同在甜美的应许");
        DB_Links.put(494, "D494当你经过试炼茫然无所从");
        DB_Links.put(499, "D499非我所是");
        DB_Links.put(515, "D515不在此时许在将来");
        DB_Links.put(522, "D522长久陷入忧患苦痛");
        DB_Links.put(527, "D527迫得太紧");
        DB_Links.put(547, "D547不是挣扎努力乃是全归依");
        DB_Links.put(552, "D552祷告乃是灵中");
        DB_Links.put(561, "D561凭信心求");
        DB_Links.put(606, "D606救我脱");
        DB_Links.put(607, "D607主啊发言主啊吹气");
        DB_Links.put(614, "D614灵能交流恩主灵能交流");
        DB_Links.put(622, "D622弟兄和睦同居何等美善");
        DB_Links.put(635, "D635当你苦受撒旦试探");
        DB_Links.put(638, "D638我神乃是大能堡垒");
        DB_Links.put(639, "D639如果战争凶猛");
        DB_Links.put(641, "D641今日争战凶猛");
        DB_Links.put(649, "D649撒旦早已定规");
        DB_Links.put(656, "D656要在身体事奉工作");
        DB_Links.put(675, "D675昨日今日直到永远");
        DB_Links.put(677, "D677怜悯慈爱宽恕温柔又谦和");
        DB_Links.put(714, "D714此时当就耶稣耶稣在此");
        DB_Links.put(715, "D715你堕落罪恶境历尽了苦情");
        DB_Links.put(721, "D721贫穷软弱悲伤忧愁");
        DB_Links.put(723, "D723耶稣恩主是人唯一需要");
        DB_Links.put(726, "D726你的欢迎声音召我前来相信");
        DB_Links.put(730, "D730听阿罪人慈声");
        DB_Links.put(732, "D732你曾离天庭");
        DB_Links.put(737, "D737当我疲困罪恶境祂以柔爱来寻");
        DB_Links.put(751, "D751今天神的国度对我是操练");
        DB_Links.put(755, "D755基督快要再临日子已紧近");
        DB_Links.put(774, "D774在起初时候");
        DB_Links.put(782, "DF2阿利路阿利路亚");
    }

    // DB MP3 links non-standard naming conventions
    private static final Map<Integer, String> BB_Links = new HashMap<>();

    static {
        BB_Links.put(1, "B1当我们开口赞美");
    }

    // DB MP3 links non-standard naming conventions
    private static final Map<Integer, String> ER_Links = new HashMap<>();

    static {
        // ER_Links.put(108, "3主的爱/01.唱啊唱啊我们来唱歌(108)");
        ER_Links.put(601, "E601耶稣我们爱你");
    }

    /**
     * Array contains the max hymnNo max (i.e. start number of next category) for each category
     */
    public static final int[] category_db = new int[]{1, 6, 53, 194, 229, 269, 330, 356, 367, 441, 454, 458, 472,
            474, 490, 529, 548, 551, 579, 592, 624, 632, 650, 662, 670, 740, 745, 752, 768, 781, 787};

    public static final int[] category_bb = new int[]{1, 101, 201, 301, 401, 501, 601, 701, 801, 901, 1001, 1101};

    public static final int[] category_xb = new int[]{1, 40, 74, 110, 131, 143, 170};

    public static final int[] category_er = new int[]{1, 101, 201, 301, 401, 501, 601, 701, 801, 901, 1001, 1101, 1201, 1301};

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
     * Fetch the required playback media resources from local directory if available.
     * Otherwise, fetch from online sites with the predefined link;
     * else drop to next mediaType for playback
     *
     * @param mediaType media Type for the playback i.e. midi, BanZhou, JianChang or MP3
     * @param proceedDownLoad download from the specified dnLink if true;
     *
     * @return array of media resource to playback. Usually only one item, two for midi resources
     */
    public List<Uri> getPlayHymn(MediaType mediaType, boolean proceedDownLoad) {
        List<Uri> uriList = new ArrayList<>();
        String dnLink = "";
        String fbLink = "";
        String dir = "";
        String tmpName;

        /*
         * First priority: fetch the user defined media links/contents for the selected hymnType.
         * An media url link is played via streaming using YoutubePlayer,
         * or ExoPlayer without downloading the file, to save local storage space.
         *
         * An empty uriList is returned when it has been handled/played in the above process;
         * else media audio link or download link is returned. The media audio content can be
         * in mp3, mid, midi format. Media video content must not be included.
         *
         * Proceed to other media handlers if is not handled in getMediaUris i.e. not defined in DB
         */
        if (mMediaContentHandler.getMediaUris(mHymnType, mHymnNo, mediaType, uriList)) {
            return uriList;
        }

        String hymnTitle = getHymnTitle();
        String fileName = mHymnNo + hymnTitle + ".mp3";

        switch (mHymnType) {
            case HYMN_ER:
                switch (mediaType) {
                    case HYMN_MEDIA:
                        // drop down to next level

                    case HYMN_BANZOU:
                        dir = mHymnType + MEDIA_MIDI;
                        tmpName = "C" + mHymnNo + ".mid";
                        if (isExist(dir, tmpName, uriList)) {
                            return uriList;
                        }

                        // https://heavenlyfood.cn/hymns/music/er/C1.mp3
                        dir = mHymnType + MEDIA_BANZOU;
                        fileName = "C" + mHymnNo + ".mp3";
                        dnLink = "https://heavenlyfood.cn/hymns/music/er/" + fileName;
                        break;

                    case HYMN_JIAOCHANG:
                        dir = mHymnType + MEDIA_JIAOCHANG;
                        tmpName = mHymnNo + ".mp3";
                        if (isExist(dir, tmpName, uriList)) {
                            return uriList;
                        }

                    case HYMN_CHANGSHI:
                        dir = mHymnType + MEDIA_CHANGSHI;
                        fileName = "C" + fileName;
                        if (isExist(dir, fileName, uriList)) {
                            return uriList;
                        }

                        if (isHFAvailable) {
                            // https://heavenlyfood.cn/hymnal/CD专辑/儿童诗歌集/3主的爱/02.大山可以挪开(318).mp3 - currently no supported
                            // https://heavenlyfood.cn/hymnal/诗歌/儿童诗歌/06爱主/C603我爱我的主耶稣.mp3
                            String subLink = "";
                            for (int x = 0; x < category_er.length; x++) {
                                if (mHymnNo < category_er[x]) {
                                    subLink = String.format(Locale.CHINA, "%02d%s/", (x - 1), hymnCategoryEr[x - 1]);
                                    break;
                                }
                            }

                            // Generate the resName for link creation
                            String resName = ER_Links.get(mHymnNo);
                            if (resName == null) {
                                resName = fileName;
                            }
                            else {
                                resName = resName + ".mp3";
                            }
                            dnLink = "https://heavenlyfood.cn/hymnal/诗歌/儿童诗歌/" + subLink + resName;
                        }
                        else if (TextUtils.isEmpty(fbLink)) {
                            fbLink = String.format(Locale.US, "http://www.lightinnj.org/mp3/k-mp3/C%04d.mp3", mHymnNo);
                        }
                        break;
                }
                break;

            case HYMN_XB:
                switch (mediaType) {
                    case HYMN_MEDIA:
                        // drop down to next level

                    case HYMN_BANZOU:
                        dir = mHymnType + MEDIA_MIDI;
                        tmpName = "X" + mHymnNo + ".mid";
                        if (isExist(dir, tmpName, uriList)) {
                            return uriList;
                        }

                        dir = mHymnType + MEDIA_BANZOU;
                        tmpName = "X" + mHymnNo + ".mp3";
                        if (isExist(dir, tmpName, uriList)) {
                            return uriList;
                        }

                    case HYMN_JIAOCHANG:
                        dir = mHymnType + MEDIA_JIAOCHANG;
                        tmpName = mHymnNo + ".mp3";
                        if (isExist(dir, tmpName, uriList)) {
                            return uriList;
                        }

                    case HYMN_CHANGSHI:
                        dir = mHymnType + MEDIA_CHANGSHI;
                        fileName = "X" + fileName;

                        if (isHFAvailable) {
                            // https://heavenlyfood.cn/hymnal/诗歌/新歌颂咏/4召会生活110/X112神生命的种子.mp3
                            String subLink = "";
                            for (int x = 0; x < category_xb.length; x++) {
                                // dnlink for xB does not use the last hymn category for fetching
                                if (mHymnNo < category_xb[x]) {
                                    subLink = String.format(Locale.CHINA, "%d%s%03d/", x, hymnCategoryXb[x - 1],
                                            category_xb[x - 1]);
                                    break;
                                }
                            }
                            dnLink = "https://heavenlyfood.cn/hymnal/诗歌/新歌颂咏/" + subLink + fileName;
                        }
                        else if (TextUtils.isEmpty(fbLink)) {
                            fbLink = String.format(Locale.US, "http://g.cgbr.org/music/x/media/%03d.mp3", mHymnNo);
                        }
                        break;
                }
                break;

            case HYMN_BB:
                switch (mediaType) {
                    case HYMN_MEDIA:
                        // drop down to next level

                    case HYMN_BANZOU:
                        // proceed to use HYMN_BANZOU if no midi files available
                        if (HymnsApp.getFileResId(MIDI_BB + mHymnNo, "raw") != 0) {
                            uriList.add(HymnsApp.getRawUri(MIDI_BB + mHymnNo));
                            uriList.add(HymnsApp.getRawUri(MIDI_BBC + mHymnNo));
                            break;
                        }

                        // https://heavenlyfood.cn/hymns/music/bu/B15.mp3
                        dir = mHymnType + MEDIA_BANZOU;
                        fileName = "B" + mHymnNo + ".mp3";

                        if (isHFAvailable) {
                            dnLink = "https://heavenlyfood.cn/hymns/music/bu/" + fileName;
                        }
                        else if (TextUtils.isEmpty(fbLink)) {
                            fbLink = String.format(Locale.US, "https://www.hymnal.net/cn/hymn/ts/%d/f=mid", mHymnNo);
                        }
                        break;

                    case HYMN_JIAOCHANG:
                        dir = mHymnType + MEDIA_JIAOCHANG;
                        tmpName = mHymnNo + ".mp3";
                        if (isExist(dir, tmpName, uriList)) {
                            return uriList;
                        }

                    case HYMN_CHANGSHI:
                        // https://heavenlyfood.cn/hymnal/诗歌/补充本/00赞美的话/B1当我们开口赞美.mp3
                        // https://heavenlyfood.cn/hymnal/诗歌/补充本/00赞美的话/B37赞美荣耀王.mp3
                        // https://heavenlyfood.cn/hymnal/诗歌/补充本/01灵与生命/B123耶稣活在我里面.mp3
                        // https://heavenlyfood.cn/hymnal/诗歌/补充本/01灵与生命/B141神在基督耶稣里成那灵.mp3
                        // https://heavenlyfood.cn/hymnal/诗歌/补充本/05教会的异象/B501基督殿城与地.mp3
                        // https://heavenlyfood.cn/hymnal/诗歌/补充本/05教会的异象/B521来这美妙住处.mp3
                        dir = mHymnType + MEDIA_CHANGSHI;
                        fileName = "B" + fileName;

                        if (isHFAvailable) {
                            String subLink = "";
                            for (int x = 0; x < category_bb.length; x++) {
                                if (mHymnNo < category_bb[x]) {
                                    subLink = String.format(Locale.CHINA, "%02d%s/", (x - 1), hymnCategoryBb[x - 1]);
                                    break;
                                }
                            }
                            // Generate the resName for link creation
                            String resName = BB_Links.get(mHymnNo);
                            if (resName == null) {
                                resName = fileName;
                            }
                            else {
                                resName = resName + ".mp3";
                            }
                            dnLink = "https://heavenlyfood.cn/hymnal/诗歌/补充本/" + subLink + resName;
                        }
                        else if (TextUtils.isEmpty(fbLink)) {
                            fbLink = String.format(Locale.US, "https://www.hymnal.net/cn/hymn/ts/%d/f=sing", mHymnNo);
                        }
                        break;
                }
                break;

            case HYMN_DB:
                switch (mediaType) {
                    case HYMN_MEDIA:
                        // drop down to next level

                    case HYMN_BANZOU:
                        // proceed to use HYMN_BANZOU if no midi files available
                        if (HymnsApp.getFileResId(MIDI_DB + mHymnNo, "raw") != 0) {
                            uriList.add(HymnsApp.getRawUri(MIDI_DB + mHymnNo));
                            uriList.add(HymnsApp.getRawUri(MIDI_DBC + mHymnNo));
                            break;
                        }

                        // https://heavenlyfood.cn/hymns/music/da/D45.mp3
                        // https://heavenlyfood.cn/hymns/music/da/D781.mp3
                        dir = mHymnType + MEDIA_BANZOU;
                        fileName = "D" + mHymnNo + ".mp3";

                        if (isHFAvailable) {
                            dnLink = "https://heavenlyfood.cn/hymns/music/da/" + fileName;
                        }
                        else if (TextUtils.isEmpty(fbLink)) {
                            fbLink = String.format(Locale.US, "https://www.hymnal.net/cn/hymn/ch/%d/f=mid", mHymnNo);
                        }
                        break;

                    case HYMN_JIAOCHANG:
                        // https://heavenlyfood.cn/hymns/jiaochang/da/781.mp3;
                        // https://heavenlyfood.cn/hymnal/诗歌/大本诗歌/02敬拜父006/D45父神我们称颂你.mp3
                        // https://heavenlyfood.cn/hymnal/诗歌/大本诗歌/09经历基督367/D422主我还有谁在天上.mp3
                        // https://heavenlyfood.cn/hymnal/诗歌/大本诗歌/30附/DF1颂赞与尊贵与荣耀归你.mp3
                        dir = mHymnType + MEDIA_JIAOCHANG;
                        fileName = mHymnNo + ".mp3";

                        dnLink = "https://heavenlyfood.cn/hymns/jiaochang/da/" + fileName;
                        break;

                    case HYMN_CHANGSHI:
                        dir = mHymnType + MEDIA_CHANGSHI;
                        fileName = "D" + fileName;

                        if (isHFAvailable) {
                            String subLink = "";
                            for (int x = 0; x < category_db.length; x++) {
                                if (mHymnNo < category_db[x]) {
                                    if (mHymnNo > HYMN_DB_NO_MAX) {
                                        subLink = String.format(Locale.CHINA, "%02d%s/", x, hymnCategoryDb[x - 1]);
                                    }
                                    else {
                                        subLink = String.format(Locale.CHINA, "%02d%s%03d/", x, hymnCategoryDb[x - 1],
                                                category_db[x - 1]);
                                    }
                                    break;
                                }
                            }

                            // Generate the resName for link creation
                            String resName = DB_Links.get(mHymnNo);
                            if (resName == null) {
                                if (mHymnNo > HYMN_DB_NO_MAX) {
                                    resName = "DF" + (mHymnNo - HYMN_DB_NO_MAX) + lyricsPhrase;
                                }
                                else {
                                    resName = "D" + mHymnNo + lyricsPhrase;
                                }
                            }
                            dnLink = "https://heavenlyfood.cn/hymnal/诗歌/大本诗歌/" + subLink + resName + ".mp3";
                        }
                        else if (TextUtils.isEmpty(fbLink)) {
                            fbLink = String.format(Locale.US, "https://www.hymnal.net/cn/hymn/ch/%d/f=sing", mHymnNo);
                        }
                        break;
                }
                break;
        }

        if (!TextUtils.isEmpty(fileName)) {
            File mediaFile = new File(FileBackend.getHymnchtvStore(dir, true), fileName);
            if (mediaFile.exists()) {
                uriList.add(Uri.fromFile(mediaFile));
            }
            else if (!TextUtils.isEmpty(fbLink) && proceedDownLoad) {
                Timber.d("FileName = %s%s; fbLink = %s", dir, fileName, fbLink);
                mMediaDownloadHandler.initHttpFileDownload(fbLink, dir, fileName);
            }
            else if (!TextUtils.isEmpty(dnLink) && proceedDownLoad) {
                Timber.d("FileName = %s%s; dnLink = %s", dir, fileName, dnLink);
                mMediaDownloadHandler.initHttpFileDownload(dnLink, dir, fileName);
            }
        }
        return uriList;
    }

    /**
     * Fetch and init the local media file URI path for play back if any else return false
     *
     * @param dir the media local dir
     * @param fileName the media filename
     * @param uriList the media URI list
     *
     * @return true if local media file is found else false
     */
    private boolean isExist(String dir, String fileName, List<Uri> uriList) {
        File mediaFile = new File(FileBackend.getHymnchtvStore(dir, false), fileName);
        if (mediaFile.exists()) {
            uriList.add(Uri.fromFile(mediaFile));
            return true;
        }
        return false;
    }

    private boolean isExist(String dir, String fileName) {
        File mediaFile = new File(FileBackend.getHymnchtvStore(dir, false), fileName);
        return mediaFile.exists();
    }

    /**
     * Get the local availability of the hymn media content for all mediaType
     *
     * @return array of media content availability for all mediaType
     */
    private boolean[] getHymnMediaState() {
        String dir;
        String hymnTitle = getHymnTitle();
        boolean[] isAvailable = {false, false, false, false};

        // Check to see if HYMN_MEDIA is available for the current selected HymnType/HymnNo
        boolean isFu = mHymnType.equals(HYMN_DB) && (mHymnNo > HYMN_DB_NO_MAX);

        // Get the prefix of the hymn filename
        String tmpName = "";
        switch (mHymnType) {
            case HYMN_ER:
                tmpName = "C" + mHymnNo;
                break;
            case HYMN_XB:
                tmpName = "X" + mHymnNo;
                break;
            case HYMN_BB:
                isAvailable[1] = HymnsApp.getFileResId(MIDI_BB + mHymnNo, "raw") != 0;
                tmpName = "B" + mHymnNo;
                break;
            case HYMN_DB:
                isAvailable[1] = HymnsApp.getFileResId(MIDI_DB + mHymnNo, "raw") != 0;
                tmpName = "D" + mHymnNo;
                break;
        }

        // iterate only the first 4 values of the mediaTypes as there is only four mediaType buttons
        MediaType[] mediaTypes = MediaType.values();
        for (int i = 0; i < mediaTypes.length - 1; i++) {
            MediaType mediaType = mediaTypes[i];
            MediaRecord mediaRecord = new MediaRecord(mHymnType, mHymnNo, isFu, mediaType);

            // Skip to next if state is already evaluated to true
            if ((isAvailable[i] |= mDB.getMediaRecord(mediaRecord, false)))
                continue;

            switch (mediaType) {
                case HYMN_MEDIA:
                    break;

                case HYMN_BANZOU:
                    dir = mHymnType + MEDIA_MIDI;
                    if (!(isAvailable[1] = isExist(dir, tmpName + ".mid"))) {
                        dir = mHymnType + MEDIA_BANZOU;
                        isAvailable[1] = isExist(dir, tmpName + ".mp3");
                    }
                    break;

                case HYMN_JIAOCHANG:
                    dir = mHymnType + MEDIA_JIAOCHANG;
                    isAvailable[2] = isExist(dir, tmpName + ".mp3");
                    break;

                case HYMN_CHANGSHI:
                    dir = mHymnType + MEDIA_CHANGSHI;
                    isAvailable[3] = isExist(dir, tmpName + hymnTitle + ".mp3");
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
        String hymnInfo = "";
        String hymnTitle = "";
        Resources res = getResources();

        switch (mHymnType) {
            case HYMN_DB:
                fileName = LYRICS_DBS_TEXT + "db" + mHymnNo + ".txt";
                break;

            case HYMN_BB:
                fileName = LYRICS_BBS_TEXT + "bb" + mHymnNo + ".txt";
                break;

            case HYMN_XB:
                fileName = LYRICS_XB_TEXT + "xb" + mHymnNo + ".txt";
                break;

            case HYMN_ER:
                fileName = LYRICS_ER_TEXT + "er" + mHymnNo + ".txt";
                break;
        }

        try {
            InputStream in2 = getResources().getAssets().open(fileName);
            byte[] buffer2 = new byte[in2.available()];
            if (in2.read(buffer2) == -1)
                return hymnInfo;

            String mResult = EncodingUtils.getString(buffer2, "utf-8");
            String[] mList = mResult.split("\r\n|\n");

            // fetch the hymn title with the category untouched
            hymnTitle = mList[1];

            // Check the third line for additional info e.g.（诗篇二篇）（英1094）
            int idx = mList[2].indexOf("（");
            if (idx != -1) {
                hymnTitle = hymnTitle + mList[2].substring(idx);
            }

            // Do the best guess to find the phrase for mp3 download @see getPlayHymn()
            idx = 4;
            String tmp = "";
            while (tmp.length() < 6) {
                tmp = mList[idx++];
            }

            lyricsPhrase = "";
            mList = tmp.split("[，、‘’！：；。？]");
            for (String s : mList) {
                if (lyricsPhrase.length() < 6) {
                    lyricsPhrase += s;
                }
            }
        } catch (Exception e) {
            Timber.w("Error getting info for hymn %s: %s", fileName, e.getMessage());
            hymnTitle = hymnTitle + HymnsApp.getResString(R.string.gui_error_file_not_found, fileName);
        }

        int resId = -1;
        switch (mHymnType) {
            case HYMN_ER:
                resId = R.string.hymn_title_mc_er;
                break;
            case HYMN_XB:
                resId = R.string.hymn_title_mc_xb;
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

    /**
     * Use android default browser for all web url access except for 'englishLyrics', avoid reload webPage for
     * englishLyrics if use access to the same english hymn no.
     *
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
            HymnsApp.showToastMessage(R.string.gui_error_media_url_invalid, type.toString());
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