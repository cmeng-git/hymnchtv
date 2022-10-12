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

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.commons.text.StringEscapeUtils;
import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.MediaType;
import org.cog.hymnchtv.R;
import org.cog.hymnchtv.persistance.DatabaseBackend;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * The class provide handlers for the Notion record scraping;
 *
 * @author Eng Chong Meng
 */
public class NotionRecord extends MediaRecord
{
    public static String NOTION_SITE = "https://plume-click-f56.notion.site";
    public static String HYMNCHTV_NOTION = NOTION_SITE + "/fb415473f9314610bbd6592ba647cdd4";

    public static final String NOTION = "Notion";
    //  Map defines the Notion categories for HymnType links access; must have exact match
    public static final List<String> nqHymnType = Arrays.asList("大本诗歌", "补充本诗歌", "儿童诗歌", "新歌颂咏");

    // Map use to translate Notion TITLE prefix to hymnType
    public static final Map<String, String> nqHymn2Type = new HashMap<>();

    static {
        nqHymn2Type.put("D", HYMN_DB);  // D53哦主你是神的活道
        nqHymn2Type.put("(", HYMN_DB);  // (附1)颂赞与尊贵与荣耀归
        nqHymn2Type.put("B", HYMN_BB);
        nqHymn2Type.put("C", HYMN_ER);
        nqHymn2Type.put("X", HYMN_XB);
    }

    // Notion JSONObject key values for MediaRecord creation and saving
    public static final String NQ_TITLE = "title";
    public static final String NQ_URL = "url";

    private static final Map<WebView, JSONObject> webList = new HashMap<>();

    private static int mCount = 0;
    private static Context mContext;

    // Create a specific MediaRecord for the web url fetch
    public NotionRecord(String hymnType, int hymnNo)
    {
        super(hymnType, hymnNo, isFu(hymnType, hymnNo), MediaType.HYMN_JIAOCHANG, null, null);
    }

    /**
     * Start the links extractions at Notion main page on new thread; All network access must not be on UI thread.
     * Fetch links only for the hymnType specified in nqHymn2Type[].
     * Need to clean up the title before the next stage fetch
     *
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
    public static void fetchNotionLinks(final MediaConfig mediaConfig)
    {
        mContext = mediaConfig;
        HymnsApp.showToastMessage(R.string.gui_nq_download_starting, NOTION);

        final WebView webView = initWebView();
        getURLSource(webView, NOTION_SITE, HYMNCHTV_NOTION, data -> {
            try {
                JSONArray jsonArray = fetchJsonArray(data);
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
                HymnsApp.showToastMessage(R.string.gui_nq_download_failed, NOTION);
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
                Timber.d(HymnsApp.getResString(R.string.gui_nq_download_completed, NOTION, mCount));
                HymnsApp.showToastMessage(R.string.gui_nq_download_completed, NOTION, mCount);
            }
        }, 600000);
    }

    /**
     * Extract the hymnType Range and proceed all the hymn records in the range
     * Proceed to hymn records for【新歌颂咏】as it contains no range value
     *
     * 【大本诗歌】001一100首
     * 【大本诗歌】101一200首
     * 【大本诗歌】201一300首
     * 【大本诗歌】301一400首
     * 【大本诗歌】401一500首
     * 【大本诗歌】501一600首
     * 【大本诗歌】601一700首
     * 【大本诗歌】701一786+附6首
     * 【圣徒最喜爱的50首合并音频】
     *
     * 追求与长进┈┈401一470首
     * 圣灵的同在┈201一212首
     *
     * @param jsonObj: containing title as Prefix to get the valid Hymn Range links,
     * and url the Notion required site url
     */
    private static void getNQHymnType(JSONObject jsonObj) throws JSONException
    {
        String title = jsonObj.getString(NQ_TITLE);
        String url = jsonObj.getString(NQ_URL);

        HymnsApp.showToastMessage(R.string.gui_nq_download_in_progress, title);
        //【新歌颂咏】does not have hymnRange; so proceed to saveNQRecord
        if (title.contains("新歌颂咏")) {
            saveNQRecord(jsonObj, null);
            return;
        }

        final WebView webView = initWebView();
        getURLSource(webView, title, url, data -> {
            webView.destroy();
            try {
                JSONArray jsonArray = fetchJsonArray(data);
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
     *
     * D33父神阿你在羔羊里
     * D34荣耀归于父神
     *
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
    private static void saveNQRecord(JSONObject jsonObj, final WebView wView) throws JSONException
    {
        String title = jsonObj.getString(NQ_TITLE);
        String url = jsonObj.getString(NQ_URL);

        final WebView webView = (wView == null) ? initWebView() : wView;
        if (wView == null) {
            webList.put(webView, jsonObj);
        }

        getURLSource(webView, title, url, data -> {
            try {
                JSONArray jsonArray = fetchJsonArray(data);
                if (jsonArray != null && jsonArray.length() != 0) {
                    webList.remove(webView);
                    webView.destroy();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                        storeNQJObject(jsonObject);
                    }
                    Timber.d(HymnsApp.getResString(R.string.gui_nq_download_completed, title, mCount));
                    HymnsApp.showToastMessage(R.string.gui_nq_download_completed, title, mCount);
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
    private static void storeNQJObject(JSONObject jsonRecord)
    {
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

                NotionRecordScrape mRecord = new NotionRecordScrape(hymnType, hymnNo);
                mRecord.setMediaUri(jsonRecord.getString(NotionRecordScrape.NQ_URL));

                long row = mDB.storeMediaRecord(mRecord);
                if (row < 0) {
                    Timber.e("### Error in creating Notion record for: %s", title);
                }
                else {
                    mCount++;
                    Timber.d("Notion Hymn Record saved (%s): %s", mCount, jsonRecord);
                }
            }
            else {
                Timber.w("### Invalid QQ record HymnTitle: %s", jsonRecord);
            }
        } catch (JSONException e) {
            Timber.e("### Error in creating Notion record with json exception: %s", e.getMessage());
        }
    }

    /**
     * Extra and phrase all the links info into JSONArray;
     *
     * @param htmlRaw the remote raw content containing the required link info
     * @return JSON Array of the extracted info or null if none found
     */
    private static JSONArray fetchJsonArray(String htmlRaw)
    {
        JSONArray jsonArray = new JSONArray();
        String htmlSource = fromContent(htmlRaw);
        if (TextUtils.isEmpty(htmlSource))
            return null;

        Pattern pattern = Pattern.compile("<div class=\"notion-page-content\"(.+)</a></div></div>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(htmlSource);
        if (matcher.find()) {
            String dataIdContent = matcher.group(1);
            if (!TextUtils.isEmpty(dataIdContent)) {
                // For extracting title/HymnNo and url link; BQ is for xb i.e. "/X040-B840-Q012-a42843dc90984f638d90fc63343c641c"
                pattern = Pattern.compile("<a href=\"(/[DBCX]*[-a-zBQ0-9]+)\".*?<div class=\"notranslate.*?>(.*?)</div></div></div><div contenteditable",
                        Pattern.DOTALL);
                matcher = pattern.matcher(dataIdContent);
                while (matcher.find()) {
                    String strLink = matcher.group(1);
                    String strHymnTitle = matcher.group(2);
                    if (!TextUtils.isEmpty(strLink) && !TextUtils.isEmpty(strHymnTitle)) {
                        // Timber.d("%s => %s", strHymnTitle, strLink);
                        try {
                            JSONObject jobject = new JSONObject()
                                    .put(NQ_TITLE, strHymnTitle)
                                    .put(NQ_URL, NOTION_SITE + strLink);
                            jsonArray.put(jobject);
                        } catch (JSONException e) {
                            Timber.w("Jason Exception: %s", e.getMessage());
                        }
                    }
                }
            }
        }
        return jsonArray;
    }

    private static String fromContent(String content)
    {
        final String htmlContent = StringEscapeUtils.unescapeJava(content)
                .trim()
                .replaceAll("  ", " ")
                .replaceAll("\\n", "")
                .replaceAll("\\\\\"", "\"");
        return htmlContent;
    }

    private static WebView initWebView()
    {
        WebView webView = new WebView(mContext);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        // webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        return webView;
    }

    public static void getURLSource(final WebView webView, String title, String urlToLoad, final ValueCallback<String> valueCallback)
    {
        // Timber.d("URl to load: %s", urlToLoad);
        webView.loadUrl(urlToLoad);
        webView.setWebViewClient(new WebViewClient()
        {
            @Override
            public void onPageFinished(WebView view, String url)
            {
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
                                    HymnsApp.showToastMessage(R.string.gui_nq_download_failed, NOTION_SITE);
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
        });
    }
}
