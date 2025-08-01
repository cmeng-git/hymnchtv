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

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.cog.hymnchtv.persistance.FileBackend;
import org.cog.hymnchtv.persistance.FilePathHelper;
import org.cog.hymnchtv.utils.AndroidUtils;
import org.cog.hymnchtv.utils.ByteFormat;

import timber.log.Timber;

/**
 * Class implements the media content handler. It proceeds to download median from online source
 * if there is no local resource available.
 * Must have only one instance of this class running at any one time; else UI display may have problem.
 *
 * @author Eng Chong Meng
 */
public class MediaDownloadHandler extends Fragment {
    public View fileXferUi;
    private TextView fileLabel = null;
    private TextView fileStatus = null;
    private TextView fileXferSpeed = null;
    private TextView estTimeRemain = null;
    private ProgressBar progressBar = null;

    /* DownloadManager Broadcast Receiver Handler */
    private DownloadManager downloadManager;
    private DownloadReceiver downloadReceiver = null;

    /* previousDownloads map of <DownloadJobId, @notNull destFile>; need TreeMap.lastKey */
    private final static NavigableMap<Long, File> fileDownloads = new TreeMap<>();
    private final static NavigableMap<Long, String> linkDownloads = new TreeMap<>();

    // File download variables
    protected File mXferFile;

    private ContentHandler mContentHandler;

    // Need this to prevent crash on rotation if there are other constructors implementation
    // public MediaGuiController() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View convertView = inflater.inflate(R.layout.file_xfer_ui, container, false);

        fileLabel = convertView.findViewById(R.id.filexferFileNameView);
        fileStatus = convertView.findViewById(R.id.filexferStatusView);

        progressBar = convertView.findViewById(R.id.file_progressbar);
        fileXferSpeed = convertView.findViewById(R.id.file_progressSpeed);
        estTimeRemain = convertView.findViewById(R.id.file_estTime);

        // Hide the file xfer UI on start
        fileXferUi = convertView.findViewById(R.id.filexferUi);
        fileXferUi.setVisibility(View.GONE);

        convertView.findViewById(R.id.view_cancel).setOnClickListener(view -> fileXferUi.setVisibility(View.GONE));
        return convertView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mContentHandler = (ContentHandler) getContext();
        if (mContentHandler == null)
            return;

        if (downloadManager == null)
            downloadManager = HymnsApp.getDownloadManager();

        if (downloadReceiver == null) {
            downloadReceiver = new DownloadReceiver();
            ContextCompat.registerReceiver(HymnsApp.getGlobalContext(), downloadReceiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED);
        }
    }

    /**
     * Returns the string, showing information for the given file.
     *
     * @param fileName the name of the file
     * @param fileSize the size of the file
     *
     * @return the name of the given file
     */
    protected String getFileLabel(String fileName, long fileSize) {
        String text = ByteFormat.format(fileSize);
        return fileName + " (" + text + ")";
    }

    // ********************************** File download Handler ********************************************//
    // Routines supporting File Download

    /**
     * Call from ContentHandler to fetch the requested media fName from the specified dnLnk
     *
     * @param dnLnk the source link for downloading file.
     * @param dir the destination dir for the downloaded file.
     * @param fileName the downloaded filename.
     */
    public void initHttpFileDownload(String dnLnk, String dir, String fileName) {
        File subDir = FileBackend.getHymnchtvStore(dir, true);
        if (subDir == null) {
            HymnsApp.showToastMessage(R.string.file_access_no_permission);
            return;
        }

        File destFile = new File(subDir, fileName);
        if (fileDownloads.containsValue(destFile)) {
            HymnsApp.showToastMessage(R.string.download_wait);
            fileXferUi.setVisibility(View.VISIBLE);
            Timber.w("Skip duplicated download file request: %s", destFile.getAbsolutePath());
            return;
        }

        fileXferUi.setVisibility(View.VISIBLE);
        fileStatus.setVisibility(View.GONE);
        fileLabel.setText(getFileLabel(fileName, mFileSize));

        String encDnLnk = dnLnk;
        try {
            // Need to encode chinese link for safe access; revert all "%3A" and "%2F" to ":" and "/" etc
            encDnLnk = AndroidUtils.UrlEncode(dnLnk);
        } catch (UnsupportedEncodingException e) {
            Timber.w("Exception in URLEncoder.encode (%s): %s", fileName, e.getMessage());
        }
        Uri uri = Uri.parse(encDnLnk);

        long jobId = download(uri, fileName);
        if (jobId != -1) {
            fileDownloads.put(jobId, destFile);
            linkDownloads.put(jobId, encDnLnk);
            startProgressChecker();
        }
        else {
            fileXferUi.setVisibility(View.GONE);
        }
    }

    /**
     * Schedules media file download.
     */
    private long download(Uri uri, String fileName) {
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        File tmpFile = new File(fileName);
        try {
            mXferFile = new File(FileBackend.getHymnchtvStore(FileBackend.TMP, true), fileName);
            request.setDestinationUri(Uri.fromFile(mXferFile));

            return downloadManager.enqueue(request);
        } catch (SecurityException e) {
            HymnsApp.showToastMessage(e.getMessage());
        } catch (Exception e) {
            Timber.w("Download Manager failed for: %s; %s", tmpFile.getAbsolutePath(), e.getMessage());
            HymnsApp.showToastMessage(R.string.file_does_not_exist);
        }
        return -1;
    }

    private class DownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Fetching the download id received with the broadcast and
            // if the received broadcast is for our enqueued download by matching download id
            long downloadJobId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            int downloadJobStatus = checkDownloadStatus(downloadJobId);

            if (fileDownloads.containsKey(downloadJobId)) {
                String dnLink = linkDownloads.get(downloadJobId);

                if (downloadJobStatus == DownloadManager.STATUS_SUCCESSFUL) {
                    Uri fileUri = downloadManager.getUriForDownloadedFile(downloadJobId);
                    File inFile = new File(FilePathHelper.getFilePath(context, fileUri));

                    if (inFile.exists()) {
                        // update fileSize for progress bar update, in case it is still not updated by download Manager
                        mFileSize = inFile.length();
                        File destFile = fileDownloads.get(downloadJobId);
                        // destFile will always not null; just to keep AS happy
                        String destFName = (destFile != null) ? destFile.getName() : "";

                        // Rename will move the received media infile to destFile dir.
                        // mFileSize == 1 if file not found online
                        if ((mFileSize > 200) && (destFile != null) && inFile.renameTo(destFile)) {
                            String uiLabel = fileLabel.getText().toString();
                            Timber.d("Downloaded file: %s (size: %s); label: %s; uiShown: %s",
                                    destFName, mFileSize, uiLabel, fileXferUi.isShown());

                            // Start playing only if the same player user still stay put.
                            // Otherwise, ui is not sync and user has no control of the play back
                            if (fileXferUi.isShown() && uiLabel.startsWith(destFName)) {
                                mContentHandler.startPlay();
                            }
                        }
                        else {
                            HymnsApp.showToastMessage(R.string.file_download_failed, dnLink);
                            Timber.d("Downloaded file failed: %s (size: %s) <= %s",
                                    inFile.getAbsolutePath(), mFileSize, dnLink);
                        }
                    }
                }
                else if (downloadJobStatus == DownloadManager.STATUS_FAILED) {
                    onError(HymnsApp.getResString(R.string.file_download_failed, dnLink));
                }
            }
            // Remove lastDownloadId from downloadManager record and delete the tmp file
            downloadManager.remove(downloadJobId);
            fileDownloads.remove(downloadJobId);
            linkDownloads.remove(downloadJobId);

            if (fileDownloads.isEmpty())
                fileXferUi.setVisibility(View.GONE);

        }
    }

    private void onError(String statusText) {
        fileStatus.setVisibility(View.GONE);
        fileStatus.setText(statusText);
        mContentHandler.onError(statusText);
    }

    /**
     * Get the jobId for the given dnFile
     *
     * @param dnFile previously download File
     *
     * @return jobId for the dnFile if available else -1
     */
    private long getJobId(File dnFile) {
        for (Map.Entry<Long, File> entry : fileDownloads.entrySet()) {
            if (entry.getValue().equals(dnFile)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    //=========================================================
    /*
     * Monitoring file download progress
     */
    private static final int PROGRESS_DELAY = 1000;

    // The maximum download idle time (60 seconds) before it is forced stop
    private static final int MAX_IDLE_TIME = 60;

    private boolean isProgressCheckerRunning = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private long previousProgress;
    private int waitTime;

    /**
     * The size of the file to be transferred.
     */
    private long mFileSize = 0;

    /**
     * The time of the last fileTransfer update.
     */
    private long mLastTimestamp = -1;

    /**
     * The number of bytes in last transferred.
     */
    private long mLastTransferredBytes = 0;

    /**
     * The last calculated average progress speed.
     */
    private long mTransferSpeedAverage = 0;

    /**
     * The last estimated remaining time.
     */
    private long mEstimatedTimeLeft = -1;

    /**
     * Starts monitoring the download progress.
     * This method is safe to call multiple times. Starting an already running progress checker is a no-op.
     */
    private void startProgressChecker() {
        if (!isProgressCheckerRunning) {
            isProgressCheckerRunning = true;

            mLastTransferredBytes = 0;
            waitTime = MAX_IDLE_TIME;
            previousProgress = -1;

            progressChecker.run();
        }
    }

    /**
     * Stops monitoring download progress.
     */
    private void stopProgressChecker() {
        isProgressCheckerRunning = false;
        handler.removeCallbacks(progressChecker);
    }

    /**
     * Checks download progress and updates status, then re-schedules itself.
     */
    private final Runnable progressChecker = new Runnable() {
        @Override
        public void run() {
            if (isProgressCheckerRunning) {
                checkProgress();
                handler.postDelayed(this, PROGRESS_DELAY);
            }
        }
    };

    /**
     * Queries the <tt>DownloadManager</tt> for the status of download job identified by the given <tt>id</tt>.
     *
     * @param id download identifier which status will be returned.
     *
     * @return download status of the job identified by given id. If the given job is not found
     * {@link DownloadManager#STATUS_FAILED} will be returned.
     */
    private int checkDownloadStatus(long id) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);

        try (Cursor cursor = downloadManager.query(query)) {
            if (!cursor.moveToFirst())
                return DownloadManager.STATUS_FAILED;
            else {
                return cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            }
        }
    }

    /**
     * Checks and show to user download progress for the last file transfer task entry only
     */
    private void checkProgress() {
        if (fileDownloads.isEmpty()) {
            stopProgressChecker();
            return;
        }

        File lastdestFile = fileDownloads.get(fileDownloads.lastKey());
        long lastDownloadId = getJobId(lastdestFile);
        if (lastDownloadId == -1)
            return;

        int lastJobStatus = checkDownloadStatus(lastDownloadId);
        String mFileName = lastdestFile.getName();

        // Terminate downloading task if failed or idleTime timeout
        if (lastJobStatus == DownloadManager.STATUS_FAILED || waitTime <= 0) {
            // Remove lastDownloadId from downloadManager record and delete the tmp file
            downloadManager.remove(lastDownloadId);
            fileDownloads.remove(lastDownloadId);
            linkDownloads.remove(lastDownloadId);

            File tmpFile = new File(FileBackend.getHymnchtvStore(FileBackend.TMP, false), mFileName);
            Timber.d("Downloading file failed due to slow progress: %s (%s): %s",
                    tmpFile.length(), mFileSize, tmpFile.getAbsolutePath());
            onError(HymnsApp.getResString(R.string.file_download_failed, tmpFile.getAbsolutePath()));
            return;
        }

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(lastDownloadId);
        query.setFilterByStatus(~(DownloadManager.STATUS_FAILED | DownloadManager.STATUS_SUCCESSFUL));
        Cursor cursor = downloadManager.query(query);

        if (cursor.moveToFirst()) {
            mFileSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            long progress = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));

            if (progressBar.isShown()) {
                fileLabel.setText(getFileLabel(mFileName, mFileSize));
                progressBar.setMax((int) mFileSize);
            }

            if (progress <= previousProgress) {
                waitTime--;
                Timber.d("Downloading file countdown, jobId: %s; status: %s; dnProgress: %s (%s)",
                        lastDownloadId, lastJobStatus, previousProgress, waitTime);
                if (waitTime < 50) {
                    fileXferSpeed.setText(HymnsApp.getResString(R.string.download_timeout_timer, waitTime));
                }
            }
            else {
                waitTime = MAX_IDLE_TIME;
                previousProgress = progress;
            }
            updateProgress(mFileName, previousProgress, System.currentTimeMillis());
        }
        cursor.close();
    }

    /**
     * Calculate a moving average for file download speed with a larger SMOOTHING_FACTOR;
     * so the UI display remaining time is no so jumpy
     *
     * @param transferredBytes file size
     * @param progressTimestamp time stamp
     */
    private void updateProgress(String fileName, long transferredBytes, long progressTimestamp) {
        long SMOOTHING_FACTOR = 100;

        // before file transfer start is -1
        if (transferredBytes == -1)
            return;

        final String bytesString = ByteFormat.format(transferredBytes);
        long byteTransferDelta = (transferredBytes == 0) ? 0 : (transferredBytes - mLastTransferredBytes);

        // Calculate running average transfer speed in bytes/sec and time left, with the given SMOOTHING_FACTOR
        if (mLastTimestamp > 0) {
            long timeElapsed = progressTimestamp - mLastTimestamp;
            long transferSpeedCurrent = (timeElapsed > 0) ? (byteTransferDelta * 1000) / timeElapsed : 0;
            if (mTransferSpeedAverage != 0) {
                mTransferSpeedAverage = (transferSpeedCurrent + (SMOOTHING_FACTOR - 1) * mTransferSpeedAverage) / SMOOTHING_FACTOR;
            }
            else {
                mTransferSpeedAverage = transferSpeedCurrent;
            }
        }
        else {
            mEstimatedTimeLeft = -1;
        }

        // Calculate  running average time left in sec
        if (mTransferSpeedAverage > 0)
            mEstimatedTimeLeft = (mFileSize - transferredBytes) / mTransferSpeedAverage;

        mLastTimestamp = progressTimestamp;
        mLastTransferredBytes = transferredBytes;

        // Need to do it here as it was found that Http File Upload completed before the progress Bar is even visible
        if (!progressBar.isShown()) {
            progressBar.setVisibility(View.VISIBLE);
            if (mXferFile != null)
                progressBar.setMax((int) mXferFile.length());
        }
        // Note: progress bar can only handle int size (4-bytes: 2,147,483, 647);
        progressBar.setProgress((int) transferredBytes);

        if (mTransferSpeedAverage > 0) {
            fileXferSpeed.setVisibility(View.VISIBLE);
            fileXferSpeed.setText(
                    HymnsApp.getResString(R.string.download_speed, ByteFormat.format(mTransferSpeedAverage), bytesString));
        }
        Timber.d("%s RxByte = %s / %s; TimeLeft = %s; speed = %s", fileName, transferredBytes, mFileSize,
                mEstimatedTimeLeft, mTransferSpeedAverage);

        if (transferredBytes >= mFileSize) {
            estTimeRemain.setVisibility(View.GONE);
        }
        else if (mEstimatedTimeLeft > 0) {
            estTimeRemain.setVisibility(View.VISIBLE);
            estTimeRemain.setText(HymnsApp.getResString(R.string.download_remaining_time,
                    AndroidUtils.formatSeconds(mEstimatedTimeLeft * 1000)));
        }
    }
}
