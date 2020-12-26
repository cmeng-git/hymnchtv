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

import android.Manifest;
import android.app.DownloadManager;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import org.cog.hymnchtv.*;
import org.cog.hymnchtv.persistance.FilePathHelper;
import org.cog.hymnchtv.persistance.PermissionUtils;
import org.cog.hymnchtv.utils.AndroidUtils;
import org.cog.hymnchtv.utils.DialogActivity;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import timber.log.Timber;

/**
 * hymnchtv update service implementation. It checks for an update and schedules .apk download using android DownloadManager.
 *
 * @author Eng Chong Meng
 */
public class UpdateServiceImpl
{
    public static int UNINSTALL_REQUEST_CODE = 120;

    // Default update link
    private static final String[] updateLinks = {"https://atalk.sytes.net", "https://atalk.mooo.com"};

    /**
     * Apk mime type constant.
     */
    public static final String APK_MIME_TYPE = "application/vnd.android.package-archive";

    // path are case-sensitive
    private static final String filePath = "/releases/hymnchtv/versionupdate.properties";

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

    /* DownloadManager Broadcast Receiver Handler */
    private DownloadReceiver downloadReceiver = null;

    /**
     * The download link
     */
    private String downloadLink;

    /**
     * <tt>SharedPreferences</tt> used to store download ids.
     */
    private SharedPreferences store;

    /**
     * Name of <tt>SharedPreferences</tt> entry used to store old download ids. Ids are stored in
     * single string separated by ",".
     */
    private static final String ENTRY_NAME = "apk_ids";

    private static UpdateServiceImpl mInstance = null;

    public static UpdateServiceImpl getInstance()
    {
        if (mInstance == null) {
            mInstance = new UpdateServiceImpl();
        }
        return mInstance;
    }

    /**
     * Checks for updates.
     *
     * @param notifyAboutNewestVersion <tt>true</tt> if the user is to be notified if they have the
     * newest version already; otherwise, <tt>false</tt>
     */
    public void checkForUpdates(boolean notifyAboutNewestVersion)
    {
        // set to inverse for testing only
        boolean isLatest = isLatestVersion();
        Timber.i("Is latest: %s\nCurrent version: %s\nLatest version: %s\nDownload link: %s",
                isLatest, currentVersion, latestVersion, downloadLink);

        // Reverse the logic i.e. !isLast for testing
        if (!isLatest && (downloadLink != null)) {
            // Check old or scheduled downloads
            List<Long> previousDownloads = getOldDownloads();
            if (previousDownloads.size() > 0) {
                long lastDownload = previousDownloads.get(previousDownloads.size() - 1);

                int lastJobStatus = checkDownloadStatus(lastDownload);
                if (lastJobStatus == DownloadManager.STATUS_SUCCESSFUL) {
                    DownloadManager downloadManager = HymnsApp.getDownloadManager();
                    Uri fileUri = downloadManager.getUriForDownloadedFile(lastDownload);
                    File apkFile = new File(FilePathHelper.getFilePath(HymnsApp.getGlobalContext(), fileUri));

                    // Ask the user if he wants to install if available and valid apk is found
                    if (isValidApkVersion(apkFile, latestVersionCode)) {
                        checkUninstallBeforeInstallApk(fileUri);
                        return;
                    }
                }
                else if (lastJobStatus != DownloadManager.STATUS_FAILED) {
                    // Download is in progress or scheduled for retry
                    AndroidUtils.showAlertDialog(HymnsApp.getGlobalContext(),
                            HymnsApp.getResString(R.string.gui_in_progress),
                            HymnsApp.getResString(R.string.gui_download_in_progress));
                    return;
                }
            }

            AndroidUtils.showAlertConfirmDialog(HymnsApp.getGlobalContext(),
                    HymnsApp.getResString(R.string.gui_update_install),
                    HymnsApp.getResString(R.string.gui_version_new_available,
                            HymnsApp.getResString(R.string.app_name), latestVersion, latestVersionCode),
                    HymnsApp.getResString(R.string.gui_download),
                    new DialogActivity.DialogListener()
                    {
                        @Override
                        public boolean onConfirmClicked(DialogActivity dialog)
                        {
                            if (PermissionUtils.isPermissionGranted(HymnsApp.getGlobalContext(),
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                downloadApk();
                            }
                            return true;
                        }

                        @Override
                        public void onDialogCancelled(DialogActivity dialog)
                        {
                        }
                    }
            );
        }
        else if (notifyAboutNewestVersion) {
            // Notify that running version is up to date
            AndroidUtils.showAlertDialog(HymnsApp.getGlobalContext(),
                    HymnsApp.getResString(R.string.gui_update_none),
                    HymnsApp.getResString(R.string.gui_version_current,
                            currentVersion, Long.toString(currentVersionCode)));
        }
    }

    /**
     * Asks the user whether to install downloaded .apk.
     *
     * @param fileUri download file uri of the apk to install.
     */
    private void askInstallDownloadedApk(Uri fileUri)
    {
        DialogActivity.showConfirmDialog(HymnsApp.getGlobalContext(),
                R.string.gui_download_completed,
                R.string.gui_downloaded_install,
                R.string.gui_update,
                new DialogActivity.DialogListener()
                {
                    @Override
                    public boolean onConfirmClicked(DialogActivity dialog)
                    {
                        // Need REQUEST_INSTALL_PACKAGES in manifest; Intent.ACTION_VIEW works for both
                        Intent intent;
                        intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setDataAndType(fileUri, APK_MIME_TYPE);

                        HymnsApp.getGlobalContext().startActivity(intent);
                        return true;
                    }

                    @Override
                    public void onDialogCancelled(DialogActivity dialog)
                    {
                    }
                });
    }

    /**
     * Asks the user to install downloaded .apk; e.g. due to version code conflict.
     *
     * @param fileUri download file uri of the apk to install.
     */
    private void askUninstallApk(Uri fileUri)
    {
        String app_pkg_name = "org.cog.hymnchtv";
        File apkFile = new File(FilePathHelper.getFilePath(HymnsApp.getGlobalContext(), fileUri));

        DialogActivity.showConfirmDialog(HymnsApp.getGlobalContext(),
                R.string.gui_download_completed,
                R.string.gui_downloaded_uninstall,
                R.string.gui_ok,
                new DialogActivity.DialogListener()
                {
                    @Override
                    public boolean onConfirmClicked(DialogActivity dialog)
                    {
                        // Need REQUEST_INSTALL_PACKAGES in manifest; Intent.ACTION_VIEW works for both
                        Intent intent;

                        intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
                        intent.setData(Uri.parse("package:" + app_pkg_name));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        MainActivity.mActivity.startActivityForResult(intent, UNINSTALL_REQUEST_CODE);
                        return true;
                    }

                    @Override
                    public void onDialogCancelled(DialogActivity dialog)
                    {
                        askInstallDownloadedApk(fileUri);
                    }
                }, apkFile.getAbsolutePath());
    }

    /**
     * Ask the user whether to allow uninstall app.
     *
     * @param fileUri download file uri of the apk to install.
     */
    private void checkUninstallBeforeInstallApk(Uri fileUri)
    {
        if (currentVersionCode > latestVersionCode) {
            askUninstallApk(fileUri);
        }
        else {
            askInstallDownloadedApk(fileUri);
        }
    }

    /**
     * Queries the <tt>DownloadManager</tt> for the status of download job identified by given <tt>id</tt>.
     *
     * @param id download identifier which status will be returned.
     * @return download status of the job identified by given id. If given job is not found
     * {@link DownloadManager#STATUS_FAILED} will be returned.
     */
    private int checkDownloadStatus(long id)
    {
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
    private void downloadApk()
    {
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
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager downloadManager = HymnsApp.getDownloadManager();
        long jobId = downloadManager.enqueue(request);
        rememberDownloadId(jobId);
    }

    private class DownloadReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            List<Long> previousDownloads = getOldDownloads();
            if (previousDownloads.size() > 0) {
                long lastDownload = previousDownloads.get(previousDownloads.size() - 1);

                int lastJobStatus = checkDownloadStatus(lastDownload);
                if (lastJobStatus == DownloadManager.STATUS_SUCCESSFUL) {
                    DownloadManager downloadManager = HymnsApp.getDownloadManager();
                    Uri fileUri = downloadManager.getUriForDownloadedFile(lastDownload);
                    File apkFile = new File(FilePathHelper.getFilePath(HymnsApp.getGlobalContext(), fileUri));

                    // Ask the user if he wants to install if available and valid apk is found
                    if (isValidApkVersion(apkFile, latestVersionCode)) {
                        checkUninstallBeforeInstallApk(fileUri);
                        return;
                    }
                }
                else if (lastJobStatus == DownloadManager.STATUS_FAILED) {
                    // Download is in progress or scheduled for retry
                    AndroidUtils.showAlertDialog(HymnsApp.getGlobalContext(),
                            HymnsApp.getResString(R.string.gui_update_install),
                            HymnsApp.getResString(R.string.gui_download_failed));
                    return;
                }
            }

            // unregistered downloadReceiver
            if (downloadReceiver != null) {
                HymnsApp.getGlobalContext().unregisterReceiver(downloadReceiver);
                downloadReceiver = null;
            }
        }
    }

    private SharedPreferences getStore()
    {
        if (store == null) {
            store = HymnsApp.getGlobalContext().getSharedPreferences("store", Context.MODE_PRIVATE);
        }
        return store;
    }

    private void rememberDownloadId(long id)
    {
        SharedPreferences store = getStore();
        String storeStr = store.getString(ENTRY_NAME, "");
        storeStr += id + ",";
        store.edit().putString(ENTRY_NAME, storeStr).apply();
    }

    private List<Long> getOldDownloads()
    {
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
    public void removeOldDownloads()
    {
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
     * @param apkFile apk File
     * @param versionCode use the given versionCode to check against the apk versionCode
     * @return true if apkFile has the specified versionCode
     */
    private boolean isValidApkVersion(File apkFile, long versionCode)
    {
        boolean isValid = false;
        if (apkFile.exists()) {
            // Get downloaded apk actual versionName and versionCode
            PackageManager pm = HymnsApp.getGlobalContext().getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
            isValid = (info != null) && (versionCode == info.versionCode);

            // Notify that the download version is not valid
//            if (!isValid) {
//                AndroidUtils.showAlertDialog(HymnsApp.getGlobalContext(),
//                        R.string.gui_update_none, R.string.gui_update_invalid);
//            }
        }
        return isValid;
    }

    /**
     * Gets the latest available (software) version online.
     *
     * @return the latest (software) version
     */
    public String getLatestVersion()
    {
        return latestVersion;
    }

    /**
     * Determines whether we are currently running the latest version.
     *
     * @return <tt>true</tt> if current running application is the latest version; otherwise, <tt>false</tt>
     */
    public boolean isLatestVersion()
    {
        Properties mProperties = null;
        String errMsg = "";

        VersionServiceImpl versionService = VersionServiceImpl.getInstance();
        currentVersion = versionService.getCurrentVersionName();
        currentVersionCode = versionService.getCurrentVersionCode();

        if (updateLinks.length == 0) {
            Timber.d("Updates are disabled, emulates latest version.");
        }
        else {
            for (String aLink : updateLinks) {
                String urlStr = aLink.trim() + filePath;
                try {
                    URL mUrl = new URL(urlStr);
                    HttpURLConnection httpConnection = (HttpURLConnection) mUrl.openConnection();
                    httpConnection.setRequestMethod("GET");
                    httpConnection.setRequestProperty("Content-length", "0");
                    httpConnection.setUseCaches(false);
                    httpConnection.setAllowUserInteraction(false);
                    httpConnection.setConnectTimeout(100000);
                    httpConnection.setReadTimeout(100000);

                    httpConnection.connect();
                    int responseCode = httpConnection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream in = httpConnection.getInputStream();
                        mProperties = new Properties();
                        mProperties.load(in);
                        break;
                    }
                } catch (IOException e) {
                    errMsg = e.getMessage();
                }
            }

            if (mProperties != null) {
                latestVersion = mProperties.getProperty("last_version");
                latestVersionCode = Long.parseLong(mProperties.getProperty("last_version_code"));
                if (BuildConfig.DEBUG) {
                    downloadLink = mProperties.getProperty("download_link-debug");
                }
                else {
                    downloadLink = mProperties.getProperty("download_link");
                }
                // return true is current running application is already the latest
                return (currentVersionCode >= latestVersionCode);
            }
            else {
                Timber.w("Could not retrieve version.properties for checking: %s", errMsg);
            }
        }
        return true;
    }
}
