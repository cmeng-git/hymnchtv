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

import android.content.Intent;
import android.os.Bundle;
import android.util.Range;
import android.view.KeyEvent;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

import androidx.fragment.app.FragmentActivity;

import org.apache.http.util.EncodingUtils;
import org.apache.http.util.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import timber.log.Timber;

import static org.cog.hymnchtv.ContentHandler.category_bb;
import static org.cog.hymnchtv.ContentHandler.category_db;
import static org.cog.hymnchtv.ContentHandler.category_er;
import static org.cog.hymnchtv.ContentHandler.category_xb;
import static org.cog.hymnchtv.ContentView.LYRICS_BBS_TEXT;
import static org.cog.hymnchtv.ContentView.LYRICS_DBS_TEXT;
import static org.cog.hymnchtv.ContentView.LYRICS_ER_TEXT;
import static org.cog.hymnchtv.ContentView.LYRICS_TOC;
import static org.cog.hymnchtv.ContentView.LYRICS_XB_TEXT;
import static org.cog.hymnchtv.MainActivity.ATTR_NUMBER;
import static org.cog.hymnchtv.MainActivity.ATTR_PAGE;
import static org.cog.hymnchtv.MainActivity.ATTR_SELECT;
import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_BB_NO_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_TMAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_ER_NO_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_XB_NO_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.rangeBbLimit;
import static org.cog.hymnchtv.utils.HymnNoValidate.rangeErLimit;

/**
 * HymnToc: Generate the hymn Toc for the user selected hymnType and Toc Type.
 * i.e. 诗歌类别, 笔画索引, and 拼音索引
 *
 * The result is displayed in Tree View structure allowing user to expand or collapse.
 * When user click on any hymn title the respective hymn contents are displayed.
 *
 * @author Eng Chong Meng
 */
public class HymnToc extends FragmentActivity
{
    /* 大本诗歌 db toc category */
    public static final String[] hymnCategoryDb
            = new String[]{"颂赞三一神", "敬拜父", "赞美主", "圣灵的丰满", "得救的证实与快乐", "羡慕", "奉献", "与基督的联合",
            "经历基督", "经历神", "十字架的夸耀", "十字架的道路", "复活的生命", "鼓励", "试炼中的安慰", "里面生命的各方面", "神医",
            "祷告", "读经", "召会", "聚会", "属灵的争战", "事奉", "传扬福音", "福音", "受浸", "国度", "荣耀的盼望", "终极的显出", "附"
    };

    /* 补充本 bb toc category */
    public static final String[] hymnCategoryBb
            = new String[]{"赞美的话", "灵与生命", "享受基督", "爱慕耶稣", "追求与长进", "教会的异象", "建造与合一", "教会的生活",
            "事奉与福音", "盼望与预备", "神的经纶"
    };

    /* 新歌颂咏 xb toc category */
    public static final String[] hymnCategoryXb
            = new String[]{"新路实行", "福音喜信", "生命与灵", "召会生活", "新耶路撒冷", "新诗歌"
    };

    /* 儿童诗歌 er toc category */
    public static final String[] hymnCategoryEr
            = new String[]{"神的创造", "主的爱", "圣灵的同在", "主的看顾", "赞美与喜乐", "祷告与读经", "爱主", "亲近倚靠主",
            "彰显主", "召会聚会", "传扬福音", "发光并争战", "经文故事篇"
    };

    // TocType for user selection
    public static final String TOC_TITLE = "目录";
    public static final String TOC_CATEGORY = "诗歌类别";
    public static final String TOC_STROKE = "笔画索引";
    public static final String TOC_PINYIN = "拼音索引";
    public static final String TOC_ENGLISH = "英中对照";

    public static List<String> hymnTocPage = new ArrayList<>();

    static {
        hymnTocPage.add(TOC_TITLE);
        hymnTocPage.add(TOC_CATEGORY);
        hymnTocPage.add(TOC_STROKE);
        hymnTocPage.add(TOC_PINYIN);
        hymnTocPage.add(TOC_ENGLISH);
    }

    // The TOC prefix for creating the correct toc text file name
    public static final String TOC_ER = "toc_er";
    public static final String TOC_XB = "toc_xb";
    public static final String TOC_BB = "toc_bb";
    public static final String TOC_DB = "toc_db";

    // The text files use to generate the various toc type for display and selection
    private static String STROKE_FILE = "_stroke.txt";
    private static String PINYIN_FILE = "_pinyin.txt";
    private static String ENGLISH_FILE = "_eng2ch.txt";

    // The treeView arrays for display
    private HashMap<String, List<String>> tocListDetail = new LinkedHashMap<>();

    private ExpandableListView expandableListView;
    private ExpandableListAdapter expandableListAdapter;
    private List<String> tocListCategory;

    /**
     * Generate the user selected hymn TOC type from the text files
     * Display the result in tree list view, for user select to shown hymn lyrics display
     *
     * @param savedInstanceState bundle
     */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        String hymnType = getIntent().getExtras().getString(ATTR_SELECT);
        if (TextUtils.isEmpty((hymnType)))
            return;
        String tocPage = getIntent().getExtras().getString(ATTR_PAGE);

        setContentView(R.layout.hymn_toc);
        expandableListView = findViewById(R.id.hymnToc);
        initHymnTocAdapter(hymnType, tocPage);
    }

    /**
     * Init the hymn TOC adapter i.e. Tree view structure
     *
     * @param hymnType the hymn type
     * @param tocPage the TOC type
     */
    private void initHymnTocAdapter(String hymnType, String tocPage)
    {
        tocListDetail = getHymnToc(hymnType, tocPage);
        tocListCategory = new ArrayList<>(tocListDetail.keySet());
        expandableListAdapter = new HymnTocExpandableListAdapter(this, tocListCategory, tocListDetail);
        expandableListView.setAdapter(expandableListAdapter);

//        expandableListView.setOnGroupExpandListener(groupPosition ->
//        HymnsApp.showToastMessage(tocListCategory.get(groupPosition) + " List Expanded."));
//
//        expandableListView.setOnGroupCollapseListener(groupPosition ->
//                HymnsApp.showToastMessage(tocListCategory.get(groupPosition) + " List Collapsed."));

        expandableListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            String hymnCategory = tocListCategory.get(groupPosition);
            String hymnTitle = tocListDetail.get(tocListCategory.get(groupPosition)).get(childPosition);

            if (!TextUtils.isEmpty(hymnTitle)) {
                onHymnTitleClick(hymnType, hymnTitle);
            }
            return true;
        });
    }


    /**
     * Show the lyrics based on the user picked hymnNo.
     *
     * @param hymnType the hymn type
     * @param hymnTitle the hymn title for extracting the hymn no for content display
     */
    private void onHymnTitleClick(String hymnType, String hymnTitle)
    {
        int hymnNo;

        int idx = hymnTitle.lastIndexOf("#");
        if (idx != -1) {
            hymnNo = Integer.parseInt(hymnTitle.substring(idx + 1));
        }
        else {
            hymnNo = Integer.parseInt(hymnTitle.split(":")[0]);
        }

        Intent intent = new Intent(this, ContentHandler.class);
        Bundle bundle = new Bundle();
        bundle.putString(ATTR_SELECT, hymnType);
        bundle.putInt(ATTR_NUMBER, hymnNo);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    /**
     * Routine to generate all the various TOC type i.e. tocPage
     * The title will indicate the select hymn type: toc
     *
     * TocCategory: is generated from the various hymn lyrics files
     * TocStroke / TocPinyin: generate based on the respective toc text file
     *
     * @param hymnType the hymn type
     * @param tocPage the toc page
     * @return HashMap to build the tree view
     */
    private HashMap<String, List<String>> getHymnToc(String hymnType, String tocPage)
    {
        int hymnNo;
        String fname;
        Range<Integer> rangeToc;

        switch (hymnType) {
            // 大本詩歌 in LYRICS_DBS_TEXT
            case HYMN_DB:
                setTitle(getString(R.string.hymn_title_db) + "：" + tocPage);

                switch (tocPage) {
                    case TOC_STROKE:
                        fname = LYRICS_TOC + TOC_DB + STROKE_FILE;
                        getHymnTocType(fname);
                        break;

                    case TOC_PINYIN:
                        fname = LYRICS_TOC + TOC_DB + PINYIN_FILE;
                        getHymnTocType(fname);
                        break;

                    case TOC_ENGLISH:
                        fname = LYRICS_TOC + TOC_DB + ENGLISH_FILE;
                        getHymnTocType(fname);
                        break;

                    case TOC_CATEGORY:
                        hymnNo = 1;
                        for (int x = 0; x < category_db.length - 1; x++) {
                            List<String> tocItems = new ArrayList<>();
                            rangeToc = new Range<>(category_db[x], category_db[x + 1] - 1);

                            while (hymnNo <= HYMN_DB_NO_TMAX) {

                                if (rangeToc.contains(hymnNo)) {
                                    fname = LYRICS_DBS_TEXT + hymnNo + ".txt";

                                    String hymnTitle = getHymnTitle(hymnNo, fname);
                                    if (hymnNo > HYMN_DB_NO_MAX) {
                                        hymnTitle = hymnTitle.replace(": ", ": 附" + (hymnNo - HYMN_DB_NO_MAX) + "-");
                                    }
                                    tocItems.add(hymnTitle);
                                    hymnNo++;
                                }
                                else {
                                    break;
                                }
                            }
                            tocListDetail.put(hymnCategoryDb[x], tocItems);
                        }
                        break;
                }
                break;

            // 補充本詩歌 in LYRICS_BBS_TEXT
            case HYMN_BB:
                setTitle(getString(R.string.hymn_title_bb) + "：" + tocPage);

                switch (tocPage) {
                    case TOC_STROKE:
                        fname = LYRICS_TOC + TOC_BB + STROKE_FILE;
                        getHymnTocType(fname);
                        break;

                    case TOC_PINYIN:
                        fname = LYRICS_TOC + TOC_BB + PINYIN_FILE;
                        getHymnTocType(fname);
                        break;

                    case TOC_ENGLISH:
                        fname = LYRICS_TOC + TOC_BB + ENGLISH_FILE;
                        getHymnTocType(fname);
                        break;

                    case TOC_CATEGORY:
                        hymnNo = 1;
                        for (int x = 0; x < category_bb.length - 1; x++) {
                            List<String> tocItems = new ArrayList<>();
                            rangeToc = new Range<>(category_bb[x], category_bb[x + 1] - 1);

                            while (hymnNo <= HYMN_BB_NO_MAX) {
                                if (hymnNo == rangeBbLimit[x]) {
                                    hymnNo = 100 * (x + 1) + 1;
                                }

                                if (rangeToc.contains(hymnNo)) {
                                    fname = LYRICS_BBS_TEXT + hymnNo + ".txt";
                                    tocItems.add(getHymnTitle(hymnNo, fname));
                                    hymnNo++;
                                }
                                else {
                                    break;
                                }
                            }
                            tocListDetail.put(hymnCategoryBb[x], tocItems);
                        }
                        break;
                }
                break;

            // 新歌颂咏 in LYRICS_XB_TEXT
            case HYMN_XB:
                setTitle(getString(R.string.hymn_title_xb) + "：" + tocPage);

                switch (tocPage) {
                    case TOC_STROKE:
                        fname = LYRICS_TOC + TOC_XB + STROKE_FILE;
                        getHymnTocType(fname);
                        break;

                    case TOC_PINYIN:
                        fname = LYRICS_TOC + TOC_XB + PINYIN_FILE;
                        getHymnTocType(fname);
                        break;

                    case TOC_CATEGORY:
                        hymnNo = 1;
                        for (int x = 0; x < category_xb.length - 1; x++) {
                            List<String> tocItems = new ArrayList<>();
                            rangeToc = new Range<>(category_xb[x], category_xb[x + 1] - 1);

                            while (hymnNo <= HYMN_XB_NO_MAX) {

                                if (rangeToc.contains(hymnNo)) {
                                    fname = LYRICS_XB_TEXT + "xb" + hymnNo + ".txt";
                                    tocItems.add(getHymnTitle(hymnNo, fname));
                                    hymnNo++;
                                }
                                else {
                                    break;
                                }
                            }
                            tocListDetail.put(hymnCategoryXb[x], tocItems);
                        }
                        break;
                }
                break;

            // 儿童诗歌 in LYRICS_ER_TEXT
            case HYMN_ER:
                setTitle(getString(R.string.hymn_title_er) + "：" + tocPage);
                switch (tocPage) {
                    case TOC_STROKE:
                        fname = LYRICS_TOC + TOC_ER + STROKE_FILE;
                        getHymnTocType(fname);
                        break;

                    case TOC_PINYIN:
                        fname = LYRICS_TOC + TOC_ER + PINYIN_FILE;
                        getHymnTocType(fname);
                        break;

                    case TOC_CATEGORY:
                        hymnNo = 1;
                        for (int x = 0; x < category_er.length - 1; x++) {
                            List<String> tocItems = new ArrayList<>();
                            rangeToc = new Range<>(category_er[x], category_er[x + 1] - 1);

                            while (hymnNo <= HYMN_ER_NO_MAX) {
                                if (hymnNo == rangeErLimit[x]) {
                                    hymnNo = 100 * (x + 1) + 1;
                                }

                                if (rangeToc.contains(hymnNo)) {
                                    fname = LYRICS_ER_TEXT + "er" + hymnNo + ".txt";
                                    tocItems.add(getHymnTitle(hymnNo, fname));
                                    hymnNo++;
                                }
                                else {
                                    break;
                                }
                            }
                            tocListDetail.put(hymnCategoryEr[x], tocItems);
                        }
                        break;
                }
                break;
        }
        return tocListDetail;
    }

    // Tools to generate the index file by stroke
//    private static Map<String, String> stroke = new LinkedHashMap<>();
//    static {
//        stroke.put("一画", "一");
//        stroke.put("二画", "人十又");
//        stroke.put("三画", "三夕大已广小");
//        stroke.put("四画", "不与为互井今仍从勿历天引无日比父长云");
//        stroke.put("五画", "世主乐他以出加务北去古叩只四圣外失宁平必旧永生用由禾召立");
//        stroke.put("六画", "争交仰任众传充光全兴再冲创合后回因在多如字安当早有欢此死自至行那邪团向伊宇");
//        stroke.put("七画", "但住何你吩听吸吹完应弟快我时旷更来步每求没灵祂身近这进远阿把花陈还芦投抓");
//        stroke.put("八画", "事凭咒哎国坦夜奇宝屈建怜或所现空耶若贫转迫降非奔彼呼享取话拣朋知终");
//        stroke.put("九画", "亲保信前受变哪城复带战既昨是活盼看神绝美荡荣要重除相标思珍拯");
//        stroke.put("十画", "凉哦宴恩流爱真破紧莫被请诸谁赶速都颂高啊哦乘家陪涌借");
//        stroke.put("十一画", "基常得惊惟惨接救教深甜祭祷脱随领第清唯");
//        stroke.put("十二画", "喂喜曾最焚等联谦释遇答葡");
//        stroke.put("十三画", "意慈摸数新暗照福罪跟路献蓝");
//        stroke.put("十四画", "儆愿模稳需歌");
//        stroke.put("十五画", "撒靠飘黎踩箭");
//        stroke.put("十六画", "嘴赞禧");
//        stroke.put("十七画", "藉繁");
//    }
//
//    private String getStroke(String idxChar) {
//        for (String key : stroke.keySet()) {
//            if (stroke.get(key).contains(idxChar)) {
//                return key;
//            }
//        }
//        return "";
//    }

    /**
     * Generate the expandable TOC list from the given tocFile sorted by the stroke or pinyin
     *
     * @param tocFile the toc file to extract info from
     */
    private void getHymnTocType(String tocFile)
    {
        List<String> tocItems = new ArrayList<>();
        String tocCategory = "";
        StringBuilder indexString = new StringBuilder("（");

        try {
            InputStream in2 = getResources().getAssets().open(tocFile);
            byte[] buffer2 = new byte[in2.available()];
            if (in2.read(buffer2) == -1)
                return;

            String mResult = EncodingUtils.getString(buffer2, "utf-8");
            String[] mList = mResult.split("\r\n|\n");

            int ml = 0;
            while (ml < mList.length) {
                if (mList[ml].matches("^.+画$|[A-Z]|[0-9~]+")) {
                    tocCategory = mList[ml++];
                    tocItems = new ArrayList<>();
                    indexString = new StringBuilder("（");
                }

                while (ml < mList.length) {
                    if (mList[ml].startsWith("^ ")) {
                        String tmp = mList[ml].substring(2, 3);
                        // Timber.d("stroke ### %s: %s", getStroke(tmp), mList[ml]);
                        if (!indexString.toString().contains(tmp)) {
                            indexString.append(tmp);
                        }
                        tocItems.add(mList[ml++].substring(2));
                    }
                    else {
                        break;
                    }
                }

                // Do not add additional info in the title text or sort the toc for English cross-reference
                if (!tocFile.contains(ENGLISH_FILE)) {
                    tocCategory += indexString + "）";
                    Collections.sort(tocItems);
                }
                tocListDetail.put(tocCategory, tocItems);
            }
        } catch (IOException e) {
            Timber.w("Content toc not available: %s", e.getMessage());
            HymnsApp.showToastMessage(R.string.gui_in_development);
        }
    }

    /**
     * Search the content of the given file for the specified search string.
     * return result if found, else null
     *
     * @param fName The name of file to search
     * @return matching string if found, else null
     */
    private String getHymnTitle(int hymnNo, String fName)
    {
        String hymnTitle = "";

        // These two variables are used to generate "英中对照"
        // String engStr = "";
        // String engNoStr = "";

        try {
            InputStream in2 = getResources().getAssets().open(fName);
            byte[] buffer2 = new byte[in2.available()];
            if (in2.read(buffer2) == -1)
                return hymnTitle;

            String mResult = EncodingUtils.getString(buffer2, "utf-8");
            String[] mList = mResult.split("\r\n|\n");

            // fetch the hymn title with the category stripped off
            hymnTitle = mList[1];
            int idx = hymnTitle.lastIndexOf("－");
            if (idx != -1) {
                hymnTitle = hymnTitle.substring(idx + 1);
            }
            // engStr = hymnTitle; //"英中对照"

            // Check the third line for additional info e.g.（诗篇二篇）（英1094）
            idx = mList[2].indexOf("（");
            if (idx != -1) {
                hymnTitle = hymnTitle + mList[2].substring(idx);
            }
            hymnTitle = String.format(Locale.CHINA, "%04d: %s", hymnNo, hymnTitle);

            // This section is used to generate "英中对照"
//            {
//                idx = mList[2].lastIndexOf("（英");
//                if (idx != -1) {
//                    int idx2 = mList[2].lastIndexOf("）");
//                    engNoStr = mList[2].substring(idx + 2, idx2);
//                }
//
//                int engNo = (TextUtils.isEmpty(engNoStr)) ? 0 : Integer.parseInt(engNoStr.split("[，|,]")[0]);
//                // String engXRef = String.format(Locale.CHINA, "%s %s #%d", engNo, engStr, hymnNo);
//                Timber.d("English ### %04d: %s #%d", engNo, engStr, hymnNo);
//            }

        } catch (IOException e) {
            Timber.w("Content search error: %s", e.getMessage());
        }
        return hymnTitle;
    }

    /**
     * Trapped KEYCODE_BACK and return to the search result display screen.
     *
     * @param keyCode The keycode
     * @param event the key event
     * @return from its super if it is not KEYCODE_BACK
     */
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
