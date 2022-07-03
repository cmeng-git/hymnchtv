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

import android.text.TextUtils;

import org.cog.hymnchtv.ContentHandler;
import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.MediaType;
import org.cog.hymnchtv.R;
import org.cog.hymnchtv.persistance.DatabaseBackend;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * The class provide handlers for the QQ JSONObject record scraping;
 *
 * @author Eng Chong Meng
 */
public class QQRecord extends MediaRecord
{
    public static final String QQ = "QQ";
     //  Map defines the QQ links for HymnType access
    public static final List<String> qqHymnType
            = Arrays.asList("【大本诗歌D】", "【䃼充本诗歌B】", "【儿童诗歌C】", "【新歌颂咏X】");

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
    private static int mCount = 0;

    // Create a specific MediaRecord for web url fetch
    public QQRecord(String hymnType, int hymnNo)
    {
        super(hymnType, hymnNo, isFu(hymnType, hymnNo), MediaType.HYMN_URL, null, null);
    }

    /**
     * Start the links extractions at QQ main page on new thread; All network access must not be on UI thread.
     * Fetch links only for the hymnType specified in qqHymn2Type[].
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
    public static void fetchQQLinks(final MediaConfig mediaConfig)
    {
        HymnsApp.showToastMessage(R.string.gui_nq_download_starting, QQ);
        new Thread()
        {
            public void run()
            {
                try {
                    JSONArray jsonArray = fetchJsonArray("诗歌（合辑）", ContentHandler.HYMNCHTV_QQ_MAIN);
                    if (jsonArray != null) {
                        mCount = 0;
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                            Timber.d("QQ HymnType (%s): %s", i, jsonObject);


                            // Proceed only for the hymnType specified in qqHymn2Type[]
                            String title = jsonObject.getString(QQ_TITLE);
                            if (qqHymnType.contains(title)) {
                                // For hymns range links prefix matching; remove the title alpha character and made "䃼" correction
                                String prefix = title.replaceAll("[DBCX]", "")
                                        .replace("䃼", "补");
                                getQQHymnType(prefix, jsonObject.getString(QQ_URL));
                            }
                        }
                    }
                } catch (JSONException e) {
                    Timber.e("URL get source exception: %s", e.getMessage());
                    HymnsApp.showToastMessage(R.string.gui_nq_download_failed, QQ);
                    return;
                }
                Timber.d(HymnsApp.getResString(R.string.gui_nq_download_completed, QQ, mCount));
                HymnsApp.showToastMessage(R.string.gui_nq_download_completed, QQ, mCount);
            }
        }.start();
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
     * @param prefix use as Prefix to get the valid Hymn Range links
     * @param url the QQ required site url
     */
    private static void getQQHymnType(String prefix, String url)
    {
        HymnsApp.showToastMessage(R.string.gui_nq_download_in_progress, prefix);
        //【新歌颂咏】does not have hymnRange; so proceed to saveQQRecord
        if (prefix.contains("新歌颂咏")) {
            saveQQRecord(prefix, url);
            return;
        }

        try {
            JSONArray jsonArray = fetchJsonArray(prefix, url);
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                    Timber.d("QQ HymnType Range (%s): %s", i, jsonObject);
                    String hymnRange = jsonObject.getString(QQ_TITLE);
                    if (hymnRange.startsWith(prefix)) {
                        saveQQRecord(jsonObject.getString(QQ_TITLE), jsonObject.getString(QQ_URL));
                    }
                }
            }
        } catch (JSONException e) {
            Timber.e("URL get source exception: %s", e.getMessage());
        }
    }

    /**
     * Save all the QQ hymn links in the DB, if it passes the valid check in mDB.storeQQRecord()
     *
     * D33父神阿你在羔羊里
     * D34荣耀归于父神
     *
     * 附录 - 经历神
     * D785(附5)何大神迹！何深奥秘
     * D786(附6)神,你生命所施拯救
     *
     * B23羔羊是配
     * C006朵朵小花含笑
     * X016小排聚会不可不去
     *
     * @param prefix the title of the url link
     * @param url the QQ required site url
     */
    private static void saveQQRecord(String prefix, String url)
    {
        try {
            JSONArray jsonArray = fetchJsonArray(prefix, url);
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                    // Timber.d("QQ Hymn Record (%s): %s", i, jsonObject);
                    storeQQJObject(jsonObject);
                }
            }
        } catch (JSONException e) {
            Timber.e("URL get source exception: %s", e.getMessage());
        }
    }

    /**
     * Save the given JSONObject to the database table hymnType, if contains valid info, else abort.
     * The JSONObject 'title' info must resolve to have:
     * a. valid hymnType
     * b. valid hymnNo
     *
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
    private static void storeQQJObject(JSONObject jsonRecord)
    {
        final DatabaseBackend mDB = DatabaseBackend.getInstance(HymnsApp.getGlobalContext());
        Pattern pattern = Pattern.compile("[DBCX](\\d+)");

        try {
            String title = jsonRecord.getString(QQ_TITLE);
            String hymnType = qqHymn2Type.get(title.substring(0, 1));
            if (TextUtils.isEmpty(hymnType)) {
                Timber.w("### Invalid QQ record HymnTitle: %s", title);
                return;
            }

            Matcher matcher = pattern.matcher(title);
            int hymnNo = -1;
            if (matcher.find()) {
                String noStr = matcher.group(1);
                if (TextUtils.isEmpty(noStr)) {
                    Timber.w("### Invalid QQ record HymnNo: %s", noStr);
                    return;
                }
                else {
                    hymnNo = Integer.parseInt(noStr);
                }
            }

            QQRecord mRecord = new QQRecord(hymnType, hymnNo);
            mRecord.setMediaUri(jsonRecord.getString(QQRecord.QQ_URL));

            long row = mDB.storeMediaRecord(mRecord);
            if (row < 0) {
                Timber.e("### Error in creating QQ record for: %s", title);
            } else {
                // Timber.d("### Saved QQ record: %s", mRecord);
                mCount++;
            }
        } catch (JSONException e) {
            Timber.e("### Error in creating QQ record with json exception: %s", e.getMessage());
        }
    }

    /**
     * Extra and phrase all the links info into JSONArray; must remove any comment text string i.e. // 后台给的数据被encode了两次
     * url: 'http://mp.weixin.qq.com/s?__biz=MzI0OTM2ODkyMA==&amp;amp;mid=2247492597&amp;amp;idx=1&amp;amp;sn=85ee7042de79cbe'.html(false).html(false), // 后台给的数据被encode了两次subject_name: '诗歌操练学唱'
     *
     * @param title the title of the url link
     * @param url the remote url containing the required link info
     * @return JSON Array of the extracted info or null if none found
     */
    private static JSONArray fetchJsonArray(String title, String url)
    {
        // Standard enclosing pattern for link info used on the QQ sites
        Pattern pattern = Pattern.compile("var jumpInfo = (\\[.*?]);");
        // android does not allow cleartextTraffic access; must force to secure link
        url = url.replace("http:", "https:");

        try {
            String urlSource = WebScraper.getURLSource(url);
            if (TextUtils.isEmpty(urlSource))
                return null;

            Matcher matcher = pattern.matcher(urlSource);
            if (matcher.find()) {
                String strJson = matcher.group(1);
                if (!TextUtils.isEmpty(strJson)) {
                    // Cleanup all the stray info before JSONArray conversation; do not change ",\\}"
                    strJson = strJson.replaceAll("\\.html\\(false\\)", "")
                            .replaceAll(",\\}", "\\}")  // \\ is not a redundant: 'LINK_TYPE_MP_APPMSG',}]
                            .replaceAll(" //.*?subject", "subject"); // remove comment string
                    return new JSONArray(strJson);
                }
            }
        } catch (IOException | JSONException e) {
            Timber.e("URL JsonArray Exception: %s", e.getMessage());
            HymnsApp.showToastMessage(R.string.gui_nq_download_failed, title);
        }
        return null;
    }
}
