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
package org.cog.hymnchtv.mediaconfig;

import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_MAX;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.text.TextUtils;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;
import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.MediaType;
import org.cog.hymnchtv.R;
import org.cog.hymnchtv.persistance.DatabaseBackend;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

/**
 * The class provide handlers for the Notion record fetching;
 *
 * @author Eng Chong Meng
 */
public class NotionRecord extends MediaRecord {
    // Site partial blocked for access.
    // public static String NOTION_SITE = "https://plume-click-f56.notion.site";
    // public static String HYMNCHTV_NOTION = "https://plume-click-f56.notion.site/fb415473f9314610bbd6592ba647cdd4";
    public static String NOTION_SITE = "https://breakopen.notion.site";
    public static String HYMNCHTV_NOTION = "https://breakopen.notion.site/BREAK-OPEN-2c52323fe4f9455cb93595f5dae95bf7";
    public static final String NOTION = "Notion";
    //  Map defines the Notion categories for HymnType links access; must have exact match
    public static final List<String> nqHymnType = Arrays.asList("大本诗歌（mp3+教唱+谱词+赏析）", "补充本诗歌", "儿童诗歌", "新歌颂咏");

    public static final String HYMN_DB2 = "hymn_db2";

    // Map use to translate Notion TITLE prefix to hymnType
    public static final Map<String, String> nqHymn2Type = new HashMap<String, String>() {
        {
            put("D", HYMN_DB);  // D53哦主你是神的活道
            put("附", HYMN_DB); // 附1  颂赞与尊贵与荣耀
            put("B", HYMN_BB);
            put("X", HYMN_XB);
            put("C", HYMN_ER);
        }
    };

    /**
     * Current webView cannot executed so many link at once. Manually select one for exec.
     */
    public static Map<String, String> NotionSites = new HashMap<String, String>() {
        {
            put(HYMN_DB, "https://breakopen.notion.site/mp3-edf8337e94824147b45605c273350e34");
            put(HYMN_DB2, "https://breakopen.notion.site/mp3-edf8337e94824147b45605c273350e34");
            put(HYMN_BB, "https://breakopen.notion.site/588a3b12cb9a4ed999d5a4ced96bdc18");
            // put(HYMN_XG, "https://breakopen.notion.site/219c0f3b696c46cda5131c1507d7152d");
            put(HYMN_XB, "https://breakopen.notion.site/c499d9182f4848039fe77fb602aa551e");
            put(HYMN_ER, "https://breakopen.notion.site/E-T-fc5adc924ad04efeb7cd2f137f7cc2c1");
        }
    };

    // 大本诗歌 range url links. webView still cannot all at once; need to break into two sections for manual download.
    public static Map<String, String> NotionDBSites = new HashMap<String, String>() {{
        put("颂赞三一神(1-6首)", "https://breakopen.notion.site/1-6-71d1d4b3f1ba4708b0a0987a8146676a");
        put("敬拜父(7-52首)", "https://breakopen.notion.site/7-52-12777bbc1093427cb4ba0eced4f1bae4");
        put("赞美主(53-193首)", "https://breakopen.notion.site/53-193-77959ba31a104c84ab5c530085be8fed");
        put("圣灵的丰满(194-228首)", "https://breakopen.notion.site/194-228-54e3a783a98a4af2879bb91fdb3db968");
        put("得救的证实与快乐(229-268首)", "https://breakopen.notion.site/229-268-dc17dad48fcb46c68131f332065baead");
        put("羡慕(269-329首)", "https://breakopen.notion.site/269-329-8a015033187a44eeb0622b4d16d9783c");
        put("奉献(330-355首)", "https://breakopen.notion.site/330-355-b382b9804cd04588b9333cdc7ac1acd9");
        put("与基督的联合(356-366首)", "https://breakopen.notion.site/356-366-0d320646eabc4ed19cd441f5b22d065d");
        put("经历基督(367-440首)", "https://breakopen.notion.site/367-440-20515a7881d94f5095c259ac337bc655");
        put("经历神(441-453首)", "https://breakopen.notion.site/441-453-8550717f15ac4aa1bc14d3f5e3023561");
        put("十字架的夸耀(454-457首)", "https://breakopen.notion.site/454-457-ade2957c7edb4330b9ead6bf190d44b9");
        put("十字架的道路(458-471首)", "https://breakopen.notion.site/458-471-7ded7a1296e84ba9b282b122667e80a0");
        put("复活的生命(472-473首)", "https://breakopen.notion.site/472-473-e3a36bae50a64081beb2c2a6511b928c");
        put("鼓励(474-489首)", "https://breakopen.notion.site/474-489-5729d3e38028405e949c8ea2160df73d");
    }};

    // Break DB URL into smaller size for retrieval; else web access will fail.
    public static Map<String, String> NotionDBSites2 = new HashMap<String, String>() {{
        put("试炼中的安慰(490-528首)", "https://breakopen.notion.site/490-528-62577aa3ad2840eab50fdbe8b12e2403");
        put("里面生命的各方面(529-547首)", "https://breakopen.notion.site/529-547-b51bca3d1c134c4dacd180b085b6224c");
        put("神医(548-550首)", "https://breakopen.notion.site/548-550-a1cadff1735f49f4b03634b82ee04c49");
        put("祷告(551-578首)", "https://breakopen.notion.site/551-578-1c1d7e5bf9254b858eec4446cff131c3");
        put("读经(579-591首)", "https://breakopen.notion.site/579-591-6459deb692074e929c88fe34b3c01810");
        put("召会(592-623首)", "https://breakopen.notion.site/592-623-8903b1009d9544519740502368a6267c");
        put("聚会(624-631首)", "https://breakopen.notion.site/624-631-0c8eb00f32f840afa0a5bd3810558321");
        put("属灵的争战(632-649首)", "https://breakopen.notion.site/632-649-df3e48314ef043faa3d3c3e955bad7a0");
        put("事奉(650-661首)", "https://breakopen.notion.site/650-661-619435de8dae414aaad7484710fc7da7");
        put("传扬福音(662-669首)", "https://breakopen.notion.site/662-669-a2e81d2ee9f948e49e5eb9602532d263");
        put("福音(670-739首)", "https://breakopen.notion.site/670-739-df5e0fbf5fe948008b7b33a6d2d44f10");
        put("受浸(740-744首)", "https://breakopen.notion.site/740-744-5b4d5a9c1019417cb02b6b7c1f31bb5e");
        put("国度(745-751首)", "https://breakopen.notion.site/745-751-e3ea3aceaef14ad6850ba34e2728948b");
        put("荣耀盼望(752-767首)", "https://breakopen.notion.site/752-767-4f8b5cf05ed14e15ad91b77a7701569e");
        put("终极显出(768-780首)", "https://breakopen.notion.site/768-780-11cf36b7cc0a4107818cdacbbcb9cffa");
        put("附录 1-6", "https://breakopen.notion.site/1-6-c13b78934c104bf0a461b06170bbde3b");
    }};

    // 补充本诗歌 range url links.
    public static Map<String, String> NotionBBSites = new HashMap<String, String>() {{
        put("补充本1-37首(赞美的话)", "https://breakopen.notion.site/1-37-317b423d06d74886b099232c3c6e2e0e");
        put("补充本101-150首（灵与生命）", "https://breakopen.notion.site/101-150-0b6914285e7a43cfb36335d2dd70fdc4");
        put("补充本201-258首（享受基督）", "https://breakopen.notion.site/201-258-ef4ecee9818c4b9ebed67e7c73059c18");
        put("补充本301-349首（爱慕耶稣）", "https://breakopen.notion.site/301-349-a6aaf97ca5b643d3a290d76f2aac5aea");
        put("补充本401-470首（追求与长进）", "https://breakopen.notion.site/401-470-f3f4525c128946869d6d09989b00ec81");
        put("补充本501-543首（召会的异象）", "https://breakopen.notion.site/501-543-bef778d2efd74a2e8162884d094df2ff");
        put("补充本601-629首（建造与合一）", "https://breakopen.notion.site/601-629-76e2f871e38d4382bce51fdb122e9abb");
        put("补充本701-762首（召会的生活）", "https://breakopen.notion.site/701-762-d7a144b2cda7435da6402a801e58187f");
        put("补充本801-880首（事奉与福音）", "https://breakopen.notion.site/801-880-b121da5c6efd4c7fae82ede8d187e622");
        put("补充本901-930首（盼望与预备）", "https://breakopen.notion.site/901-930-b7ebcbb99d55413cb5eb24494aca1d4b");
        put("补充本1001-1005首（神的经纶）", "https://breakopen.notion.site/1001-1005-a19c2647522c4586a1b4ac251cee6823");
    }};

    // 新歌颂咏 range url links.
    public static Map<String, String> NotionXBSites = new HashMap<String, String>() {{
        put("新歌颂咏", "https://breakopen.notion.site/c499d9182f4848039fe77fb602aa551e");
    }};

    // 儿童诗歌 range url links.
    public static Map<String, String> NotionERSites = new HashMap<String, String>() {{
        put("神的创造(001一017)", "https://breakopen.notion.site/001-017-7258b21b74e64c3887ee9b2c3fa5f839");
        put("主的爱(101一124)", "https://breakopen.notion.site/101-124-2275d95e31c54d42b44e6cd33d584785");
        put("圣灵的同在(201一212)", "https://breakopen.notion.site/201-212-b258c624c66745539d2f4d35586d5db7");
        put("主的看顾(301一323)", "https://breakopen.notion.site/301-323-74cac6b4764145968be8b1536d54db2a");
        put("赞美与喜乐(401一445)", "https://breakopen.notion.site/401-445-0deb8f44f10b45e481d9913d4bbc0095");
        put("祷告与读经(501一524)", "https://breakopen.notion.site/501-524-cee5d6d38d034e11ac281a656d2dd9f9");
        put("爱主(601一621)", "https://breakopen.notion.site/601-621-7aa93947afbe4904a2f1735de5452d42");
        put("亲近倚靠主(701一719)", "https://breakopen.notion.site/701-719-181446609743481baacb0ee6eb313f73");
        put("彰显主(801一836)", "https://breakopen.notion.site/801-836-90168f76eb7c421cabe5845e993c7e36");
        put("召会与聚会901一920)", "https://breakopen.notion.site/901-920-18887d1acaee47678b3d14c7b76f4301");
        put("传扬福音(1001一1039)", "https://breakopen.notion.site/1001-1039-800acff9d4fa4d9590a3e4199280bb07");
        put("发光并争战(1101一1118)", "https://breakopen.notion.site/1101-1118-4eb23d1bf07d4056a4b154b546b7ee91");
        put("经文故事篇(1201一1232)", "https://breakopen.notion.site/1201-1232-9edf01acbfeb4b1087ee7278004ba5de");
    }};

    // Notion JSONObject key values for MediaRecord creation and saving
    public static final String NQ_TITLE = "title";
    public static final String NQ_URL = "url";

    // Allow 30 sec for each url session to complete; usually each section take <20 sec
    public static long waitTime = 45000;

    private static final Map<WebView, JSONObject> webList = new HashMap<>();

    private static int mCount = 0;
    private static Context mContext;

    private static boolean isOverWrite = false;

    // Create a specific MediaRecord for the web url fetch
    public NotionRecord(String hymnType, int hymnNo) {
        super(hymnType, hymnNo, isFu(hymnType, hymnNo), MediaType.HYMN_JIAOCHANG, null, null);
    }

    /**
     * Use prefetch links for retrieve Notion records.
     */
    public static void fetchNotionRecords(final MediaConfig mediaConfig) {
        mContext = mediaConfig;
        isOverWrite = mediaConfig.isOverWrite();
        long sessionNo = 0;
        webList.clear();

        Handler urlHandler = new Handler();
        for (String hymnType : NotionSites.keySet()) {
            urlHandler.postDelayed(() -> {
                Set<Map.Entry<String, String>> HymnSites = null;
                switch (hymnType) {
                    case HYMN_DB:
                        HymnSites = NotionDBSites.entrySet();
                        break;
                    case HYMN_DB2:
                        HymnSites = NotionDBSites2.entrySet();
                        break;
                    case HYMN_BB:
                        HymnSites = NotionBBSites.entrySet();
                        break;
                    case HYMN_XB:
                        HymnSites = NotionXBSites.entrySet();
                        break;
                    case HYMN_ER:
                        HymnSites = NotionERSites.entrySet();
                        break;
                }

                if (HymnSites != null) {
                    HymnsApp.showToastMessage(R.string.nq_download_in_progress, hymnType);
                    for (Map.Entry<String, String> hymnSite : HymnSites) {
                        String title = hymnSite.getKey();
                        String url = hymnSite.getValue();
                        Timber.d("Notion HymnType Range: %s", title);
                        try {
                            JSONObject jsonObj = new JSONObject()
                                    .put(NQ_TITLE, title)
                                    .put(NQ_URL, url);
                            saveNQRecord(jsonObj, null);
                        } catch (JSONException e) {
                            Timber.e("URL get source exception: %s", e.getMessage());
                        }
                    }
                }
            }, waitTime * sessionNo++);
        }

        // Check after 10 minutes to see if it has completed loading.
        new Handler().postDelayed(() -> {
            if (!webList.isEmpty()) {
                Timber.d("Clear the incomplete web sites: %s", webList.size());
                // Note10 complete all download in 5 min; Huawei 13.2 matePro in 3.5 min. Enough time given for 7.5 min?
                // So clear unfinished url fetch, just leave it run in background; just report the current end result
                for (WebView webSet : webList.keySet()) {
                    webSet.destroy();
                }
                webList.clear();
            }
            Timber.d(HymnsApp.getResString(R.string.nq_download_completed, NOTION, mCount));
            HymnsApp.showToastMessage(R.string.nq_download_completed, NOTION, mCount);
            mediaConfig.btnNQ.setEnabled(true);
            mediaConfig.btnNQ.setTextColor(Color.DKGRAY);
        }, waitTime * sessionNo * 2);
    }

    /**
     * Note: Currently this method is not working as WebView cannot handle too many requests.
     * Start the links extractions at Notion main page on new thread; All network access must not be on UI thread.
     * Fetch and process links only for the hymnType specified in nqHymn2Type[].
     * Need to clean up the title before the next stage fetch
     * <p>
     * 【大本诗歌D】
     * 【补充本诗歌B】
     * 【儿童诗歌C】
     * 【新歌颂咏Ⅹ】
     * 【青年诗歌Q】
     * 【新诗分享N】
     * 【特会成全诗歌】
     * 【擘饼专辑诗歌】
     * 【诗歌音频合辑】
     */
    public static void fetchNotionLinks(final MediaConfig mediaConfig) {
        mContext = mediaConfig;
        HymnsApp.showToastMessage(R.string.nq_download_starting, NOTION);

        final WebView webView = initWebView();
        getURLSource(webView, NOTION, HYMNCHTV_NOTION, data -> {
            try {
                JSONArray jsonArray = fetchJsonArray(data, true);
                if (jsonArray != null && jsonArray.length() != 0) {
                    webView.destroy();
                    mCount = 0;
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                        Timber.d("Notion HymnType (%s): %s", i, jsonObject);

                        // Proceed only for the hymnType specified in nqHymn2Type[]
                        String title = jsonObject.getString(NQ_TITLE);
                        if (nqHymnType.contains(title)) {
                            getNQHymnType(jsonObject);
                        }
                    }
                }
            } catch (JSONException e) {
                Timber.e("URL get source exception: %s", e.getMessage());
                HymnsApp.showToastMessage(R.string.nq_download_failed, NOTION);
            }
        });

        // Check after 10 minutes to see if it has completed loading.
        new Handler().postDelayed(() -> {
            if (!webList.isEmpty()) {
                Timber.d("Restart the incomplete web scraping sites: %s", webList.size());
                for (Map.Entry<WebView, JSONObject> webSet : webList.entrySet()) {
                    try {
                        saveNQRecord(webSet.getValue(), webSet.getKey());
                    } catch (JSONException e) {
                        Timber.e("JSONException in final state: %s", webSet.getValue());
                    }
                }
            }
            else {
                Timber.d(HymnsApp.getResString(R.string.nq_download_completed, NOTION, mCount));
                HymnsApp.showToastMessage(R.string.nq_download_completed, NOTION, mCount);
            }
        }, 300000);
    }

    /**
     * Extract the hymnType Range and proceed all the hymn records in the range
     * Proceed to hymn records for【新歌颂咏】as it contains no range value
     * 【大本诗歌】001一100首
     * 【大本诗歌】101一200首
     * 【大本诗歌】201一300首
     * 【大本诗歌】301一400首
     * 【大本诗歌】401一500首
     * 【大本诗歌】501一600首
     * 【大本诗歌】601一700首
     * 【大本诗歌】701一786+附6首
     * 【圣徒最喜爱的50首合并音频】
     * <p>
     * 追求与长进┈┈401一470首
     * 圣灵的同在┈201一212首
     *
     * @param jsonObj: containing title as Prefix to get the valid Hymn Range links,
     * and url the Notion required site url
     */
    private static void getNQHymnType(JSONObject jsonObj) throws JSONException {
        String title = jsonObj.getString(NQ_TITLE);
        String url = jsonObj.getString(NQ_URL);

        HymnsApp.showToastMessage(R.string.nq_download_in_progress, title);
        //【新歌颂咏】does not have hymnRange; so proceed to saveNQRecord
        if (title.contains("新歌颂咏")) {
            saveNQRecord(jsonObj, null);
            return;
        }

        final WebView webView = initWebView();
        getURLSource(webView, title, url, data -> {
            webView.destroy();
            try {
                JSONArray jsonArray = fetchJsonArray(data, false);
                if (jsonArray != null && jsonArray.length() != 0) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                        Timber.d("Notion HymnType Range (%s): %s", i, jsonObject);
                        String hymnRange = jsonObject.getString(NQ_TITLE);
                        saveNQRecord(jsonObject, null);
                    }
                }
            } catch (JSONException e) {
                Timber.e("URL get source exception: %s", e.getMessage());
            }
        });
    }

    /**
     * Save all the Notion hymn links in the DB, if it passes the valid check in mDB.storeNQRecord()
     * D33父神阿你在羔羊里
     * D34荣耀归于父神
     * <p>
     * 附录 - 经历神
     * (附1)颂赞与尊贵与荣耀归
     * (附5)何大神迹！何深奥秘
     * B23羔羊是配
     * C006朵朵小花含笑
     * X016小排聚会不可不去
     *
     * @param jsonObj: containing title as Prefix to get the valid Hymn Range links,
     * and url the Notion required site url
     * @param wView: Create new if pass in webView is null
     */
    private static void saveNQRecord(JSONObject jsonObj, final WebView wView) throws JSONException {
        String title = jsonObj.getString(NQ_TITLE);
        String url = jsonObj.getString(NQ_URL);

        final WebView webView = (wView == null) ? initWebView() : wView;
        if (wView == null) {
            webList.put(webView, jsonObj);
        }

        getURLSource(webView, title, url, data -> {
            try {
                JSONArray jsonArray = fetchJsonArray(data, false);
                if (jsonArray != null && jsonArray.length() != 0) {
                    webList.remove(webView);
                    webView.destroy();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                        storeNQJObject(jsonObject);
                    }
                    Timber.d(HymnsApp.getResString(R.string.nq_download_completed, title, mCount));
                    HymnsApp.showToastMessage(R.string.nq_download_completed, title, mCount);
                }
            } catch (JSONException e) {
                Timber.e("URL get source exception: %s", e.getMessage());
            }
        });
    }

    /**
     * Save the given JSONObject to the database table hymnType, if contains valid info, else abort.
     * The JSONObject 'title' info must resolve to have:
     * a. valid hymnType
     * b. valid hymnNo
     * c. isFu based on "(附1)颂赞与尊贵与荣耀归"
     */
    private static void storeNQJObject(JSONObject jsonRecord) {
        final DatabaseBackend mDB = DatabaseBackend.getInstance(HymnsApp.getGlobalContext());
        Pattern pattern = Pattern.compile("[DBCX](\\d+)");
        Pattern patternFu = Pattern.compile("附([1-9])");

        try {
            String title = jsonRecord.getString(NQ_TITLE);
            String hymnType = nqHymn2Type.get(title.substring(0, 1));

            if (TextUtils.isEmpty(hymnType)) {
                Timber.w("### Invalid Notion record HymnTitle: %s", jsonRecord);
                return;
            }

            int hymnNo = 0;
            Matcher matcher;
            if (title.contains("附")) {
                hymnNo = HYMN_DB_NO_MAX;
                matcher = patternFu.matcher(title);
            }
            else {
                matcher = pattern.matcher(title);
            }

            if (matcher.find()) {
                String noStr = matcher.group(1);
                if (TextUtils.isEmpty(noStr)) {
                    Timber.w("### Invalid Notion record HymnNo: %s", noStr);
                    return;
                }
                else {
                    hymnNo += Integer.parseInt(noStr);
                }

                NotionRecord mRecord = new NotionRecord(hymnType, hymnNo);
                mRecord.setMediaUri(jsonRecord.getString(NQ_URL));
                if (isOverWrite || !MediaConfig.hasMediaRecord(mRecord)) {
                    long row = mDB.storeMediaRecord(mRecord);
                    if (row < 0) {
                        Timber.e("### Error in creating Notion record for: %s", title);
                    }
                    else {
                        mCount++;
                        Timber.d("Notion Hymn Record saved (%s): %s", mCount, jsonRecord);
                    }
                }
            }
            else {
                Timber.w("### Invalid QQ record HymnTitle: %s", jsonRecord);
            }
        } catch (Exception e) {
            Timber.e("### Error in creating Notion record with json exception: %s", e.getMessage());
        }
    }

    /**
     * Extra and phrase all the links info into JSONArray;
     *
     * @param htmlRaw the remote raw content containing the required link info
     *
     * @return JSON Array of the extracted info or null if none found
     */
    private static JSONArray fetchJsonArray(String htmlRaw, boolean main) {
        JSONArray jsonArray = new JSONArray();
        String htmlSource = fromContent(htmlRaw);
        if (TextUtils.isEmpty(htmlSource))
            return null;

        Pattern pattern = Pattern.compile("<div class=\"notion-page-content\"(.+)</a></div></div>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(htmlSource);
        if (matcher.find()) {
            String dataIdContent = matcher.group(1);
            if (!TextUtils.isEmpty(dataIdContent)) {
                if (main) {
                    pattern = Pattern.compile("<a href=\"(/[-a-z0-9]+)\" style=\"cursor:pointer;.*?[0-9]+\"><span.*?>(.*?)</span></a>", Pattern.DOTALL);
                }
                else {
                    // For extracting title/HymnNo and url link; NQ is for bb i.e.
                    // <a href="/B876-eccabf42d6434a1892777fd82448e48e?pvs=25"
                    pattern = Pattern.compile("<a href=\"(/[DBCX]*[-A-Za-z0-9]+)\\?pvs=25\".*?<div class=\"notranslate.*?>(.*?)</div>.+?</div><div contenteditable", Pattern.DOTALL);
                }
                matcher = pattern.matcher(dataIdContent);
                while (matcher.find()) {
                    String strLink = matcher.group(1);
                    String strHymnTitle = matcher.group(2);
                    if (!TextUtils.isEmpty(strLink) && !TextUtils.isEmpty(strHymnTitle)) {
                        // Timber.d("%s => %s", strHymnTitle, strLink);
                        try {
                            JSONObject jObject = new JSONObject()
                                    .put(NQ_TITLE, strHymnTitle)
                                    .put(NQ_URL, strLink.startsWith("http") ? strLink : NOTION_SITE + strLink);
                            jsonArray.put(jObject);
                        } catch (JSONException e) {
                            Timber.w("Jason Exception: %s", e.getMessage());
                        }
                    }
                }
            }
        }
        return jsonArray;
    }

    private static String fromContent(String content) {
        final String htmlContent = StringEscapeUtils.unescapeJava(content)
                .trim()
                .replaceAll("  ", " ")
                .replaceAll("\\n", "")
                .replaceAll("\\\\\"", "\"");
        return htmlContent;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private static WebView initWebView() {
        WebView webView = new WebView(mContext);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        return webView;
    }

    public static void getURLSource(final WebView webView, String title, String urlToLoad,
            final ValueCallback<String> valueCallback) {
        // Timber.d("URl to load: %s", urlToLoad);
        webView.loadUrl(urlToLoad); // preload url and wait for 1.5 sec before checking.
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Timber.w("On Page Finished Call: %s: %s", webView.getProgress(), url);
                if (webView.getProgress() == 100) {
                    // Must give some time for js to populate the dynamic page content; else not working
                    new Handler().postDelayed(() -> {
                        try {
                            webView.evaluateJavascript("document.documentElement.outerHTML", data -> {
                                // webView.evaluateJavascript("document.documentElement.outerHTML", valueCallback);
                                if (data.contains("notion-page-content")) {
                                    valueCallback.onReceiveValue(data);
                                }
                                else {
                                    Timber.w("Web scrapping failed: %s", urlToLoad);
                                    HymnsApp.showToastMessage(R.string.nq_download_failed, NOTION_SITE);
                                }
                            });
                        } catch (RuntimeException e) {
                            valueCallback.onReceiveValue(null);
                        }
                    }, 1500);
                }
//                else {
//                    Timber.w("On Page Finished Call: %s: %s", webView.getProgress(), url);
//                }
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                Timber.e("### WebView Render Process Gone: %s", title);
                view.destroy();
                HymnsApp.showToastMessage(R.string.nq_download_failed, title);
                return true;
            }
        });
    }

    /**
     * Return the Notion Url link based on given hymnType; hymnNo not use currently.
     *
     * @param hymnType: current user selected hymn Type
     * @param hymnNo: current user selected hymn No
     *
     * @return the url link.
     */
    public static String getNotionSite(String hymnType, int hymnNo) {
        return NotionSites.get(hymnType);
    }
}