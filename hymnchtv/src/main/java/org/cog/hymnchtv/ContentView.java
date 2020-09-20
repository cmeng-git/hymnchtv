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
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.cog.hymnchtv.persistance.FileBackend;
import org.cog.hymnchtv.persistance.FilePathHelper;
import org.cog.hymnchtv.utils.ByteFormat;
import org.cog.hymnchtv.utils.HymnIdx2NoConvert;

import java.io.*;
import java.util.*;

import timber.log.Timber;

import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_NB;
import static org.cog.hymnchtv.MainActivity.TOC_BB;
import static org.cog.hymnchtv.MainActivity.TOC_DB;
import static org.cog.hymnchtv.MainActivity.TOC_ER;
import static org.cog.hymnchtv.MainActivity.TOC_NB;

/**
 * The class displays the hymn lyrics content selected by user
 *
 * @author Eng Chong Meng
 */
@SuppressLint("NonConstantResourceId")
public class ContentView extends Fragment implements OnClickListener, OnLongClickListener
{
    public static String LYRICS_ER_SCORE = "lyrics_er_score/";
    public static String LYRICS_NB_SCORE = "lyrics_nb_score/";
    public static String LYRICS_BB_SCORE = "lyrics_bb_score/";
    public static String LYRICS_DB_SCORE = "lyrics_db_score/";

    public static String LYRICS_BBS_TEXT = "lyrics_bbs_text/";
    public static String LYRICS_DBS_TEXT = "lyrics_dbs_text/";

    public static String LYRICS_BB_TEXT = "lyrics_bb_text/";
    public static String LYRICS_DB_TEXT = "lyrics_db_text/";

    public static String LYRICS_TOC = "lyrics_toc/";

    public final static String LYRICS_TYPE = "lyricsType";
    public final static String LYRICS_INDEX = "lyricsIndex";

    private View lyricsView;
    private View mConvertView;
    private ImageView mContentView = null;

    private final ProgressBar progressBar = null;
    private final TextView fileLabel = null;
    private final TextView fileStatus = null;

    private MediaType mediaType;

    private SharedPreferences mSharePref;

    /**
     * The xfer file full path for saving the received file.
     */
    protected File mXferFile;
    protected Uri mUri;
    List<Uri> mediaHymns;

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

    // Need this to prevent crash on rotation if there are other constructors implementation
    // public ContentView()
    // {
    // }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        mConvertView = inflater.inflate(R.layout.content_lyrics, container, false);
        lyricsView = mConvertView.findViewById(R.id.lyricsView);
        mContentView = mConvertView.findViewById(R.id.contentView);

        String lyricsType = getArguments().getString(LYRICS_TYPE);
        int lyricsIndex = getArguments().getInt(LYRICS_INDEX);

        if (!TextUtils.isEmpty(lyricsType)) {
            updateHymnContent(lyricsType, lyricsIndex);
        }
        return mConvertView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        registerForContextMenu(lyricsView);
    }

    @Override
    public void onPause()
    {
        unregisterForContextMenu(lyricsView);
        super.onPause();
    }

    /**
     * The lyrics png file has the following formats: HYMN_ER, HYMN_NB, HYMN_BB, HYMN_DB
     * i.e. er, nb, bb, db followed by the hymn number, a, b, c etc for more than one page;
     * The files are stored in asset respective sub-dir e.g. LYRICS_NB_SCORE
     *
     * The content view can support up to 5 pages for user vertical scrolling
     *
     * @param lyricsType see below cases
     * @param index hymn index provided by the page adapter when use scroll
     */
    private void updateHymnContent(String lyricsType, int index)
    {
        String resPrefix;
        String resFName = "";

        int[] hymnInfo = HymnIdx2NoConvert.hymnIdx2NoConvert(lyricsType, index);

        switch (lyricsType) {
            case HYMN_ER:
                resPrefix = LYRICS_ER_SCORE + hymnInfo[0];
                break;

            case HYMN_NB:
                resPrefix = LYRICS_NB_SCORE + "nb" + hymnInfo[0];
                break;

            case HYMN_BB:
                resPrefix = LYRICS_BB_SCORE + "bb" + hymnInfo[0];
                resFName = LYRICS_BBS_TEXT + hymnInfo[0] + ".txt";
                break;

            case HYMN_DB:
                resPrefix = LYRICS_DB_SCORE + "db" + hymnInfo[0];
                resFName = LYRICS_DBS_TEXT + hymnInfo[0] + ".txt";
                break;

            case TOC_ER:
                resPrefix = LYRICS_TOC + "er_toc";
                break;

            case TOC_NB:
                resPrefix = LYRICS_TOC + "nb_toc";
                break;

            case TOC_BB:
                resPrefix = LYRICS_TOC + "bb_toc";
                break;

            case TOC_DB:
                resPrefix = LYRICS_TOC + "db_toc";
                break;

            default: //if (HYMN_ER.equals(mSelect)) {
                resPrefix = LYRICS_ER_SCORE + "er" + hymnInfo[0];
        }


        if (resPrefix.startsWith(LYRICS_TOC)) {
            showLyricsToc(resPrefix, index);
            return;
        }

        showLyricsScore(resPrefix, hymnInfo);

        if (!TextUtils.isEmpty(resFName))
            showLyricsText(resFName);
    }

    private void showLyricsToc(String resPrefix, int index)
    {
        if (resPrefix.startsWith(LYRICS_TOC)) {
            String resName = resPrefix + index + ((index > 19) ? ".jpg" : ".png");
            Uri resUri = Uri.fromFile(new File("//android_asset/", resName));
            MyGlideApp.loadImage(mContentView, resUri);
            return;
        }

    }

    private void showLyricsScore(String resPrefix, int[] hymnInfo)
    {
        int pages = hymnInfo[1]; // The number of pages for the current hymn number
        ImageView contentView;

        String resName = resPrefix + ".png";
        Uri resUri = Uri.fromFile(new File("//android_asset/", resName));
        MyGlideApp.loadImage(mContentView, resUri);

        if (pages > 1) {
            contentView = mConvertView.findViewById(R.id.contentView_a);
            resName = resPrefix + "a.png";
            resUri = Uri.fromFile(new File("//android_asset/", resName));
            MyGlideApp.loadImage(contentView, resUri);
        }
        else {
            return;
        }

        if (pages > 2) {
            contentView = mConvertView.findViewById(R.id.contentView_b);
            resName = resPrefix + "b.png";
            resUri = Uri.fromFile(new File("//android_asset/", resName));
            MyGlideApp.loadImage(contentView, resUri);
        }
        else {
            return;
        }

        if (pages > 3) {
            contentView = mConvertView.findViewById(R.id.contentView_c);
            resName = resPrefix + "c.png";
            resUri = Uri.fromFile(new File("//android_asset/", resName));
            MyGlideApp.loadImage(contentView, resUri);
        }
        else {
            return;
        }

        if (pages > 4) {
            contentView = mConvertView.findViewById(R.id.contentView_d);
            resName = resPrefix + "d.png";
            resUri = Uri.fromFile(new File("//android_asset/", resName));
            MyGlideApp.loadImage(contentView, resUri);
        }
    }


    private void showLyricsText(String resFName)
    {
        TextView lyricsView = mConvertView.findViewById(R.id.contentView_txt);
        lyricsView.setTextSize(HymnsApp.isPortrait ? 20 : 35);

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().getAssets().open(resFName)));
            StringBuilder lyrics = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                lyrics.append(line);
                lyrics.append('\n');
            }
            lyricsView.setText(lyrics);
        } catch (IOException e) {
            Timber.w("Error reading file: %s", resFName);
        }
    }


    public ImageView getSelectedView()
    {
        return mContentView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        getActivity().getMenuInflater().inflate(R.menu.content_menu, menu);
    }

    // ================ Http Hymn download Support functions ==========

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
                for (Uri uri : mediaHymns) {
                    mUri = uri;
                    // playStart();
                }
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
            for (Uri uri : mediaHymns) {
                mUri = uri;
                // playerStop();
            }
            return true;
        }
        return false;
    }

    public void updateHymnContent(int index)
    {
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
        // setTransferFilePath(fileName, mimeType);

        // Change the file name to the name we would use on the local file system.
        if (!mXferFile.getName().equals(fileName)) {
            String label = getFileLabel(mXferFile.getName(), infile.length());
            // hymnInfo.setText(label);
        }
        return mXferFile;
    }

    // ********************************************************************************************//
    // Routines supporting File Download

    /**
     * Method fired when the chat message is clicked. {@inheritDoc}
     * Trigger from @see ChatFragment#
     *
     * @param checkFileSize check acceptable file Size limit before download if true
     */
    private void initHttpFileDownload(boolean checkFileSize)
    {
        String url;
        if (previousDownloads.contains(dnLink))
            return;

        if (downloadReceiver == null) {
            downloadReceiver = new DownloadReceiver();
            HymnsApp.getGlobalContext().registerReceiver(downloadReceiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }

        url = dnLink;
        // for testing only to display url in chat window
        // mChatFragment.getChatPanel().addMessage("", new Date(), IMessage.ENCODE_PLAIN, IMessage.ENCODE_PLAIN, aesgcmUrl.getAesgcmUrl());

        Uri uri = Uri.parse(url);
        if (fileSize == -1) {
            fileSize = queryFileSize(uri);
            // hymnInfo.setText(getFileLabel(fileName, fileSize));
        }

        long jobId = download(uri);
        if (jobId > 0) {
            previousDownloads.put(jobId, dnLink);
            Timber.d("Download Manager init HttpFileDownload Size: %s %s", fileSize, previousDownloads.toString());
        }
    }

    /**
     * Schedules media file download.
     */
    private long download(Uri uri)
    {
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        try {
            File tmpFile = new File(FileBackend.getHymnchtvStore(FileBackend.TMP, true), fileName);
            request.setDestinationUri(Uri.fromFile(tmpFile));
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
                        File outFile = createOutFile(inFile);

                        // Plain media file sharing; rename will move the infile to outfile dir.
                        if (inFile.renameTo(outFile)) {
                            mXferFile = outFile;
                            // updateView(FileTransferStatusChangeEvent.COMPLETED, null);
                        }

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
        for (Map.Entry entry : previousDownloads.entrySet()) {
            if (entry.getValue().equals(dnLink)) {
                return (long) entry.getKey();
            }
        }
        return -1;
    }

    //=========================================================
    /*
     * Monitoring file download progress
     */
    private static final int PROGRESS_DELAY = 1000;

    // Maximum download idle time (60 seconds) before it is forced stop
    private static final int MAX_IDLE_TIME = 60;

    private boolean isProgressCheckerRunning = false;
    private Handler handler = new Handler();

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

            // onUploadProgress(progress, fileSize);
            // updateProgress(uploadedBytes, System.currentTimeMillis());

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
