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

import android.os.*;
import android.text.TextUtils;

import org.cog.hymnchtv.*;
import org.cog.hymnchtv.persistance.DatabaseBackend;
import org.cog.hymnchtv.utils.SocketConnection;
import org.jetbrains.annotations.NotNull;
import org.json.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * The class provide handlers for the QQ info record
 * @see MediaConfig for the format of the QQ record
 *
 * @author Eng Chong Meng
 */
public class QQRecord
{
    public static final List<String> qqHymnType
            = Arrays.asList("【大本诗歌D】", "【䃼充本诗歌B】", "【儿童诗歌C】", "【新歌颂咏X】");

    public static final Map<String, String> qqHymn2Type = new HashMap<>();

    static {
        qqHymn2Type.put("D", HYMN_DB);
        qqHymn2Type.put("B", HYMN_BB);
        qqHymn2Type.put("C", HYMN_ER);
        qqHymn2Type.put("X", HYMN_XB);
    }

    // QQ JSONObject key values
    public static final String QQ_TITLE = "title";
    public static final String QQ_URL = "url";
    public static final String QQ_SUBJECT = "subject_name";

    public static final String DOWNLOAD_FP = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    public static final String DOWNLOAD_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getName();

    private final String mHymnType;
    private final int mHymnNo;
    private String mHymnTitle;
    private String mMediaUri;
    private String mFilePath;

    public QQRecord(String hymnType, int hymnNo)
    {
        this(hymnType, hymnNo, null, null, null);
    }

    public QQRecord(String hymnType, int hymnNo, String title, String mediaUri, String filePath)
    {
        mHymnType = hymnType;
        mHymnNo = hymnNo;
        mHymnTitle = title;
        mMediaUri = mediaUri;
        mFilePath = filePath;
    }

    public void setMediaUri(String mediaUri)
    {
        mMediaUri = mediaUri;
    }

    public void setFilePath(String filePath)
    {
        mFilePath = filePath;
    }

    public void setHymnTitle(String title)
    {
        mHymnTitle = title;
    }

    public String getHymnType()
    {
        return mHymnType;
    }

    public int getHymnNo()
    {
        return mHymnNo;
    }

    public String getHymnTitle()
    {
        return mHymnTitle;
    }

    public String getMediaUri()
    {
        if (TextUtils.isEmpty(mMediaUri) || "null".equalsIgnoreCase(mMediaUri))
            return null;

        return mMediaUri;
    }

    public String getMediaFilePath()
    {
        if (TextUtils.isEmpty(mFilePath) || "null".equalsIgnoreCase(mFilePath))
            return null;

        return mFilePath;
    }

    /**
     * Convert the give string which containing full parameters for conversion to QQRecord
     *
     * @param qqString the exported string or ListView string
     * @return the converted QQRecord, or null for invalid qqRecord string
     */
    public static QQRecord toRecord(String qqString)
    {
        if (TextUtils.isEmpty(qqString))
            return null;

        // Try to split assuming ListView item string
        String[] recordItem = qqString.split(":[# ]|\\nuri: |\\nfp: |[ ]*[,\\t][ ]*");

        // must has all the 5 parameters for the QQRecord
        if (recordItem.length < 5)
            return null;

        return new QQRecord(recordItem[0],
                Integer.parseInt(recordItem[1]),
                recordItem[2],
                recordItem[3],
                recordItem[4]);
    }

    /**
     * Convert the media records in the database to an exportable string
     *
     * @return QQRecord string for database export
     */
    public String toExportString()
    {
        String qqRecord = null;
        if (getMediaFilePath() != null) {
            qqRecord = String.format(Locale.CHINA, "%s,%d,%s,%s,%s\r\n",
                    mHymnType, mHymnNo, mHymnTitle, null, mFilePath);
        }
        else if (getMediaUri() != null) {
            qqRecord = String.format(Locale.CHINA, "%s,%d,%s,%s,%s\r\n",
                    mHymnType, mHymnNo, mHymnTitle, mMediaUri, null);
        }
        return qqRecord;
    }

    /**
     * Convert the database QQRecord info to user-friendly display list record.
     *
     * @return QQRecord in user readable String
     */
    public @NotNull String toString()
    {
        // Decode the uri link for friendly user UI
        String uriLink = getMediaUri();
        String filePath = getMediaFilePath();
        if (filePath != null) {
            filePath = filePath.replace(DOWNLOAD_FP, DOWNLOAD_DIR);
        }

        String hymnNo = String.format(Locale.CHINA, "%04d", mHymnNo);
        return String.format(Locale.CHINA, "%s:#%s: %s\nuri: %s\nfp: %s",
                    mHymnType, hymnNo, mHymnTitle, uriLink, filePath);
    }

    /**
     * Start the QQ links extractions at QQ main page on new thread; All network access must not be on UI thread.
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
        HymnsApp.showToastMessage(R.string.gui_qq_download_starting);
        new Thread()
        {
            public void run()
            {
                try {
                    JSONArray jsonArray = fetchJsonArray(ContentHandler.HYMNCHTV_QQ_MAIN);
                    if (jsonArray != null) {
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
                    HymnsApp.showToastMessage(R.string.gui_qq_download_failed);
                    return;
                }

                HymnsApp.showToastMessage(R.string.gui_qq_download_completed);
                new Handler(Looper.getMainLooper()).post(mediaConfig::showQQRecords);
            }
        }.start();
    }

    /**
     * Extract the hymnType Range and proceed all the hymn records in the range
     * Proceed to hymn records for【新歌颂咏】as it contains not range value
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
        HymnsApp.showToastMessage(R.string.gui_qq_download_in_progress, prefix);
        //【新歌颂咏】does not have hymnRange; so proceed to saveQQRecord
        if (prefix.contains("新歌颂咏")) {
            saveQQRecord(url);
            return;
        }

        try {
            JSONArray jsonArray = fetchJsonArray(url);
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                    Timber.d("QQ HymnType Range (%s): %s", i, jsonObject);
                    String hymnRange = jsonObject.getString(QQ_TITLE);
                    if (hymnRange.startsWith(prefix)) {
                        saveQQRecord(jsonObject.getString(QQ_URL));
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
     * @param url the QQ required site url
     */
    private static void saveQQRecord(String url)
    {
        DatabaseBackend mDB = DatabaseBackend.getInstance(HymnsApp.getGlobalContext());
        try {
            JSONArray jsonArray = fetchJsonArray(url);
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                    // Timber.d("QQ Hymn Record (%s): %s", i, jsonObject);
                    mDB.storeQQJObject(jsonObject);
                }
            }
        } catch (JSONException e) {
            Timber.e("URL get source exception: %s", e.getMessage());
        }
    }

    /**
     * Extra and phrase all the links info into JSONArray
     *
     * @param url the remote url containing the required link info
     * @return JSON Array of the extracted info or null if none found
     */
    private static JSONArray fetchJsonArray(String url)
    {
        // Standard enclosing pattern for link info used on the QQ sites
        Pattern pattern = Pattern.compile("var jumpInfo = (\\[.*?]);");
        // android does not allow cleartextTraffic access
        url = url.replace("http:", "https:");

        try {
            String urlSource = SocketConnection.getURLSource(url);
            Matcher matcher = pattern.matcher(urlSource);
            if (matcher.find()) {
                String strJson = matcher.group(1);
                if (!TextUtils.isEmpty(strJson)) {
                    // Cleanup all the stray info before JSONArray conversation; do not change ",\\}"
                    strJson = strJson.replaceAll("\\.html\\(false\\)", "").replaceAll(",\\}", "\\}");
                    return new JSONArray(strJson);
                }
            }
        } catch (IOException | JSONException e) {
            Timber.e("URL JsonArray Exception: %s", e.getMessage());
        }
        return null;
    }
}
