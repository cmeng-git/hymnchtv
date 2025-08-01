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

import static org.cog.hymnchtv.HymnsApp.showToastMessage;
import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;
import org.cog.hymnchtv.BaseActivity;
import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.MediaType;
import org.cog.hymnchtv.R;
import org.cog.hymnchtv.persistance.DatabaseBackend;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

/**
 * The class provide handlers for the QQ JSONObject record scraping;
 * 【补充本诗歌】召会的生活(801一880)－诗歌链接：506 下载完成！
 *
 * @author Eng Chong Meng
 */
public class QQRecord extends MediaRecord {
    public static String HYMNCHTV_QQ_MAIN = "https://mp.weixin.qq.com/s/kgqBH0C_zgDaBnxbvC9wew";
    public static final String QQ = "QQ";
    // Map defines the QQ categories for HymnType links access; must have exact match
    // public static final List<String> qqHymnType = Arrays.asList("【大本诗歌D】", "【补充本诗歌B】", "【新歌颂咏X】", "【儿童诗歌C】");
    // Only 【补充本诗歌B】is available, others have been denied access
    public static final List<String> qqHymnType = Arrays.asList("【补充本诗歌B】", " ");

    // Map use to translate QQ_TITLE prefix to hymnType
    public static final Map<String, String> qqHymn2Type = new HashMap<>();

    static {
        qqHymn2Type.put("D", HYMN_DB);
        qqHymn2Type.put("B", HYMN_BB);
        qqHymn2Type.put("C", HYMN_ER);
        qqHymn2Type.put("X", HYMN_XB);
    }

    // QQ JSONObject key values for MediaRecord creation and saving
    public static final String QQ_TITLE = "title";
    public static final String QQ_URL = "url";

    private static int mFound = 0;
    private static int mSaved = 0;
    private static Context mContext;

    // Create a specific MediaRecord for web url fetch
    public QQRecord(String hymnType, int hymnNo) {
        super(hymnType, hymnNo, isFu(hymnType, hymnNo), MediaType.HYMN_JIAOCHANG, null, null);
    }

    /**
     * Start the links extractions at QQ main page on new thread; All network access must not be on UI thread.
     * Fetch links only for the hymnType specified in qqHymn2Type[].
     * Need to clean up the title before the next stage fetch
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
    public static void fetchQQLinks(final MediaConfig mediaConfig) {
        String mTitle = "诗歌（合辑）";
        mContext = mediaConfig;
        showToastMessage(R.string.nq_download_starting, QQ);

        final WebView webView = initWebView();
        getURLSource(webView, mTitle, HYMNCHTV_QQ_MAIN, data -> {
            try {
                JSONArray jsonArray = fetchJsonArray(mTitle, data);
                if (jsonArray != null && jsonArray.length() != 0) {
                    webView.destroy();
                    mFound = 0;
                    mSaved = 0;
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                        Timber.d("QQ HymnType (%s): %s", i, jsonObject);

                        // Proceed only for the hymnType specified in qqHymnType[]
                        String title = jsonObject.getString(QQ_TITLE);
                        if (qqHymnType.contains(title)) {
                            getQQHymnType(jsonObject);
                        }
                    }
                }
            } catch (JSONException e) {
                Timber.e("URL get source exception: %s", e.getMessage());
                showToastMessage(R.string.nq_download_failed, mTitle);
            }
        });
    }

    /**
     * Extract the hymnType Range and proceed all the hymn records in the range
     * Proceed to hymn records for【新歌颂咏】as it contains no range value
     * <p>
     * 【大本诗歌】1一100首
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
    private static void getQQHymnType(JSONObject jsonObj) throws JSONException {
        // Must strip off tailing alphabet before checking hymnRange
        String title = jsonObj.getString(QQ_TITLE).replaceAll("[DBCX]", "");
        String url = jsonObj.getString(QQ_URL);

        showToastMessage(R.string.nq_download_in_progress, title);
        //【新歌颂咏】does not have hymnRange; so proceed to saveQQRecord
        if (title.contains("新歌颂咏")) {
            saveQQRecord(jsonObj);
            return;
        }

        Pattern pattern = Pattern.compile(".+?(\\d+一\\d+)");
        final WebView webView = initWebView();
        getURLSource(webView, title, url, data -> {
            webView.destroy();
            try {
                JSONArray jsonArray = fetchJsonArray(title, data);
                if (jsonArray != null && jsonArray.length() != 0) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                        String hymnRange = jsonObject.getString(QQ_TITLE);
                        if (pattern.matcher(hymnRange).find()) {
                            Timber.d("QQ HymnType Range (%s): %s", i, jsonObject);
                            saveQQRecord(jsonObject);
                        }
                    }
                }
            } catch (JSONException e) {
                Timber.e("URL get source exception: %s", e.getMessage());
            }
        });
    }

    /**
     * Save all the QQ hymn links in the DB, if it passes the valid check in mDB.storeQQRecord()
     * <p>
     * D33父神阿你在羔羊里
     * D34荣耀归于父神
     * <p>
     * 附录 - 经历神
     * D785(附5)何大神迹！何深奥秘
     * D786(附6)神,你生命所施拯救
     * <p>
     * B23羔羊是配
     * C006朵朵小花含笑
     * X016小排聚会不可不去
     *
     * @param jsonObj: containing title as Prefix to get the valid Hymn Range links, and url the Notion required site url
     */
    private static void saveQQRecord(JSONObject jsonObj) throws JSONException {
        String title = jsonObj.getString(QQ_TITLE);
        String url = jsonObj.getString(QQ_URL);

        final WebView webView = initWebView();
        getURLSource(webView, title, url, data -> {
            try {
                JSONArray jsonArray = createJsonArray(title, data);
                if (jsonArray != null && jsonArray.length() != 0) {
                    webView.destroy();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                        storeQQJObject(jsonObject);
                    }
                    Timber.d(HymnsApp.getResString(R.string.nq_download_completed, title, mSaved, mFound));
                    showToastMessage(R.string.nq_download_completed, title, mSaved, mFound);
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
     * <p>
     * Valid QQ JSONObject:
     * {
     * title: 'D1但愿荣耀归于圣父',
     * item_show_type: '0',
     * url: 'http://mp.weixin.qq.com/s?__biz=MzUwOTc2ODcxNA==&amp;amp;mid=2247486824&amp;amp;idx=5&amp;amp;sn=97d137a5a4ddb1b0b778087ac84770b7
     * &amp;amp;chksm=f90c680dce7be11b4a3445af8dbe636b5a3d921ce910427609684bff2e5d95d0cda0696af419&amp;amp;scene=21#wechat_redirect',
     * subject_name: '诗歌操练学习',
     * link_type: 'LINK_TYPE_MP_APPMSG',
     * }
     */
    private static void storeQQJObject(JSONObject jsonRecord) {
        final DatabaseBackend mDB = DatabaseBackend.getInstance(HymnsApp.getGlobalContext());
        Pattern pattern = Pattern.compile("[DBCX](\\d+)");

        try {
            String title = jsonRecord.getString(QQ_TITLE);
            String hymnType = qqHymn2Type.get(title.substring(0, 1));
            if (TextUtils.isEmpty(hymnType)) {
                Timber.w("### Invalid QQ record HymnTitle: %s", jsonRecord);
                return;
            }

            Matcher matcher = pattern.matcher(title);
            int hymnNo;
            if (matcher.find()) {
                String noStr = matcher.group(1);
                if (TextUtils.isEmpty(noStr)) {
                    Timber.w("### Invalid QQ record HymnNo: %s", noStr);
                    return;
                }
                else {
                    hymnNo = Integer.parseInt(noStr);
                }

                QQRecord mRecord = new QQRecord(hymnType, hymnNo);
                mRecord.setMediaUri(jsonRecord.getString(QQRecord.QQ_URL));
                mFound++;
                long row = mDB.storeMediaRecord(mRecord);
                if (row < 0) {
                    Timber.e("### Error in creating QQ record for: %s", title);
                }
                else {
                    if ((mSaved % 10) == 0)
                        Timber.d("QQ Hymn Record saved (%s): %s", mSaved, jsonRecord);
                    mSaved++;
                }
            }
            else {
                Timber.w("### Invalid QQ record HymnTitle: %s", jsonRecord);
            }
        } catch (JSONException e) {
            Timber.e("### Error in creating QQ record with json exception: %s", e.getMessage());
        }
    }

    /**
     * Extra and phrase the htmlRaw info into JSONArray;
     *
     * @param htmlRaw the remote raw content containing the required link info
     *
     * @return JSON Array of the extracted info or null if none found
     */
    private static JSONArray createJsonArray(String title, String htmlRaw) {
        // <strong>B755跟随榜样</strong>
        // <strong><span style="font-size: 16px;">B759在复活里聚集</span></strong>
        Pattern pattern = Pattern.compile("<a target=\"_blank\" href=\"(.*?)\".*?data-itemshowtype=\"0\" tab=\"innerlink\".+?<strong.*?>(.+?)</strong>");
        if (TextUtils.isEmpty(htmlRaw))
            return null;

        JSONArray jsonArray = new JSONArray();
        htmlRaw = StringEscapeUtils.unescapeJava(htmlRaw).replaceAll("http:", "https:");
        Matcher matcher = pattern.matcher(htmlRaw);
        while (matcher.find()) {
            String mediaTitle = matcher.group(2);
            String mediaUrl = matcher.group(1);

            if (!TextUtils.isEmpty(mediaTitle) && !TextUtils.isEmpty(mediaUrl)) {
                JSONObject jsonBody = new JSONObject();
                try {
                    // Strip any <span> element in the mediaTitle
                    jsonBody.put(QQ_TITLE, mediaTitle.replaceAll("<span style=\".+?\">", ""));
                    jsonBody.put(QQ_URL, mediaUrl);
                    jsonArray.put(jsonBody);
                } catch (JSONException e) {
                    Timber.e("URL JsonArray Exception: %s", e.getMessage());
                }
            }
        }
        if (jsonArray.length() == 0) {
            Timber.d(HymnsApp.getResString(R.string.nq_download_failed, title));
        }

        return jsonArray;
    }

    /**
     * Extra and phrase the htmlRaw info into JSONArray;
     *
     * @param title the title of the url link
     * @param htmlRaw the remote raw content containing the required link info
     *
     * @return JSON Array of the extracted info or null if none found
     */
    private static JSONArray fetchJsonArray(String title, String htmlRaw) {
        // Standard enclosing pattern for link info used on the QQ sites
        Pattern pattern = Pattern.compile("var jumpInfo = (\\[.*?]);");
        try {
            if (TextUtils.isEmpty(htmlRaw))
                return null;

            Matcher matcher = pattern.matcher(htmlRaw);
            if (matcher.find()) {
                String strJson = matcher.group(1);
                if (!TextUtils.isEmpty(strJson)) {
                    return new JSONArray(toJsonString(strJson));
                }
            }
        } catch (JSONException e) {
            Timber.e("URL JsonArray Exception: %s", e.getMessage());
            showToastMessage(R.string.nq_download_failed, title);
        }
        return null;
    }

    /**
     * Cleanup all the stray info before JSONArray conversation:
     * i.e. \n, \s, ".html(false)", remove extra ',' in "'LINK_TYPE_MP_APPMSG',}" and comments;
     * android does not allow cleartextTraffic access; must force to secure link i.e. https
     *
     * @param jsonStr JSONArray in string format
     *
     * @return cleanup Json string
     */
    private static String toJsonString(String jsonStr) {
        return StringEscapeUtils.unescapeJava(jsonStr)
                .replaceAll("[\n|  ]", "")
                .replaceAll("\\.html\\(false\\)", "")
                .replaceAll(",\\}", "\\}")
                .replaceAll("䃼", "补")
                .replaceAll(",//.*?subject", ",subject")
                .replaceAll("http:", "https:");
    }

    @SuppressLint("SetJavaScriptEnabled")
    private static WebView initWebView() {
        WebView webView = new WebView(mContext);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        return webView;
    }

    public static void getURLSource(final WebView webView, String title, String urlToLoad, final ValueCallback<String> valueCallback) {
        // Timber.d("URl to load: %s", urlToLoad);
        webView.loadUrl(urlToLoad); // preload url and wait for 1.0 sec before checking.

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Timber.w("On Page Finished Call: %s: %s", webView.getProgress(), url);
                if (webView.getProgress() == 100) {
                    // Must give some time for js to populate the dynamic page content; else not working
                    BaseActivity.uiHandler.postDelayed(() -> {
                        try {
                            webView.evaluateJavascript("document.documentElement.outerHTML", data -> {
                                // webView.evaluateJavascript("document.documentElement.outerHTML", valueCallback);
                                if (data.contains("rich_media_content")) {
                                    valueCallback.onReceiveValue(data);
                                }
                                else {
                                    Timber.w("Web scrapping failed: %s", title);
                                    showToastMessage(R.string.nq_download_failed, title);
                                }
                            });
                        } catch (RuntimeException e) {
                            valueCallback.onReceiveValue(null);
                        }
                    }, 1000);
                }
            }
        });
    }
}
