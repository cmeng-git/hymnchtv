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
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.*;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.cog.hymnchtv.persistance.FileBackend;
import org.cog.hymnchtv.persistance.FilePathHelper;
import org.cog.hymnchtv.service.audioservice.AudioBgService;
import org.cog.hymnchtv.utils.ByteFormat;

import java.io.File;
import java.util.*;

import timber.log.Timber;

import static org.cog.hymnchtv.MainActivity.PREF_MEDIA_HYMN;

/**
 * Class implements the media player UI. It provides the full media playback control e.g. play, pause, stop
 * and select play position etc.
 *
 * @author Eng Chong Meng
 */
@SuppressLint("NonConstantResourceId")
public class MediaController extends Fragment
        implements OnClickListener, OnLongClickListener, SeekBar.OnSeekBarChangeListener, RadioGroup.OnCheckedChangeListener
{
    /**
     * The state of a player where playback is stopped
     */
    private static final int STATE_STOP = 0;
    /**
     * The state of a player when it's created
     */
    private static final int STATE_IDLE = 1;
    /**
     * The state of a player where playback is paused
     */
    private static final int STATE_PAUSE = 2;
    /**
     * The state of a player that is actively playing
     */
    private static final int STATE_PLAY = 3;

    private static String STATE_PLAYER = "statePlayer";

    private int statePlayer;
    private AnimationDrawable mPlayerAnimate;

    private static final Map<Uri, BroadcastReceiver> bcRegisters = new HashMap<>();

    private View playerUi;
    private ImageView playbackPlay;
    private TextView hymnInfo = null;
    private TextView playbackPosition;
    private TextView playbackDuration;
    private SeekBar playbackSeekBar;

    private ProgressBar progressBar = null;
    private final TextView fileLabel = null;
    private TextView fileStatus = null;
    private TextView fileXferError = null;
    private TextView fileXferSpeed = null;
    private TextView estTimeRemain = null;

    private boolean isSeeking = false;
    private int positionSeek;
    private final boolean isMediaAudio = true;
    private final String mimeType = "midi";

    private MediaType mMediaType;

    private SharedPreferences mSharePref;

    private MpBroadcastReceiver mReceiver = null;

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

    private ContentHandler mContentHandler;

    // Need this to prevent crash on rotation if there are other constructors implementation
    // public MediaController()
    // {
    // }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View convertView = inflater.inflate(R.layout.player_ui, container, false);

        playerUi = convertView.findViewById(R.id.playerUi);

        hymnInfo = convertView.findViewById(R.id.hymn_info);
        hymnInfo.setMovementMethod(new ScrollingMovementMethod());
        hymnInfo.setHorizontallyScrolling(true);

        playbackPlay = convertView.findViewById(R.id.playback_play);
        playbackPosition = convertView.findViewById(R.id.playback_position);
        playbackDuration = convertView.findViewById(R.id.playback_duration);
        playbackSeekBar = convertView.findViewById(R.id.playback_seekbar);

        fileStatus = convertView.findViewById(R.id.filexferStatusView);
        fileXferError = convertView.findViewById(R.id.errorView);

        fileXferSpeed = convertView.findViewById(R.id.file_progressSpeed);
        estTimeRemain = convertView.findViewById(R.id.file_estTime);
        progressBar = convertView.findViewById(R.id.file_progressbar);

        if (savedInstanceState != null) {
            statePlayer = savedInstanceState.getInt(STATE_PLAYER);
        }
        else {
            statePlayer = STATE_STOP;
        }

        // Note-5: seek progressBar is not visible and thumb partially clipped with xml default settings.
        // So increase the seekBar height to 16dp i.e. progressBar = 6dp
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && (getContext() != null)) {
            final float scale = getContext().getResources().getDisplayMetrics().density;
            int dp_padding = (int) (16 * scale + 0.5f);

            playbackSeekBar.requestLayout();
            playbackSeekBar.getLayoutParams().height = dp_padding;
        }

        // set to viewHolder default state
        playbackSeekBar.setOnSeekBarChangeListener(this);

        playbackPlay.setOnClickListener(this);
        playbackPlay.setOnLongClickListener(this);

        mPlayerAnimate = (AnimationDrawable) playbackPlay.getBackground();

        RadioGroup hymnTypesGroup = convertView.findViewById(R.id.hymnsGroup);
        hymnTypesGroup.setOnCheckedChangeListener(this);

        return convertView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mContentHandler = (ContentHandler) getContext();
        if (mContentHandler == null)
            return;

        if (mReceiver == null) {
            mReceiver = new MpBroadcastReceiver();
            registerMpBroadCastReceiver();
        }

        hymnInfo.setText(mContentHandler.getHymnInfo());

        // Get the user selected mediaType to playback
        mSharePref = MainActivity.getSharedPref();
        int mediaType = mSharePref.getInt(PREF_MEDIA_HYMN, MediaType.HYMN_MIDI.getValue());
        mMediaType = MediaType.valueOf(mediaType);

        mediaHymns = mContentHandler.getPlayHymn(this.mMediaType);
        for (Uri uri : mediaHymns) {
            mUri = uri;
            playerInit();
        }
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
                startPlay();
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
            stopPlay();
            return true;
        }
        return false;
    }

    public void startPlay()
    {
        // re-fetch play list if it is empty
        if (mediaHymns.isEmpty()) {
            mediaHymns = mContentHandler.getPlayHymn(this.mMediaType);
        }
        for (Uri uri : mediaHymns) {
            mUri = uri;
            playStart();
        }
    }

    public void stopPlay()
    {
        for (Uri uri : mediaHymns) {
            mUri = uri;
            playerStop();
        }

        // Clear the hymns list on stopPlay, allowing playback to fetch new if user changes the hymn
        mediaHymns.clear();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId)
    {
        SharedPreferences.Editor editor = mSharePref.edit();

        RadioButton rb = group.findViewById(checkedId);
        if (null != rb) {
            // Toast.makeText(mActivity, rb.getText(), Toast.LENGTH_SHORT).show();

            switch (checkedId) {
                case R.id.btn_midi:
                    mMediaType = MediaType.HYMN_MIDI;
                    break;
                case R.id.btn_learn:
                    mMediaType = MediaType.HYMN_LEARN;
                    HymnsApp.showToastMessage(R.string.gui_in_development);
                    break;
                case R.id.btn_accplay:
                    mMediaType = MediaType.HYMN_ACCPLAY;
                    HymnsApp.showToastMessage(R.string.gui_in_development);
                    break;
                case R.id.btn_mp3:
                    mMediaType = MediaType.HYMN_MP3;
                    HymnsApp.showToastMessage(R.string.gui_in_development);
                    break;
            }

            editor.putInt(PREF_MEDIA_HYMN, mMediaType.getValue());
            editor.apply();
        }
    }

    /**
     * Initialize the broadcast receiver for the media player (uri).
     * Keep the active bc receiver instance in bcRegisters list to ensure only one bc is registered
     *
     * @param file the media file
     * @return true if init is successful
     */
    private boolean bcReceiverInit(File file)
    {
        //        String mimeType = checkMimeType(file);
        //        if ((mimeType != null) && (mimeType.contains("audio") || mimeType.contains("3gp"))) {
        if (statePlayer == STATE_STOP) {
            BroadcastReceiver bcReceiver;
            if ((bcReceiver = bcRegisters.get(mUri)) != null) {
                LocalBroadcastManager.getInstance(mContentHandler).unregisterReceiver(bcReceiver);
            }

            registerMpBroadCastReceiver();
            bcRegisters.put(mUri, mReceiver);
        }
        return true;
        //        }
        //        return false;
    }

    private void registerMpBroadCastReceiver()
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioBgService.PLAYBACK_STATE);
        filter.addAction(AudioBgService.PLAYBACK_STATUS);
        LocalBroadcastManager.getInstance(mContentHandler).registerReceiver(mReceiver, filter);
    }

    /**
     * Get the active media player status or just media info for the view display;
     * update the view holder content via Broadcast receiver
     */
    private boolean playerInit()
    {
        if (isMediaAudio) {
            if (statePlayer == STATE_STOP) {

                if (!bcReceiverInit(mXferFile))
                    return false;

                Intent intent = new Intent(mContentHandler, AudioBgService.class);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(mUri, mimeType);
                intent.setAction(AudioBgService.ACTION_PLAYER_INIT);
                mContentHandler.startService(intent);
            }
            return true;
        }
        return false;
    }

    /**
     * Stop the current active media player playback
     */
    private void playerStop()
    {
        if (isMediaAudio) {
            if ((statePlayer == STATE_PAUSE) || (statePlayer == STATE_PLAY)) {

                Intent intent = new Intent(mContentHandler, AudioBgService.class);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(mUri, mimeType);
                intent.setAction(AudioBgService.ACTION_PLAYER_STOP);
                mContentHandler.startService(intent);
            }
        }
    }

    /**
     * Toggle audio file playback states:
     * STOP -> PLAY -> PAUSE -> PLAY;
     * long press play button to STOP
     *
     * Proceed to open the file for VIEW if this is not an audio file
     */
    private void playStart()
    {
        Intent intent = new Intent(mContentHandler, AudioBgService.class);
        if (isMediaAudio) {
            if (statePlayer == STATE_PLAY) {
                intent.setData(mUri);
                intent.setAction(AudioBgService.ACTION_PLAYER_PAUSE);
                mContentHandler.startService(intent);
                return;
            }
            else if (statePlayer == STATE_STOP) {
                if (!bcReceiverInit(mXferFile))
                    return;
            }

            intent.setAction(AudioBgService.ACTION_PLAYER_START);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(mUri, mimeType);
            mContentHandler.startService(intent);
            return;
        }

        intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(mUri, mimeType);

        PackageManager manager = mContentHandler.getPackageManager();
        List<ResolveInfo> info = manager.queryIntentActivities(intent, 0);
        if (info.size() == 0) {
            intent.setDataAndType(mUri, "*/*");
        }
        try {
            mContentHandler.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            HymnsApp.showToastMessage(R.string.gui_file_OPEN_NO_APPLICATION);
        }
    }

    /**
     * SeekTo player new start play position
     *
     * @param position seek time position
     */
    private void playerSeek(int position)
    {
        if (isMediaAudio) {
            if (!bcReceiverInit(mXferFile))
                return;

            Intent intent = new Intent(mContentHandler, AudioBgService.class);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(mUri, mimeType);
            intent.putExtra(AudioBgService.PLAYBACK_POSITION, position);
            intent.setAction(AudioBgService.ACTION_PLAYER_SEEK);
            mContentHandler.startService(intent);
        }
    }

    /**
     * Media player BroadcastReceiver to animate and update player view holder info
     */
    private class MpBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // proceed only if it is the playback of the current mUri
            if (mUri == null || !mUri.equals(intent.getParcelableExtra(AudioBgService.PLAYBACK_URI)))
                return;

            int position = intent.getIntExtra(AudioBgService.PLAYBACK_POSITION, 0);
            int audioDuration = intent.getIntExtra(AudioBgService.PLAYBACK_DURATION, 0);

            if ((statePlayer == STATE_PLAY) && AudioBgService.PLAYBACK_STATUS.equals(intent.getAction())) {
                if (!isSeeking)
                    playbackPosition.setText(formatTime(position));
                playbackDuration.setText(formatTime(audioDuration - position));
                playbackSeekBar.setMax(audioDuration);
                playbackSeekBar.setProgress(position);

            }
            else if (AudioBgService.PLAYBACK_STATE.equals(intent.getAction())) {
                AudioBgService.PlaybackState playbackState
                        = (AudioBgService.PlaybackState) intent.getSerializableExtra(AudioBgService.PLAYBACK_STATE);

                Timber.d("Audio playback state: %s (%s/%s): %s", playbackState, position, audioDuration, mUri.getPath());
                switch (playbackState) {
                    case init:
                        statePlayer = STATE_IDLE;
                        playbackDuration.setText(formatTime(audioDuration));
                        playbackPosition.setText(formatTime(0));
                        playbackSeekBar.setMax(audioDuration);
                        playbackSeekBar.setProgress(0);

                        mPlayerAnimate.stop();
                        playbackPlay.setImageResource(R.drawable.ic_play_stop);
                        break;

                    case play:
                        statePlayer = STATE_PLAY;
                        playbackSeekBar.setMax(audioDuration);
                        playerUi.clearAnimation();

                        playbackPlay.setImageDrawable(null);
                        mPlayerAnimate.start();
                        break;

                    case stop:
                        statePlayer = STATE_STOP;
                        bcRegisters.remove(mUri);
                        LocalBroadcastManager.getInstance(mContentHandler).unregisterReceiver(mReceiver);
                        // flow through
                    case pause:
                        if (statePlayer != STATE_STOP) {
                            statePlayer = STATE_PAUSE;
                        }
                        playbackPosition.setText(formatTime(position));
                        playbackDuration.setText(formatTime(audioDuration - position));
                        playbackSeekBar.setMax(audioDuration);
                        playbackSeekBar.setProgress(position);

                        mPlayerAnimate.stop();
                        playbackPlay.setImageResource((statePlayer == STATE_PAUSE)
                                ? R.drawable.ic_play_pause : R.drawable.ic_play_stop);
                        break;
                }
            }
        }
    }

    /**
     * OnSeekBarChangeListener callback interface
     *
     * A SeekBar callback that notifies clients when the progress level has been
     * changed. This includes changes that were initiated by the user through a
     * touch gesture or arrow key/trackball as well as changes that were initiated
     * programmatically.
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
        if (fromUser && (playbackSeekBar == seekBar)) {
            positionSeek = progress;
            playbackPosition.setText(formatTime(progress));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar)
    {
        if (playbackSeekBar == seekBar) {
            isSeeking = true;
        }

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar)
    {
        if (playbackSeekBar == seekBar) {
            playerSeek(positionSeek);
            isSeeking = false;
        }
    }

    /**
     * Format the given time to mm:ss
     *
     * @param time time is ms
     * @return the formatted time string in mm:ss
     */
    private String formatTime(int time)
    {
        // int ms = (time % 1000) / 10;
        int seconds = time / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
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
            hymnInfo.setText(getFileLabel(fileName, fileSize));
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

            String statusText = HymnsApp.getResString(R.string.gui_file_RECEIVE_FAILED, dnLink);
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
