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
import org.apache.http.util.TextUtils;
import org.cog.hymnchtv.persistance.FileBackend;
import org.cog.hymnchtv.utils.*;

import java.io.File;
import java.io.InputStream;
import java.util.*;

import timber.log.Timber;

import static org.cog.hymnchtv.ContentView.LYRICS_BBS_TEXT;
import static org.cog.hymnchtv.ContentView.LYRICS_DBS_TEXT;
import static org.cog.hymnchtv.ContentView.LYRICS_ER_TEXT;
import static org.cog.hymnchtv.ContentView.LYRICS_XB_TEXT;
import static org.cog.hymnchtv.MainActivity.ATTR_NUMBER;
import static org.cog.hymnchtv.MainActivity.ATTR_PAGE;
import static org.cog.hymnchtv.MainActivity.ATTR_SELECT;
import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB_NO_MAX;
import static org.cog.hymnchtv.MainActivity.HYMN_DB_NO_TMAX;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;
import static org.cog.hymnchtv.MainActivity.PREF_MENU_SHOW;
import static org.cog.hymnchtv.MainActivity.PREF_SETTINGS;
import static org.cog.hymnchtv.MainActivity.TOC_BB;
import static org.cog.hymnchtv.MainActivity.TOC_DB;
import static org.cog.hymnchtv.MainActivity.TOC_ER;
import static org.cog.hymnchtv.MainActivity.TOC_XB;

/**
 * The class handles the actual content source address decoding for the user selected hymn
 *
 * @author Eng Chong Meng
 */
@SuppressLint("NonConstantResourceId")
public class ContentHandler extends FragmentActivity implements ViewPager.OnPageChangeListener
{
    // sub-directory for various media type
    public static String MEDIA_MIDI = "/media_midi/";
    public static String MEDIA_JIAOCHANG = "/media_jiaochang/";
    public static String MEDIA_BANZOU = "/media_banzou/";
    public static String MEDIA_CHANGSHI = "/media_changshi/";

    public static final String PAGE_CONTENT = "content";
    public static final String PAGE_MAIN = "main";
    public static final String PAGE_SEARCH = "search";

    public static final String MIDI_ER = "em";
    public static final String MIDI_XB = "xm";

    public static final String MIDI_BB = "bm";
    public static final String MIDI_BBC = "bmc";

    public static final String MIDI_DB = "dm";
    public static final String MIDI_DBC = "dmc";

    private static final String FILE_XFER_FRAGMENT = "file_xfer_fragment";

    public boolean isShowMenu;

    // Hymn number selected by user
    private int hymnNo;
    private int hymnIdx = -1;

    private boolean mPlayMode = true;

    private String mPage = PAGE_CONTENT;
    private String mSelect;

    public PopupWindow pop;

    public SharedPreferences sPreference;

    /**
     * 大本诗歌 MP3 file naming is a mess, so attempt to use lyricsPhrase; may not match all the times
     */
    private String lyricsPhrase;

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

        // Attach the File Transfer GUI; Use single instance created in HymnApp;
        // do not create/add new, otherwise GUI display is no working properly
        mMediaHandler = HymnsApp.mMediaHandler;
        getSupportFragmentManager().beginTransaction().replace(R.id.filexferGui, mMediaHandler).commit();

        // Always start with UiPlayer hidden if in landscape mode
        sPreference = getSharedPreferences(PREF_SETTINGS, 0);
        isShowMenu = sPreference.getBoolean(PREF_MENU_SHOW, true) && HymnsApp.isPortrait;

        Bundle bundle = getIntent().getExtras();
        mSelect = bundle.getString(ATTR_SELECT);
        mPage = bundle.getString(ATTR_PAGE);
        hymnNo = bundle.getInt(ATTR_NUMBER);

        switch (mSelect) {
            // Convert the user input hymn number i.e: hymn #1 => #0 i.e.index number
            case HYMN_ER:
            case HYMN_XB:
            case HYMN_BB:
            case HYMN_DB:
                hymnIdx = HymnNo2IdxConvert.hymnNo2IdxConvert(mSelect, hymnNo);
                break;

            case TOC_ER:
            case TOC_XB:
            case TOC_BB:
            case TOC_DB:
                mPlayMode = false;
                break;
        }

        // The pager adapter, which provides the pages to the view pager widget.
        FragmentManager fragmentManager = getSupportFragmentManager();
        PagerAdapter mPagerAdapter = new MyPagerAdapter(fragmentManager, mSelect);

        ViewPager mPager = findViewById(R.id.viewPager);
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
        showPlayerUi(isShowMenu && !mSelect.startsWith("toc_"));
    }

    private void showPlayerUi(boolean show)
    {
        mMediaController.initPlayerUi(show, mPlayMode);
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

    /**
     * Start to play after file is download. Call from mediaHandler.
     */
    public void startPlay()
    {
        mMediaController.startPlay();
    }

    public void onError(String statusText)
    {
        mMediaController.playbackPlay.setImageResource(R.drawable.ic_play_stop);
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

    /**
     * Array contains the max hymnNo max (i.e. start number of next category) for each category
     */
    private static final int[] category_db = new int[]{1, 6, 53, 194, 229, 269, 330, 356, 367, 441, 454, 458, 472,
            474, 490, 529, 548, 551, 579, 592, 624, 632, 650, 662, 670, 740, 745, 752, 768, 781, 787, 800};

    private static final int[] category_bb = new int[]{1, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1100};

    private static final int[] category_xb = new int[]{1, 40, 74, 110, 131, 143, 164, 170};

    private static final int[] category_er = new int[]{1, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1100, 1200, 1300};;

    /**
     * For testing of the getPlayHymn algorithms for the specified media Type
     * and proceed to download if proceedDownload is true;
     */
    public void da_link_test(MediaType mediaType, boolean proceedDownLoad)
    {
        for (int hymnIdx = 1; hymnIdx <= HYMN_DB_NO_TMAX; hymnIdx++) {
            int[] hymnNoPage = HymnIdx2NoConvert.hymnIdx2NoConvert(mSelect, hymnIdx);
            hymnNo = hymnNoPage[0];
            if (hymnNo != -1) {
                getPlayHymn(mediaType, proceedDownLoad);
            }
        }
    }

    // 第112首 神生命的种子 http://g.cgbr.org/music/x/media/112x.mp3
    // http://g.cgbr.org/music/x/media/139.mp3

    /**
     * Fetch the required playback media resources from local directory if available.
     * Otherwise, fetch from online sites with predefined link;
     * else drop to next mediaType for playback
     *
     * @param mediaType media Type for playback i.e. midi, BanZhou, JianChang or MP3
     * @param proceedDownLoad download from the specified dnLink if true;
     * @return arrya of media resouce to placback. Usually only one item, two for midi resources
     */
    public List<Uri> getPlayHymn(MediaType mediaType, boolean proceedDownLoad)
    {
        List<Uri> uriList = new ArrayList<>();
        if (!mPlayMode) {
            return uriList;
        }

        String dnLink = "";
        String dir = "";
        String fileName;
        String hymnTitle;
        String tmpName;

        /*
         * Generate the hymn fileName (remove all punctuation marks) and the lyricsPhrase
         * Currently use in  MP3 media fileName is: ? + hymnNo + hymnTitle + ".mp3"
         *
         * But also required when drop down from other mediaType
         */
        String pattern = "[，、‘’！：；。？]";
        hymnTitle = getHymnInfo().split(":\\s|？|（")[1].replaceAll(pattern, "");
        int idx = hymnTitle.lastIndexOf("－");
        if (idx != -1) {
            hymnTitle = hymnTitle.substring(idx + 1);
        }
        fileName = hymnNo + hymnTitle + ".mp3";

        switch (mSelect) {
            case HYMN_ER:
                switch (mediaType) {
                    case HYMN_MIDI:
                        dir = mSelect + MEDIA_MIDI;
                        tmpName = "C" + hymnNo + ".mid";
                        if (isExist(dir, tmpName, uriList)) {
                            return uriList;
                        }

                    // https://heavenlyfood.cn/hymns/music/er/C1.mp3
                    case HYMN_BANZOU:
                        dir = mSelect + MEDIA_BANZOU;
                        fileName = "C" + hymnNo + ".mp3";
                        dnLink = "https://heavenlyfood.cn/hymns/music/er/" + fileName;
                        break;

                    case HYMN_JIAOCHANG:
                        dir = mSelect + MEDIA_JIAOCHANG;
                        tmpName = hymnNo + ".mp3";
                        if (isExist(dir, tmpName, uriList)) {
                            return uriList;
                        }

                    // https://heavenlyfood.cn/hymnal/诗歌/儿童诗歌/06爱主/C603我爱我的主耶稣.mp3
                    case HYMN_CHANGSHI:
                        dir = mSelect + MEDIA_CHANGSHI;
                        fileName = "C" + fileName;
                        if (isExist(dir, fileName, uriList)) {
                            return uriList;
                        }

                        String subLink = "";
                        for (int x = 0; x < category_er.length; x++) {
                            if (hymnNo < category_er[x]) {
                                String category = HymnsApp.getResStringByName("ermt" + x);
                                subLink = String.format(Locale.CHINA, "%02d%s/", (x - 1), category);
                                break;
                            }
                        }
                        dnLink = "https://heavenlyfood.cn/hymnal/诗歌/儿童诗歌/" + subLink + fileName;
                        break;
                }
                break;

            case HYMN_XB:
                switch (mediaType) {
                    case HYMN_MIDI:
                        dir = mSelect + MEDIA_MIDI;
                        tmpName = "X" + hymnNo + ".mid";
                        if (isExist(dir, tmpName, uriList)) {
                            return uriList;
                        }

                    case HYMN_BANZOU:
                        dir = mSelect + MEDIA_BANZOU;
                        tmpName = "X" + hymnNo + ".mp3";
                        if (isExist(dir, tmpName, uriList)) {
                            return uriList;
                        }

                    case HYMN_JIAOCHANG:
                        dir = mSelect + MEDIA_JIAOCHANG;
                        tmpName = hymnNo + ".mp3";
                        if (isExist(dir, tmpName, uriList)) {
                            return uriList;
                        }

                        // https://heavenlyfood.cn/hymnal/诗歌/新歌颂咏/4召会生活110/X112神生命的种子.mp3
                    case HYMN_CHANGSHI:
                        dir = mSelect + MEDIA_CHANGSHI;
                        fileName = "X" + fileName;

                        String subLink = "";
                        for (int x = 0; x < category_xb.length; x++) {
                            if (hymnNo < category_xb[x]) {
                                String category = HymnsApp.getResStringByName("xbmt" + x);
                                subLink = String.format(Locale.CHINA, "%d%s%03d/", x, category, category_xb[x - 1]);
                                break;
                            }
                        }
                        dnLink = "https://heavenlyfood.cn/hymnal/诗歌/新歌颂咏/" + subLink + fileName;
                        break;
                }
                break;

            case HYMN_BB:
                switch (mediaType) {
                    case HYMN_MIDI:
                        // Drop down to play HYMN_BANZOU if no midi files available
                        if (HymnsApp.getFileResId(MIDI_BB + hymnNo, "raw") != 0) {
                            uriList.add(HymnsApp.getRawUri(MIDI_BB + hymnNo));
                            uriList.add(HymnsApp.getRawUri(MIDI_BBC + hymnNo));
                            break;
                        }

                        // https://heavenlyfood.cn/hymns/music/bu/B15.mp3
                    case HYMN_BANZOU:
                        dir = mSelect + MEDIA_BANZOU;
                        fileName = "B" + hymnNo + ".mp3";
                        dnLink = "https://heavenlyfood.cn/hymns/music/bu/" + fileName;
                        break;

                    case HYMN_JIAOCHANG:
                        dir = mSelect + MEDIA_JIAOCHANG;
                        tmpName = hymnNo + ".mp3";
                        if (isExist(dir, tmpName, uriList)) {
                            return uriList;
                        }

                        // https://heavenlyfood.cn/hymnal/诗歌/补充本/00赞美的话/B37赞美荣耀王.mp3
                        // https://heavenlyfood.cn/hymnal/诗歌/补充本/01灵与生命/B123耶稣活在我里面.mp3
                        // https://heavenlyfood.cn/hymnal/诗歌/补充本/01灵与生命/B141神在基督耶稣里成那灵.mp3
                        // https://heavenlyfood.cn/hymnal/诗歌/补充本/05教会的异象/B501基督殿城与地.mp3
                        // https://heavenlyfood.cn/hymnal/诗歌/补充本/05教会的异象/B521来这美妙住处.mp3

                        // https://heavenlyfood.cn/hymnal/%E8%AF%97%E6%AD%8C/%E8%A1%A5%E5%85%85%E6%9C%AC/01%E7%81%B5%E4%B8%8E%E7%94%9F%E5%91%BD/B141%E7%A5%9E%E5%9C%A8%E5%9F%BA%E7%9D%A3%E8%80%B6%E7%A9%8C%E9%87%8C%E6%88%90%E9%82%A3%E7%81%B5.mp3
                        // https://heavenlyfood.cn/hymnal/%E8%AF%97%E6%AD%8C/%E8%A1%A5%E5%85%85%E6%9C%AC/01%E7%81%B5%E4%B8%8E%E7%94%9F%E5%91%BD/B141%E7%A5%9E%E5%9C%A8%E5%9F%BA%E7%9D%A3%E8%80%B6%E7%A8%A3%E9%87%8C%E6%88%90%E9%82%A3%E7%81%B5.mp3
                    case HYMN_CHANGSHI:
                        dir = mSelect + MEDIA_CHANGSHI;
                        fileName = "B" + fileName;

                        String subLink = "";
                        for (int x = 0; x < category_bb.length; x++) {
                            if (hymnNo < category_bb[x]) {
                                String category = HymnsApp.getResStringByName("bbmt" + x);
                                subLink = String.format(Locale.CHINA, "%02d%s/", (x - 1), category);
                                break;
                            }
                        }
                        dnLink = "https://heavenlyfood.cn/hymnal/诗歌/补充本/" + subLink + fileName;
                        break;
                }
                break;

            case HYMN_DB:
                switch (mediaType) {
                    case HYMN_MIDI:
                        // Drop down to play HYMN_BANZOU if no midi files available
                        if (HymnsApp.getFileResId(MIDI_DB + hymnNo, "raw") != 0) {
                            uriList.add(HymnsApp.getRawUri(MIDI_DB + hymnNo));
                            uriList.add(HymnsApp.getRawUri(MIDI_DBC + hymnNo));
                            break;
                        }

                        // https://heavenlyfood.cn/hymns/music/da/D45.mp3
                        // https://heavenlyfood.cn/hymns/music/da/D781.mp3
                    case HYMN_BANZOU:
                        dir = mSelect + MEDIA_BANZOU;
                        fileName = "D" + hymnNo + ".mp3";
                        dnLink = "https://heavenlyfood.cn/hymns/music/da/" + fileName;
                        break;

                    // https://heavenlyfood.cn/hymns/jiaochang/da/781.mp3;
                    case HYMN_JIAOCHANG:
                        dir = mSelect + MEDIA_JIAOCHANG;
                        fileName = hymnNo + ".mp3";
                        dnLink = "https://heavenlyfood.cn/hymns/jiaochang/da/" + fileName;
                        break;

                    // https://heavenlyfood.cn/hymnal/诗歌/大本诗歌/02敬拜父006/D45父神我们称颂你.mp3
                    // https://heavenlyfood.cn/hymnal/诗歌/大本诗歌/09经历基督367/D422主我还有谁在天上.mp3
                    // https://heavenlyfood.cn/hymnal/诗歌/大本诗歌/30附/DF1颂赞与尊贵与荣耀归你.mp3
                    case HYMN_CHANGSHI:
                        dir = mSelect + MEDIA_CHANGSHI;
                        fileName = "D" + fileName;

                        String subLink = "";
                        for (int x = 0; x < category_db.length; x++) {
                            if (hymnNo < category_db[x]) {
                                String category = HymnsApp.getResStringByName("dbmt" + x);
                                if (hymnNo > HYMN_DB_NO_MAX) {
                                    subLink = String.format(Locale.CHINA, "%02d%s/", x, category);
                                }
                                else {
                                    subLink = String.format(Locale.CHINA, "%02d%s%03d/", x, category, category_db[x - 1]);
                                }
                                break;
                            }
                        }

                        // Generate the resName for link creation
                        String resName = DB_Links.get(hymnNo);
                        if (resName == null) {
                            if (hymnNo > HYMN_DB_NO_MAX) {
                                resName = "DF" + (hymnNo - HYMN_DB_NO_MAX) + lyricsPhrase;
                            }
                            else {
                                resName = "D" + hymnNo + lyricsPhrase;
                            }
                        }
                        dnLink = "https://heavenlyfood.cn/hymnal/诗歌/大本诗歌/" + subLink + resName + ".mp3";
                        break;
                }
                break;
        }

        if (!TextUtils.isEmpty(fileName)) {
            Timber.d("FileName = %s%s; dnLink = %s", dir, fileName, dnLink);
            File mediaFile = new File(FileBackend.getHymnchtvStore(dir, true), fileName);
            if (mediaFile.exists()) {
                uriList.add(Uri.fromFile(mediaFile));
            }
            else if (!TextUtils.isEmpty(dnLink) && proceedDownLoad) {
                mMediaHandler.initHttpFileDownload(dnLink, dir, fileName);
            }
        }
        return uriList;
    }

    private boolean isExist(String dir, String fileName, List<Uri> uriList)
    {
        File mediaFile = new File(FileBackend.getHymnchtvStore(dir, false), fileName);
        if (mediaFile.exists()) {
            uriList.add(Uri.fromFile(mediaFile));
            return true;
        }
        return false;
    }

    public String getHymnInfo()
    {
        String fName = "";
        String hymnInfo = "";
        String hymnTitle = "";
        Resources res = getResources();

        switch (mSelect) {
            case HYMN_ER:
                fName = LYRICS_ER_TEXT + "er" + hymnNo + ".txt";
                break;

            case HYMN_XB:
                fName = LYRICS_XB_TEXT + "xb" + hymnNo + ".txt";
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
            String[] mList = mResult.split("\r\n|\n"); // applicable for "\r\n", remove the \r later on if any

            // fetch the song title
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
                if (lyricsPhrase.length() < 6)
                    lyricsPhrase += s;
            }

        } catch (Exception e) {
            Timber.w("Error getting info for hymn %s: %s", fName, e.getMessage());
            hymnTitle = hymnTitle + HymnsApp.getResString(R.string.gui_error_file_not_found, fName);
        }

        switch (mSelect) {
            case HYMN_ER:
                hymnInfo = res.getString(R.string.hymn_title_er, hymnNo, hymnTitle);
                break;
            case HYMN_XB:
                hymnInfo = res.getString(R.string.hymn_title_xb, hymnNo, hymnTitle);
                break;
            case HYMN_BB:
                hymnInfo = res.getString(R.string.hymn_title_bb, hymnNo, hymnTitle);
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
