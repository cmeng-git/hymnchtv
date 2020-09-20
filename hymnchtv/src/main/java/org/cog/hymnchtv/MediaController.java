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
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.cog.hymnchtv.service.audioservice.AudioBgService;

import java.io.File;
import java.util.*;

import timber.log.Timber;

import static org.cog.hymnchtv.MainActivity.PREF_MEDIA_HYMN;

/**
 * Class implements the media player UI. It provides the full media playback control
 * e.g. play, pause, stop and select play position etc.
 * The UI also includes user selectable media options for the playback i.e. Midi, 教唱, 伴奏 and MP3
 *
 * The UI hymn info and playback are synchronous with the user selected Hymn number.
 * The hymn playing continues, even as user slide to select new hymn; but get updated when the hymn ends or
 * stop by the user.
 *
 * 播放按钮点一下，开始播放媒体档。
 * 再次点播放按钮，播放就会暂停。
 * 再次点播放按钮，从暂停位置继续播放。
 * 长按播放按钮后，可再次从头开始播放。
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

    private boolean isSeeking = false;
    private int positionSeek;
    private final boolean isMediaAudio = true;

    private MediaType mMediaType;

    private SharedPreferences mSharePref;

    private MpBroadcastReceiver mReceiver = null;

    /**
     * The xfer file full path for saving the received file.
     */
    protected File mXferFile;
    protected Uri mUri;
    List<Uri> mediaHymns;

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

        initHymnInfo(mContentHandler.getHymnInfo());

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
     * Initialize the Media Player UI
     *
     * @param isShow show player UI if true
     * @param isPlayEnable Enable play button if has playable content
     */
    public void initPlayerUi(boolean isShow, boolean isPlayEnable)
    {
        playbackPlay.setAlpha(isPlayEnable ? 1.0f : 0.3f);
        playerUi.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }

    /**
     * Update the player title info with the given text string
     *
     * @param info
     */
    public void initHymnInfo(String info)
    {
        if (STATE_STOP == statePlayer) {
            hymnInfo.setText(info);
        }
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
        initHymnInfo(mContentHandler.getHymnInfo());
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
        // String mimeType = checkMimeType(file);
        // if ((mimeType != null) && (mimeType.contains("audio") || mimeType.contains("3gp"))) {
        if (statePlayer == STATE_STOP) {
            BroadcastReceiver bcReceiver;
            if ((bcReceiver = bcRegisters.get(mUri)) != null) {
                LocalBroadcastManager.getInstance(mContentHandler).unregisterReceiver(bcReceiver);
            }

            registerMpBroadCastReceiver();
            bcRegisters.put(mUri, mReceiver);
        }
        return true;
        // }
        // return false;
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
                Intent intent = new Intent(mContentHandler, AudioBgService.class);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setData(mUri);
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
                intent.setData(mUri);
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
            intent.setData(mUri);
            mContentHandler.startService(intent);
            return;
        }

        intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setData(mUri);

        PackageManager manager = mContentHandler.getPackageManager();
        List<ResolveInfo> info = manager.queryIntentActivities(intent, 0);
        if (info.size() == 0) {
            intent.setData(mUri);
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
            intent.setData(mUri);
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

                        mediaHymns.clear();
                        initHymnInfo(mContentHandler.getHymnInfo());
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
            for (Uri uri : mediaHymns) {
                mUri = uri;
                playerSeek(positionSeek);
            }
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
}
