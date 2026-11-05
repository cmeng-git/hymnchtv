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
package org.cog.hymnchtv.service.androidupdate;

import static org.cog.hymnchtv.MainActivity.PREF_SETTINGS;
import static org.cog.hymnchtv.mediaconfig.MediaConfig.PREF_VERSION_URL;
import static org.cog.hymnchtv.mediaconfig.MediaConfig.URL_IMPORT_VERSION;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.cog.hymnchtv.BuildConfig;
import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.MainActivity;
import org.cog.hymnchtv.R;
import org.cog.hymnchtv.mediaconfig.MediaConfig;
import org.cog.hymnchtv.persistance.FileBackend;
import org.cog.hymnchtv.persistance.FilePathHelper;
import org.cog.hymnchtv.persistance.PermissionUtils;
import org.cog.hymnchtv.utils.CustomDialogWv;
import org.cog.hymnchtv.utils.DialogActivity;
import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

/**
 * hymnchtv update service implementation. It checks for an update and schedules .apk download using <code>DownloadManager</code>.
 * All release/debug version has this implementated.
 *
 * @author Eng Chong Meng
 */
public class UpdateServiceImpl {
    // Default update link; path is case-sensitive.
    private static final String updateLink =
            "https://raw.githubusercontent.com/cmeng-git/hymnchtv/master/hymnchtv/release/version.properties";

    // filename is case-sensitive
    private static final String fileNameApk = String.format("/hymnchtv-%s.apk", BuildConfig.BUILD_TYPE);
    // github apk is in the release directory; for apk download
    private static final String urlApk = "https://github.com/cmeng-git/hymnchtv/releases/download/%s";

    // Url import link file location
    private static final String urlImport = "https://raw.githubusercontent.com/cmeng-git/hymnchtv/master/hymnchtv/src/main/assets/url_import.txt";
    // New changes info is extracted from changeLog
    private static final String changeLog = "https://raw.githubusercontent.com/cmeng-git/hymnchtv/master/hymnchtv/src/main/res/xml/changelog_master.xml";

    /**
     * Apk mime type constant.
     */
    private static final String APK_MIME_TYPE = "application/vnd.android.package-archive";

    /**
     * The download link for the installed application
     */
    private String downloadLink = null;

    /**
     * Current installed version string / version Code
     */
    private String currentVersion;
    private long currentVersionCode;

    /**
     * The latest version string / version code
     */
    private String latestVersion;
    private long latestVersionCode;
    private boolean mIsLatest = false;

    /* DownloadManager Broadcast Receiver Handler */
    private DownloadReceiver downloadReceiver = null;
    private HttpURLConnection mHttpConnection;

    /**
     * <code>SharedPreferences</code> used to store download ids.
     */
    private SharedPreferences store;

    /**
     * Name of <code>SharedPreferences</code> entry used to store old download ids. Ids are stored in
     * single string separated by ",".
     */
    private static final String ENTRY_NAME = "apk_ids";

    private static UpdateServiceImpl mInstance = null;

    public static UpdateServiceImpl getInstance() {
        if (mInstance == null) {
            mInstance = new UpdateServiceImpl();
        }
        return mInstance;
    }

    /**
     * Checks for updates and notify user of any new version, and take necessary action.
     */
    public void checkForUpdates() {
        // cmeng: reverse the logic to !isLatestVersion() for testing
        mIsLatest = isLatestVersion();
        Timber.i("Is latest: %s\nCurrent version: %s\nLatest version: %s\nDownload link: %s",
                mIsLatest, currentVersion, latestVersion, downloadLink);

        if ((downloadLink != null)) {
            if (!mIsLatest) {
                if (checkLastDLFileForAction() < DownloadManager.ERROR_UNKNOWN)
                    return;
                // currentVersion = "2.7.1";

                Context context = HymnsApp.getInstance();
                String title = context.getString(R.string.app_update_install);
                String message = context.getString(R.string.app_version_new_available, latestVersion,
                        String.valueOf(latestVersionCode), fileNameApk, currentVersion, String.valueOf(currentVersionCode));
                String btnText = context.getString(R.string.download);

                String historyText = "&#9210; 无更新";
                if (isValidateLink(changeLog)) {
                    try {
                        InputStream inputStream = mHttpConnection.getInputStream();
                        String releaseNotes = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

                        Pattern pattern = Pattern.compile("<changelog>.+?(<release version.+?)<release version=\"" + currentVersion, Pattern.DOTALL);
                        Matcher matcher = pattern.matcher(releaseNotes);
                        if (matcher.find()) {
                            historyText = matcher.group(1);
                            if (historyText != null) {
                                historyText = historyText
                                        .replaceAll("<release", "<b>Release")
                                        .replaceAll("versioncode.+?\">", "</b>")
                                        .replaceAll("<change", "&#9210; <change")
                                        .replaceAll("\n", "<br />");
                            }
                        }
                    }
                    catch (IOException e) {
                        Timber.d("Invalid changeLog link: %s", e.getMessage());
                        return;
                    }
                }

                Bundle args = new Bundle();
                args.putString(CustomDialogWv.ARG_MESSAGE, message);
                args.putString(CustomDialogWv.ARG_HISTORY, historyText);

                DialogActivity.showCustomDialog(HymnsApp.getInstance(), title, CustomDialogWv.class.getName(),
                        args, btnText, new DialogActivity.DialogListener() {
                            @Override
                            public boolean onConfirmClicked(DialogActivity dialog) {
                                downloadApk();
                                return true;
                            }

                            @Override
                            public void onDialogCancelled(@NotNull DialogActivity dialog) {
                            }
                        }, null);
            }
            else {
                // Notify that running version is up to date
                DialogActivity.showConfirmDialog(HymnsApp.getGlobalContext(),
                        R.string.app_update_none,
                        R.string.app_version_current,
                        R.string.download_again,
                        new DialogActivity.DialogListener() {
                            @Override
                            public boolean onConfirmClicked(DialogActivity dialog) {
                                if (PermissionUtils.checkWriteStoragePermission(MainActivity.getInstance())) {
                                    if (checkLastDLFileForAction() >= DownloadManager.ERROR_UNKNOWN)
                                        downloadApk();
                                }
                                return true;
                            }

                            @Override
                            public void onDialogCancelled(DialogActivity dialog) {
                            }
                        }, currentVersion, currentVersionCode, latestVersion, latestVersionCode
                );
            }
        }
        else {
            HymnsApp.showToastMessage(R.string.app_update_none);
        }
    }

    /**
     * Check for any existing downloaded file and take appropriate action;
     *
     * @return Last DownloadManager status; default to DownloadManager.ERROR_UNKNOWN if status unknown
     */
    private int checkLastDLFileForAction() {
        // Check old or scheduled downloads
        int lastJobStatus = DownloadManager.ERROR_UNKNOWN;

        List<Long> previousDownloads = getOldDownloads();
        if (!previousDownloads.isEmpty()) {
            long lastDownload = previousDownloads.get(previousDownloads.size() - 1);
            int[] status = checkDownloadStatus(lastDownload);

            lastJobStatus = status[0];
            int reason = status[1];
            if (lastJobStatus == DownloadManager.STATUS_SUCCESSFUL) {
                DownloadManager downloadManager = HymnsApp.getDownloadManager();
                Uri fileUri = downloadManager.getUriForDownloadedFile(lastDownload);

                // Ask the user if he wants to install the valid apk when found
                if (isValidApkVersion(fileUri, latestVersionCode)) {
                    askInstallDownloadedApk(fileUri);
                }
            }
            else if (lastJobStatus != DownloadManager.STATUS_FAILED) {
                // Download is in progress or scheduled for retry
                DialogActivity.showDialog(HymnsApp.getGlobalContext(),
                        R.string.in_progress,
                        R.string.download_in_progress);
            }
            else {
                // Download id return failed status, remove failed id and retry
                removeOldDownloads();
                DialogActivity.showDialog(HymnsApp.getGlobalContext(),
                        R.string.app_update_install, R.string.download_failed, reason);
            }
        }
        return lastJobStatus;
    }

    /**
     * Asks the user whether to install downloaded .apk.
     *
     * @param fileUri download file uri of the apk to install.
     */
    private void askInstallDownloadedApk(Uri fileUri) {
        DialogActivity.showConfirmDialog(HymnsApp.getGlobalContext(),
                R.string.download_completed,
                R.string.app_install_ready,
                mIsLatest ? R.string.app_reinstall : R.string.app_update,
                new DialogActivity.DialogListener() {
                    @Override
                    public boolean onConfirmClicked(DialogActivity dialog) {
                        Context context = HymnsApp.getGlobalContext();
                        // Need REQUEST_INSTALL_PACKAGES in manifest; Intent.ACTION_VIEW works for both
                        Intent intent;
                        intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                        intent.setDataAndType(fileUri, APK_MIME_TYPE);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        context.startActivity(intent);
                        return true;
                    }

                    @Override
                    public void onDialogCancelled(DialogActivity dialog) {
                    }
                }, latestVersion);
    }

    /**
     * Queries the <code>DownloadManager</code> for the status of download job identified by given <code>id</code>.
     *
     * @param id download identifier which status will be returned.
     *
     * @return download status of the job identified by given id. If given job is not found
     * {@link DownloadManager#STATUS_FAILED} will be returned.
     */
    @SuppressLint("Range")
    private int[] checkDownloadStatus(long id) {
        DownloadManager downloadManager = HymnsApp.getDownloadManager();
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);
        int[] status = new int[2];

        try (Cursor cursor = downloadManager.query(query)) {
            if (!cursor.moveToFirst()) {
                status[0] = DownloadManager.STATUS_FAILED;
                status[1] = DownloadManager.ERROR_INSUFFICIENT_SPACE;

                List<Long> downloadIds = new ArrayList<>();
                try (Cursor cursor1 = downloadManager.query(new DownloadManager.Query())) {
                    if (cursor1 != null) {
                        // Find the column index for the unique download ID
                        int idColumnIndex = cursor1.getColumnIndex(DownloadManager.COLUMN_ID);

                        // Iterate through the results and collect the IDs
                        while (cursor1.moveToNext()) {
                            long downloadId = cursor1.getLong(idColumnIndex);
                            downloadIds.add(downloadId);
                        }
                        Timber.e("Failed Id: %s; %s", id, downloadIds);
                    }
                } catch (Exception e) {
                    Timber.e("Check Download Status: %s", e.getMessage());
                }
            }
            else {
                int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                status[0] = cursor.getInt(statusIndex);
                status[1] = cursor.getInt(reasonIndex);
            }
            return status;
        }
    }

    /**
     * Schedules .apk download.
     */
    private void downloadApk() {
        Uri uri = Uri.parse(downloadLink);
        String fileName = uri.getLastPathSegment();

        if (downloadReceiver == null) {
            downloadReceiver = new DownloadReceiver();
            ContextCompat.registerReceiver(HymnsApp.getGlobalContext(), downloadReceiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED);
        }

        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setMimeType(APK_MIME_TYPE);
        File dnFile = new File(FileBackend.getHymnchtvStore(FileBackend.TMP, true), fileName);
        request.setDestinationUri(Uri.fromFile(dnFile));

        DownloadManager downloadManager = HymnsApp.getDownloadManager();
        long jobId = downloadManager.enqueue(request);
        rememberDownloadId(jobId);
    }

    private class DownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (checkLastDLFileForAction() < DownloadManager.ERROR_UNKNOWN)
                return;

            // unregistered downloadReceiver
            if (downloadReceiver != null) {
                HymnsApp.getGlobalContext().unregisterReceiver(downloadReceiver);
                downloadReceiver = null;
            }
        }
    }

    private SharedPreferences getStore() {
        if (store == null) {
            store = HymnsApp.getGlobalContext().getSharedPreferences("store", Context.MODE_PRIVATE);
        }
        return store;
    }

    private void rememberDownloadId(long id) {
        SharedPreferences store = getStore();
        String storeStr = store.getString(ENTRY_NAME, "");
        storeStr += id + ",";
        store.edit().putString(ENTRY_NAME, storeStr).apply();
    }

    private List<Long> getOldDownloads() {
        String storeStr = getStore().getString(ENTRY_NAME, "");
        String[] idStrs = storeStr.split(",");
        List<Long> apkIds = new ArrayList<>(idStrs.length);
        for (String idStr : idStrs) {
            try {
                if (!idStr.isEmpty())
                    apkIds.add(Long.parseLong(idStr));
            }
            catch (NumberFormatException e) {
                Timber.e("Error parsing apk id for string: %s [%s]", idStr, storeStr);
            }
        }
        return apkIds;
    }

    /**
     * Removes old downloads.
     */
    public void removeOldDownloads() {
        List<Long> apkIds = getOldDownloads();
        DownloadManager downloadManager = HymnsApp.getDownloadManager();
        for (long id : apkIds) {
            Timber.d("Removing .apk for id %s", id);
            downloadManager.remove(id);
        }
        getStore().edit().remove(ENTRY_NAME).apply();
    }

    /**
     * Validate the downloaded apk file for correct versionCode and its apk name
     *
     * @param fileUri apk Uri
     * @param versionCode use the given versionCode to check against the apk versionCode
     *
     * @return true if apkFile has the specified versionCode
     */
    private boolean isValidApkVersion(Uri fileUri, long versionCode) {
        // Default to valid as getPackageArchiveInfo() always return null; but sometimes OK
        boolean isValid = true;
        File apkFile = new File(FilePathHelper.getFilePath(HymnsApp.getGlobalContext(), fileUri));

        if (apkFile.exists()) {
            // Get downloaded apk actual versionCode and check its versionCode validity
            PackageManager pm = HymnsApp.getGlobalContext().getPackageManager();
            PackageInfo pckgInfo = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);

            if (pckgInfo != null) {
                long apkVersionCode;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
                    apkVersionCode = pckgInfo.versionCode;
                else
                    apkVersionCode = pckgInfo.getLongVersionCode();

                isValid = (versionCode == apkVersionCode);
                if (!isValid) {
                    HymnsApp.showToastMessage(R.string.app_version_invalid, apkVersionCode, versionCode);
                    Timber.d("Downloaded apk actual version code: %s (%s)", apkVersionCode, versionCode);
                }
            }
        }
        return isValid;
    }

    /**
     * Gets the latest available (software) version online.
     *
     * @return the latest (software) version
     */
    public String getLatestVersion() {
        return HymnsApp.getResString(R.string.app_new_available, latestVersion, latestVersionCode);
    }

    /**
     * Determines whether we are currently running the latest version.
     *
     * @return <code>true</code> if current running application is the latest version; otherwise, <code>false</code>
     */
    public boolean isLatestVersion() {
        VersionServiceImpl versionService = VersionServiceImpl.getInstance();
        currentVersion = versionService.getCurrentVersionName();
        currentVersionCode = versionService.getCurrentVersionCode();

        try {
            if (isValidateLink(updateLink)) {
                InputStream inputStream = mHttpConnection.getInputStream();
                Properties mProperties = new Properties();
                mProperties.load(inputStream);
                inputStream.close();

                latestVersion = mProperties.getProperty("last_version");
                latestVersionCode = Long.parseLong(mProperties.getProperty("last_version_code"));

                try {
                    String urlImport = mProperties.getProperty("url_import");
                    int versionUrl = Integer.parseInt(urlImport);
                    checkUrlImport(versionUrl);
                }
                catch (NumberFormatException e) {
                    Timber.e("Url import info unavailable: %s", e.getMessage());
                }

                downloadLink = urlApk.replace("%s", latestVersion) + fileNameApk;
                if (isValidateLink(downloadLink)) {
                    MainActivity.mHasUpdate = currentVersionCode < latestVersionCode;
                    // return true if current running application is already the latest
                    return (currentVersionCode >= latestVersionCode);
                }
                // No apk found for update
                else {
                    downloadLink = null;
                }
            }
        }
        catch (IOException e) {
            Timber.w("Could not retrieve version.properties for checking: %s", e.getMessage());
        }

        // return true if all failed to force update.
        return true;
    }

    private void checkUrlImport(int version) {
        Context context = HymnsApp.getGlobalContext();
        SharedPreferences mSharedPref = context.getSharedPreferences(PREF_SETTINGS, 0);
        int versionUrl = mSharedPref.getInt(PREF_VERSION_URL, URL_IMPORT_VERSION);

        if ((version > versionUrl) && isValidateLink(urlImport)) {
            try {
                // Open input stream from the HTTP connection; save a local copy and
                // use it for import url records. Cannot use http inputStream directly.
                InputStream inputStream = mHttpConnection.getInputStream();
                File file = MediaConfig.saveUrlImportFile(inputStream, version);
                if (file != null) {
                    inputStream = new FileInputStream(file);
                    MediaConfig.importUrlRecords(inputStream, false);
                    inputStream.close();
                }
            }
            catch (IOException e) {
                Timber.e("%s", e.getMessage());
            }
        }
    }

    /**
     * Check if the given link is accessible.
     *
     * @param link the link to check
     *
     * @return true if link is accessible
     */
    private boolean isValidateLink(String link) {
        try {
            URL mUrl = new URL(link);
            mHttpConnection = (HttpURLConnection) mUrl.openConnection();
            mHttpConnection.setRequestMethod("GET");
            mHttpConnection.setRequestProperty("Content-length", "0");
            mHttpConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
            mHttpConnection.setUseCaches(false);
            mHttpConnection.setAllowUserInteraction(false);
            mHttpConnection.setConnectTimeout(100000);
            mHttpConnection.setReadTimeout(100000);

            mHttpConnection.connect();
            int responseCode = mHttpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return true;
            }
        }
        catch (IOException e) {
            Timber.d("Invalid url: %s", e.getMessage());
            return false;
        }
        return false;
    }
}