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

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.cog.hymnchtv.persistance.FileBackend;
import org.cog.hymnchtv.persistance.FilePathHelper;
import org.cog.hymnchtv.utils.ByteFormat;

import java.io.File;
import java.util.Hashtable;
import java.util.Map;

import timber.log.Timber;

import static org.cog.hymnchtv.MainActivity.PREF_MEDIA_HYMN;

/**
 * Class implements the media content handler. It proceeds to download median from online source
 * if there is no local resource available.
 *
 * @author Eng Chong Meng
 */
@SuppressLint("NonConstantResourceId")
public class MediaHandler extends Fragment implements OnClickListener, OnLongClickListener
{
    /**
     * The state of a player where playback is stopped
     */
    private static final int STATE_STOP = 0;
    private static String STATE_PLAYER = "statePlayer";
    private int statePlayer;
    private TextView hymnInfo = null;

    private View fileXferUi;
    private ProgressBar progressBar = null;
    private final TextView fileLabel = null;

    private TextView fileStatus = null;
    private TextView fileXferError = null;
    private TextView fileXferSpeed = null;
    private TextView estTimeRemain = null;

    private MediaType mMediaType;

    private SharedPreferences mSharePref;

    /**
     * The xfer file full path for saving the received file.
     */
    protected File mXferFile;

    // File download variables
    private long fileSize;
    private String fileName;
    private String dnLink;

    /* previousDownloads <DownloadJobId, Download Link> */
    private final Hashtable<Long, String> previousDownloads = new Hashtable<>();

    /* previousDownloads <DownloadJobId, DownloadFileMimeType Link> */
    // private final Hashtable<Long, String> mimeTypes = new Hashtable<>();

    /* DownloadManager Broadcast Receiver Handler */
    private DownloadManager downloadManager;
    private DownloadReceiver downloadReceiver = null;

    private ContentHandler mContentHandler;

    private static MediaHandler mInstance = null;

    // Need this to prevent crash on rotation if there are other constructors implementation
    // public MediaController()
    // {
    // }

    public MediaHandler getInstance()
    {
        if (mInstance == null) {
            mInstance = new MediaHandler();
        }
        return mInstance;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View convertView = inflater.inflate(R.layout.file_xfer_ui, container, false);

        fileStatus = convertView.findViewById(R.id.filexferStatusView);
        fileXferError = convertView.findViewById(R.id.errorView);

        fileXferSpeed = convertView.findViewById(R.id.file_progressSpeed);
        estTimeRemain = convertView.findViewById(R.id.file_estTime);
        progressBar = convertView.findViewById(R.id.file_progressbar);

        fileXferUi = convertView.findViewById(R.id.filexferUi);
        fileXferUi.setVisibility(View.INVISIBLE);

        if (savedInstanceState != null) {
            statePlayer = savedInstanceState.getInt(STATE_PLAYER);
        }
        else {
            statePlayer = STATE_STOP;
        }
        return convertView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mContentHandler = (ContentHandler) getContext();
        if (mContentHandler == null)
            return;

        //        if (mReceiver == null) {
        //            mReceiver = new MpBroadcastReceiver();
        //            registerMpBroadCastReceiver();
        //        }

        // Get the user selected mediaType to playback
        mSharePref = MainActivity.getSharedPref();
        int mediaType = mSharePref.getInt(PREF_MEDIA_HYMN, MediaType.HYMN_MIDI.getValue());
        mMediaType = MediaType.valueOf(mediaType);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        savedInstanceState.putInt(STATE_PLAYER, statePlayer);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Returns a string showing information for the given file.
     *
     * @param file the file
     * @return the name and size of the given file
     */
    protected String getFileLabel(File file)
    {
        if ((file != null) && file.exists()) {
            String fileName = file.getName();
            long fileSize = file.length();
            return getFileLabel(fileName, fileSize);
        }
        return (file == null) ? "" : file.getName();
    }

    /**
     * Returns the string, showing information for the given file.
     *
     * @param fileName the name of the file
     * @param fileSize the size of the file
     * @return the name of the given file
     */
    protected String getFileLabel(String fileName, long fileSize)
    {
        String text = ByteFormat.format(fileSize);
        return fileName + " (" + text + ")";
    }

    /**
     * Handle button click action events.
     */
    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.playback_play:
                break;
        }
    }

    /**
     * Handles buttons long press action events
     * mainly use to stop and release player
     */
    @Override
    public boolean onLongClick(View v)
    {
        if (v.getId() == R.id.playback_play) {
            return true;
        }
        return false;
    }

    /**
     * Determine the mimeType of the given file
     *
     * @param file the media file to check
     * @return mimeType or null if undetermined
     */
    private String checkMimeType(File file)
    {
        if (!file.exists()) {
            // HymnsApp.showToastMessage(R.string.gui_file_DOES_NOT_EXIST);
            return null;
        }

        try {
            Uri uri = FileBackend.getUriForFile(mContentHandler, file);
            String mimeType = FileBackend.getMimeType(mContentHandler, uri);
            if ((mimeType == null) || mimeType.contains("application")) {
                mimeType = "*/*";
            }
            return mimeType;

        } catch (SecurityException e) {
            Timber.i("No permission to access %s: %s", file.getAbsolutePath(), e.getMessage());
            HymnsApp.showToastMessage(R.string.gui_file_OPEN_NO_PERMISSION);
            return null;
        }
    }

    /**
     * Generate the mXferFile full filePath based on the given fileName and mimeType
     *
     * @param fileName the incoming xfer fileName
     * @param mimeType the incoming file mimeType
     */
    protected void setTransferFilePath(String fileName, String mimeType)
    {
        String downloadPath = FileBackend.MEDIA_DOCUMENT;
        if (fileName.contains("voice-"))
            downloadPath = FileBackend.MEDIA_VOICE_RECEIVE;
        else if (!TextUtils.isEmpty(mimeType) && !mimeType.startsWith("*")) {
            downloadPath = FileBackend.MEDIA + File.separator + mimeType.split("/")[0];
        }

        File downloadDir = FileBackend.getHymnchtvStore(downloadPath, true);
        mXferFile = new File(downloadDir, fileName);

        // If a file with the given name already exists, add an index to the file name.
        int index = 0;
        int filenameLength = fileName.lastIndexOf(".");
        if (filenameLength == -1) {
            filenameLength = fileName.length();
        }
        while (mXferFile.exists()) {
            String newFileName = fileName.substring(0, filenameLength) + "-"
                    + ++index + fileName.substring(filenameLength);
            mXferFile = new File(downloadDir, newFileName);
        }
    }


    //================ File download Handler ===============

    /**
     * Creates the local file to save to.
     *
     * @return the local created file to save to.
     */
    private File createOutFile(File infile)
    {
        String fileName = infile.getName();
        String mimeType = FileBackend.getMimeType(getActivity(), Uri.fromFile(infile));
        setTransferFilePath(fileName, mimeType);

        // Change the file name to the name we would use on the local file system.
        if (!mXferFile.getName().equals(fileName)) {
            String label = getFileLabel(mXferFile.getName(), infile.length());
            hymnInfo.setText(label);
        }
        return mXferFile;
    }

    /**
     * Returns the label to show on the progress bar.
     *
     * @param bytesString the bytes that have been transferred
     * @return the label to show on the progress bar
     */
    protected String getProgressLabel(long bytesString)
    {
        return HymnsApp.getResString(R.string.gui_received, bytesString);
    }

    // ********************************************************************************************//
    // Routines supporting File Download

    // 第112首 神生命的种子 http://g.cgbr.org/music/x/media/112x.mp3
    // http://g.cgbr.org/music/x/media/139.mp3


    // heavenly.food
    // https://heavenlyfood.cn/hymns/jiaochang/da/293.mp3
    // https://heavenlyfood.cn/hymnal/%E8%AF%97%E6%AD%8C/%E5%A4%A7%E6%9C%AC%E8%AF%97%E6%AD%8C/03%E8%B5%9E%E7%BE%8E%E4%B8%BB053/D141%E5%93%A6%E4%B8%BB%E8%80%B6%E7%A8%A3%E6%AF%8F%E6%83%B3%E5%88%B0%E4%BD%A0.mp3
    // https://heavenlyfood.cn/hymnal/%E8%AF%97%E6%AD%8C/%E5%A4%A7%E6%9C%AC%E8%AF%97%E6%AD%8C/03%E8%B5%9E%E7%BE%8E%E4%B8%BB053/D141%E5%93%A6%E4%B8%BB%E8%80%B6%E7%A8%A3%E6%AF%8F%E6%83%B3%E5%88%B0%E4%BD%A0.mp3

    public static String link_jiaochang = "https://heavenlyfood.cn/hymns/jiaochang/da/";

    /**
     * Method fired when the chat message is clicked. {@inheritDoc}
     * Trigger from @see ChatFragment#
     *
     * @param dnLink the source link for downloading file.
     * @param dir the source link for downloading file.
     * @param fileName the source link for downloading file.
     */
    public void initHttpFileDownload(String dnLink, String dir, String fileName)
    {
        String url;
        if (previousDownloads.contains(dnLink))
            return;

        if (downloadReceiver == null) {
            downloadReceiver = new DownloadReceiver();
            HymnsApp.getGlobalContext().registerReceiver(downloadReceiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }

        Uri uri = Uri.parse(dnLink);
        if (fileSize == -1) {
            fileSize = queryFileSize(uri);
            hymnInfo.setText(getFileLabel(fileName, fileSize));
        }

        long jobId = download(uri, dir, fileName);
        if (jobId > 0) {
            previousDownloads.put(jobId, dnLink);
            Timber.d("Download Manager init HttpFileDownload Size: %s %s", fileSize, previousDownloads.toString());
        }
    }

    /**
     * Schedules media file download.
     */
    private long download(Uri uri, String dir, String fileName)
    {
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        try {
            File outFile = new File(FileBackend.getHymnchtvStore(dir, true), fileName);
            request.setDestinationUri(Uri.fromFile(outFile));
            // request.addRequestHeader("User-Agent", getUserAgent());

            return downloadManager.enqueue(request);
        } catch (SecurityException e) {
            HymnsApp.showToastMessage(e.getMessage());
        } catch (Exception e) {
            HymnsApp.showToastMessage(R.string.gui_file_DOES_NOT_EXIST);
        }
        return -1;
    }

    /**
     * Query the http uploaded file size for auto download.
     */
    private long queryFileSize(Uri uri)
    {
        Timber.d("Download Manager file size query started");
        int size = -1;
        DownloadManager.Request request = new DownloadManager.Request(uri);
        long id = downloadManager.enqueue(request);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);

        // allow loop for 3 seconds for slow server. Server can return size == 0 ?
        int wait = 3;
        while ((wait-- > 0) && (size <= 0)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Timber.w("Download Manager query file size exception: %s", e.getMessage());
                return -1;
            }
            Cursor cursor = downloadManager.query(query);
            if (cursor.moveToFirst()) {
                size = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            }
            cursor.close();
        }
        Timber.d("Download Manager file size query end: %s (%s)", size, wait);
        return size;
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
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);

        try (Cursor cursor = downloadManager.query(query)) {
            if (!cursor.moveToFirst())
                return DownloadManager.STATUS_FAILED;
            else {
                return cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            }
        }
    }

    private class DownloadReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // Fetching the download id received with the broadcast and
            // if the received broadcast is for our enqueued download by matching download id
            long lastDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            int lastJobStatus = checkDownloadStatus(lastDownloadId);
            Timber.d("Download receiver %s: %s", lastDownloadId, lastJobStatus);

            String statusText = HymnsApp.getResString(R.string.gui_file_DOWNLOAD_FAILED, dnLink);
            if (previousDownloads.containsKey(lastDownloadId)) {
                if (lastJobStatus == DownloadManager.STATUS_SUCCESSFUL) {
                    String dnLink = previousDownloads.get(lastDownloadId);

                    Uri fileUri = downloadManager.getUriForDownloadedFile(lastDownloadId);
                    File inFile = new File(FilePathHelper.getPath(context, fileUri));

                    // update fileSize for progress bar update, in case it is still not updated by download Manager
                    fileSize = inFile.length();

                    if (inFile.exists()) {
                        // Create outFile
                        // File outFile = createOutFile(inFile);

                        // Plain media file sharing; rename will move the infile to outfile dir.
                        // if (inFile.renameTo(outFile)) {
                        // mXferFile = outFile;
                        // updateView(FileTransferStatusChangeEvent.COMPLETED, null);
                        // }

                        // Timber.d("Downloaded fileSize: %s (%s)", outFile.length(), fileSize);
                        previousDownloads.remove(lastDownloadId);
                        downloadManager.remove(lastDownloadId);
                    }
                }
                else if (lastJobStatus == DownloadManager.STATUS_FAILED) {
                    fileStatus.setText(statusText);
                }
            }
            else if (DownloadManager.STATUS_FAILED == lastJobStatus) {
                fileStatus.setText(statusText);
            }
        }
    }

    /**
     * Get the jobId for the given dnLink
     *
     * @param dnLink previously download link
     * @return jobId for the dnLink if available else -1
     */
    private long getJobId(String dnLink)
    {
        for (Map.Entry<Long, String> entry : previousDownloads.entrySet()) {
            if (entry.getValue().equals(dnLink)) {
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
    private final Handler handler = new Handler();

    private long previousProgress;
    private int waitTime;

    /**
     * Checks download progress.
     */
    private void checkProgress()
    {
        long lastDownloadId = getJobId(dnLink);
        int lastJobStatus = checkDownloadStatus(lastDownloadId);
        Timber.d("Downloading file jobId: %s; status: %s; dnProgress: %s (%s)", lastDownloadId, lastJobStatus,
                previousProgress, waitTime);

        // Terminate downloading task if failed or idleTime timeout
        if (lastJobStatus == DownloadManager.STATUS_FAILED || waitTime < 0) {
            File tmpFile = new File(FileBackend.getHymnchtvStore(FileBackend.TMP, true), fileName);
            Timber.d("Downloaded fileSize (failed): %s (%s)", tmpFile.length(), fileSize);
        }

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterByStatus(~(DownloadManager.STATUS_FAILED | DownloadManager.STATUS_SUCCESSFUL));
        Cursor cursor = downloadManager.query(query);
        if (!cursor.moveToFirst()) {
            cursor.close();
            return;
        }
        do {
            fileSize = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            long progress = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));

            if (progressBar.isShown()) {
                fileLabel.setText(getFileLabel(fileName, fileSize));
                progressBar.setMax((int) fileSize);
            }

            if (progress <= previousProgress)
                waitTime--;
            else {
                waitTime = MAX_IDLE_TIME;
                previousProgress = progress;
            }

            //            onUploadProgress(progress, fileSize);
            //            updateProgress(uploadedBytes, System.currentTimeMillis());

        } while (cursor.moveToNext());
        cursor.close();
    }

    /**
     * Starts watching download progress.
     *
     * This method is safe to call multiple times. Starting an already running progress checker is a no-op.
     */
    private void startProgressChecker()
    {
        if (!isProgressCheckerRunning) {
            isProgressCheckerRunning = true;
            waitTime = MAX_IDLE_TIME;
            previousProgress = -1;

            progressChecker.run();
        }
    }

    /**
     * Stops watching download progress.
     */
    private void stopProgressChecker()
    {
        isProgressCheckerRunning = false;
        handler.removeCallbacks(progressChecker);
    }

    /**
     * Checks download progress and updates status, then re-schedules itself.
     */
    private Runnable progressChecker = new Runnable()
    {
        @Override
        public void run()
        {
            if (isProgressCheckerRunning) {
                checkProgress();
                handler.postDelayed(this, PROGRESS_DELAY);
            }
        }
    };
}
