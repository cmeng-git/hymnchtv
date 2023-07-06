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
import static org.cog.hymnchtv.mediaconfig.MediaConfig.IMPORT_URL_VERSION;
import static org.cog.hymnchtv.mediaconfig.MediaConfig.PREF_VERSION_URL;

import android.Manifest;
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

import org.cog.hymnchtv.BuildConfig;
import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.MainActivity;
import org.cog.hymnchtv.R;
import org.cog.hymnchtv.mediaconfig.MediaConfig;
import org.cog.hymnchtv.persistance.FileBackend;
import org.cog.hymnchtv.persistance.FilePathHelper;
import org.cog.hymnchtv.persistance.PermissionUtils;
import org.cog.hymnchtv.utils.DialogActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import timber.log.Timber;

/**
 * hymnchtv update service implementation. It checks for an update and schedules .apk download using android DownloadManager.
 * It is only activated for the debug version. Android initials the auto-update from PlayStore for release version.
 *
 * @author Eng Chong Meng
 */
public class UpdateServiceImpl {
    // Default update link; path is case-sensitive.
    private static final String[] updateLinks = {
            "https://raw.githubusercontent.com/cmeng-git/hymnchtv/master/hymnchtv/release/version.properties",
            "https://atalk.sytes.net/releases/hymnchtv/version.properties"
    };

    // Url import link file location
    private static final String urlImport = "https://raw.githubusercontent.com/cmeng-git/hymnchtv/master/hymnchtv/src/main/assets/url_import.txt";

    /**
     * Apk mime type constant.
     */
    private static final String APK_MIME_TYPE = "application/vnd.android.package-archive";
    private static final String fileNameApk = String.format("/hymnchtv-%s.apk", BuildConfig.BUILD_TYPE);

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
                if (checkLastDLFileAction() < DownloadManager.ERROR_UNKNOWN)
                    return;

                DialogActivity.showConfirmDialog(HymnsApp.getGlobalContext(),
                        R.string.gui_app_update_install,
                        R.string.gui_app_version_new_available,
                        R.string.gui_download,
                        new DialogActivity.DialogListener() {
                            @Override
                            public boolean onConfirmClicked(DialogActivity dialog) {
                                if (PermissionUtils.hasWriteStoragePermission(MainActivity.getInstance(), true)) {
                                    downloadApk();
                                }
                                return true;
                            }

                            @Override
                            public void onDialogCancelled(DialogActivity dialog) {
                            }
                        }, HymnsApp.getResString(R.string.app_title_main), latestVersion, latestVersionCode, currentVersion, currentVersionCode
                );
            } else {
                // Notify that running version is up to date
                DialogActivity.showConfirmDialog(HymnsApp.getGlobalContext(),
                        R.string.gui_app_update_none,
                        R.string.gui_app_version_current,
                        R.string.gui_download_again,
                        new DialogActivity.DialogListener() {
                            @Override
                            public boolean onConfirmClicked(DialogActivity dialog) {
                                if (PermissionUtils.hasWriteStoragePermission(MainActivity.getInstance(), true)) {
                                    if (checkLastDLFileAction() >= DownloadManager.ERROR_UNKNOWN)
                                        downloadApk();
                                }
                                return true;
                            }

                            @Override
                            public void onDialogCancelled(DialogActivity dialog) {
                            }
                        }, currentVersion, currentVersionCode, latestVersion, currentVersionCode
                );
            }
        } else {
            HymnsApp.showToastMessage(R.string.gui_app_update_none);
        }
    }

    /**
     * Check for any existing downloaded file and take appropriate action;
     *
     * @return Last DownloadManager status; default to DownloadManager.ERROR_UNKNOWN if status unknown
     */
    private int checkLastDLFileAction() {
        // Check old or scheduled downloads
        int lastJobStatus = DownloadManager.ERROR_UNKNOWN;

        List<Long> previousDownloads = getOldDownloads();
        if (previousDownloads.size() > 0) {
            long lastDownload = previousDownloads.get(previousDownloads.size() - 1);

            lastJobStatus = checkDownloadStatus(lastDownload);
            if (lastJobStatus == DownloadManager.STATUS_SUCCESSFUL) {
                DownloadManager downloadManager = HymnsApp.getDownloadManager();
                Uri fileUri = downloadManager.getUriForDownloadedFile(lastDownload);

                // Ask the user if he wants to install the valid apk when found
                if (isValidApkVersion(fileUri, latestVersionCode)) {
                    askInstallDownloadedApk(fileUri);
                }
            } else if (lastJobStatus != DownloadManager.STATUS_FAILED) {
                // Download is in progress or scheduled for retry
                DialogActivity.showDialog(HymnsApp.getGlobalContext(),
                        R.string.gui_in_progress,
                        R.string.gui_download_in_progress);
            } else {
                // Download id return failed status, remove failed id and retry
                removeOldDownloads();
                DialogActivity.showDialog(HymnsApp.getGlobalContext(),
                        R.string.gui_app_update_install, R.string.gui_download_failed);
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
                R.string.gui_download_completed,
                R.string.gui_app_install_ready,
                mIsLatest ? R.string.gui_app_reinstall : R.string.gui_app_update,
                new DialogActivity.DialogListener() {
                    @Override
                    public boolean onConfirmClicked(DialogActivity dialog) {
                        Context context = HymnsApp.getGlobalContext();
                        // Need REQUEST_INSTALL_PACKAGES in manifest; Intent.ACTION_VIEW works for both
                        Intent intent;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                        } else {
                            intent = new Intent(Intent.ACTION_VIEW);
                        }
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
     * @return download status of the job identified by given id. If given job is not found
     * {@link DownloadManager#STATUS_FAILED} will be returned.
     */
    @SuppressLint("Range")
    private int checkDownloadStatus(long id) {
        DownloadManager downloadManager = HymnsApp.getDownloadManager();
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);

        try (Cursor cursor = downloadManager.query(query)) {
            if (!cursor.moveToFirst())
                return DownloadManager.STATUS_FAILED;
            else
                return cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
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
            HymnsApp.getGlobalContext().registerReceiver(downloadReceiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
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
            if (checkLastDLFileAction() < DownloadManager.ERROR_UNKNOWN)
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
            } catch (NumberFormatException e) {
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
                    HymnsApp.showToastMessage(R.string.gui_app_version_invalid, apkVersionCode, versionCode);
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
        return HymnsApp.getResString(R.string.gui_app_new_available, latestVersion, latestVersionCode);
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

        for (String aLink : updateLinks) {
            try {
                if (isValidateLink(aLink)) {
                    InputStream in = mHttpConnection.getInputStream();
                    Properties mProperties = new Properties();
                    mProperties.load(in);

                    latestVersion = mProperties.getProperty("last_version");
                    latestVersionCode = Long.parseLong(mProperties.getProperty("last_version_code"));

                    try {
                        int versionUrl = Integer.parseInt(mProperties.getProperty("version_import"));
                        checkUrlImport(versionUrl);
                    } catch (NumberFormatException e) {
                        Timber.e("Url version unavailable: %s", e.getMessage());
                    }

                    String aLinkPrefix = aLink.substring(0, aLink.lastIndexOf("/"));
                    downloadLink = aLinkPrefix + fileNameApk;
                    if (isValidateLink(downloadLink)) {
                        MainActivity.mHasUpdate = currentVersionCode < latestVersionCode;
                        // return true if current running application is already the latest
                        return (currentVersionCode >= latestVersionCode);
                    } else {
                        downloadLink = null;
                    }
                }
            } catch (IOException e) {
                Timber.w("Could not retrieve version.properties for checking: %s", e.getMessage());
            }
        }
        // return true if all failed to force update.
        return true;
    }

    private void checkUrlImport(int version) {
        Context context = HymnsApp.getGlobalContext();
        SharedPreferences mSharedPref = context.getSharedPreferences(PREF_SETTINGS, 0);
        int versionUrl = mSharedPref.getInt(PREF_VERSION_URL, IMPORT_URL_VERSION);

        if ((version > versionUrl) && isValidateLink(urlImport)) {
            try {
                // opens input stream from the HTTP connection
                InputStream inputStream = mHttpConnection.getInputStream();

                // Save a copy in hymn import_export directory for user later access
                String fileName = String.format("url_import-%s.txt",
                        new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()));
                File file = MediaConfig.createFileIfNotExist(fileName, true);
                if (file != null) {
                    FileOutputStream outputStream = new FileOutputStream(file);
                    FileBackend.copy(inputStream, outputStream);
                    outputStream.close();

                    inputStream = new FileInputStream(file);
                    MediaConfig.importUrlRecords(inputStream, false);
                    inputStream.close();

                    SharedPreferences.Editor mEditor = mSharedPref.edit();
                    mEditor.putInt(PREF_VERSION_URL, version);
                    mEditor.apply();
                }
            } catch (IOException e) {
                Timber.e("%s", e.getMessage());
            }
        }
    }

    /**
     * Check if the given link is accessible.
     *
     * @param link the link to check
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
        } catch (IOException e) {
            Timber.d("Invalid url: %s", e.getMessage());
            return false;
        }
        return false;
    }
}