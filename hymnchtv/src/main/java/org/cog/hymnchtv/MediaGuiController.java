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
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.cog.hymnchtv.service.audioservice.AudioBgService;
import org.cog.hymnchtv.utils.ViewUtil;

import java.util.*;

import timber.log.Timber;

import static org.cog.hymnchtv.MainActivity.PREF_MEDIA_HYMN;
import static org.cog.hymnchtv.MainActivity.PREF_SETTINGS;
import static org.cog.hymnchtv.service.audioservice.AudioBgService.PlaybackState;

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

public class MediaGuiController extends Fragment implements AdapterView.OnItemSelectedListener,
        SeekBar.OnSeekBarChangeListener, RadioGroup.OnCheckedChangeListener
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

    public static final String PREF_PLAYBACK_LOOP = "PlayBack_Loop";
    public static final String PREF_PLAYBACK_LOOPCOUNT = "PlayBack_LoopCount";
    public static final String PREF_PLAYBACK_SPEED = "PlayBack_Speed";

    private static final String PLAYER_STATE = "playerState";
    private static final String PLAYER_INFO = "playerInfo";
    private static final String PLAYER_URIS = "playerUris";

    private int playerState;
    ArrayList<String> playerUris = new ArrayList<>();
    private AnimationDrawable mPlayerAnimate;

    private static final Map<Uri, BroadcastReceiver> bcRegisters = new HashMap<>();

    public ImageView playbackPlay;

    private View playerUi;
    private TextView hymnInfo = null;
    private TextView playbackPosition;
    private TextView playbackDuration;
    private SeekBar playbackSeekBar;
    private Spinner playbackSpeed;
    private CheckBox cbPlaybackLoop;
    private EditText edLoopCount;
    private Button mBtnMedia;

    private boolean isSeeking = false;
    private int positionSeek;
    private final boolean isMediaAudio = true;

    private MediaType mMediaType;

    private SharedPreferences mSharedPref;
    private static SharedPreferences.Editor mEditor;

    private MpBroadcastReceiver mReceiver = null;

    private static final String[] mpSpeedValues = HymnsApp.getAppResources().getStringArray(R.array.mp_speed_value);

    protected Uri mUri;
    List<Uri> mediaHymns = new ArrayList<>();

    private ContentHandler mContentHandler;

    // Need this to prevent crash on rotation if there are other constructors implementation
    // public MediaGuiController()
    // {
    // }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View convertView = inflater.inflate(R.layout.media_player_audio_ui, container, false);
        playerUi = convertView.findViewById(R.id.playerUi);

        hymnInfo = convertView.findViewById(R.id.hymn_info);
        hymnInfo.setMovementMethod(new ScrollingMovementMethod());
        hymnInfo.setHorizontallyScrolling(true);

        playbackPlay = convertView.findViewById(R.id.playback_play);
        playbackPosition = convertView.findViewById(R.id.playback_position);
        playbackDuration = convertView.findViewById(R.id.playback_duration);
        playbackSeekBar = convertView.findViewById(R.id.playback_seekbar);

        cbPlaybackLoop = convertView.findViewById(R.id.playback_loop);
        cbPlaybackLoop.setOnClickListener(v -> onLoopClick());

        edLoopCount = convertView.findViewById(R.id.loopCount);
        edLoopCount.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                onLoopValueChange();
                return true;
            }
            return false;
        });

        playbackSpeed = convertView.findViewById(R.id.playback_speed);
        playbackSpeed.setOnItemSelectedListener(this);

        if (savedInstanceState != null) {
            playerState = savedInstanceState.getInt(PLAYER_STATE);
            String playerInfo = savedInstanceState.getString(PLAYER_INFO);
            hymnInfo.setText(playerInfo);

            playerUris = savedInstanceState.getStringArrayList(PLAYER_URIS);
            for (String uriString : playerUris) {
                mediaHymns.add(Uri.parse(uriString));
            }
        }
        else {
            playerState = STATE_STOP;
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

        // playbackPlay.setOnClickListener(this);
        playbackPlay.setOnClickListener(view -> startPlay());
        playbackPlay.setOnLongClickListener(view -> {
            stopPlay();
            return true;
        });

        mPlayerAnimate = (AnimationDrawable) playbackPlay.getBackground();

        RadioGroup hymnTypesGroup = convertView.findViewById(R.id.hymnsGroup);
        hymnTypesGroup.setOnCheckedChangeListener(this);
        mBtnMedia = convertView.findViewById(R.id.btn_media);

        return convertView;
    }

    @Override
    @SuppressLint("CommitPrefEdits")
    public void onResume()
    {
        super.onResume();
        mContentHandler = (ContentHandler) getContext();
        if (mContentHandler == null)
            return;

        // init and prepare the mediaPlayer state receiver
        if (mReceiver == null) {
            mReceiver = new MpBroadcastReceiver();
            registerMpBroadCastReceiver();
        }

        // Request ContentHandler to update the media gui controller hymn display info and media button text color
        mContentHandler.updateMediaPlayerInfo();

        // Get the user selected mediaType for playback
        mSharedPref = getActivity().getSharedPreferences(PREF_SETTINGS, 0);
        mEditor = mSharedPref.edit();
        if (null == mEditor) {
            Timber.d("SharePref (Editor): %s (%s)", mSharedPref, mEditor);
        }

        int mediaType = mSharedPref.getInt(PREF_MEDIA_HYMN, MediaType.HYMN_BANZOU.getValue());
        mMediaType = MediaType.valueOf(mediaType);
        checkRadioButton(mMediaType);

        // Init user selected playback speed
        String speed = mSharedPref.getString(PREF_PLAYBACK_SPEED, "1.0");
        for (int i = 0; i < mpSpeedValues.length; i++) {
            if (mpSpeedValues[i].equals(speed)) {
                playbackSpeed.setSelection(i);
                break;
            }
        }
        setPlaybackSpeed(speed);

        // Init user selected playback loop parameters (order important)
        boolean isLoop = mSharedPref.getBoolean(PREF_PLAYBACK_LOOP, false);
        cbPlaybackLoop.setChecked(isLoop);
        edLoopCount.setVisibility(isLoop ? View.VISIBLE : View.GONE);

        String loopValue = mSharedPref.getString(PREF_PLAYBACK_LOOPCOUNT, "1");
        edLoopCount.setText(loopValue);
        setPlaybackLoopCount(loopValue);

        // Need this to resume last play state when user changes hymnNo while playing
        for (Uri uri : mediaHymns) {
            mUri = uri;
        }
    }

    /**
     * Need to save all the player state to restore when user return after screen rotation
     * i.e. playerState, playerInfo and playUris
     *
     * @param savedInstanceState for player states
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        savedInstanceState.putInt(PLAYER_STATE, playerState);
        savedInstanceState.putString(PLAYER_INFO, hymnInfo.getText().toString());

        // Need this to resume last play state when user changes hymnNo while playing
        for (Uri uri : mediaHymns) {
            playerUris.add(uri.toString());
        }
        savedInstanceState.putStringArrayList(PLAYER_URIS, playerUris);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Initialize the Media Player UI
     *
     * @param isShow show player UI if true
     */
    public void initPlayerUi(boolean isShow)
    {
        playerUi.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }

    /**
     * Update the player title info with the given text string and
     * Set the Radio Button_Media Text color to Black if user defined media is available, else gray
     *
     * @param info player info
     * @param isAvailable true if use defined media is available
     */
    public void initHymnInfo(String info, boolean isAvailable)
    {
        if (STATE_STOP == playerState) {
            hymnInfo.setText(info);
            mBtnMedia.setTextColor(isAvailable ? Color.BLACK : Color.GRAY);
        }
    }

    /**
     * This is activated by user; or automatic from mediaController when the downloaded uri is completed
     */
    public void startPlay()
    {
        // Set true to test the getPlayHymn algorithms
        boolean test = false;
        if (test) {
            mContentHandler.da_link_test(mMediaType, false);
            return;
        }
        // proceed to fetch the playback uri list if it is empty
        if (mediaHymns.isEmpty()) {
            mediaHymns = mContentHandler.getPlayHymn(mMediaType, true);
        }

        for (Uri uri : mediaHymns) {
            mUri = uri;
            playStart();
        }
        edLoopCount.clearFocus();
    }

    public void stopPlay()
    {
        for (Uri uri : mediaHymns) {
            mUri = uri;
            playerStop();
        }

        /*
         * Do not clear the hymns list on stopPlay, allowing playback to fetch new if user changes the hymn
         * Must let MpBroadcastReceiver perform the required task, else has problem in Stop state update
         */
        // mediaHymns.clear();
        // mContentHandler.initMediaPlayerInfo();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId)
    {
        RadioButton rb = group.findViewById(checkedId);
        if (null != rb) {
            switch (checkedId) {
                case R.id.btn_media:
                    mMediaType = MediaType.HYMN_MEDIA;
                    break;
                case R.id.btn_jiaochang:
                    mMediaType = MediaType.HYMN_JIAOCHANG;
                    break;
                case R.id.btn_banzhou:
                    mMediaType = MediaType.HYMN_BANZOU;
                    break;
                case R.id.btn_changshi:
                    mMediaType = MediaType.HYMN_CHANGSHI;
                    break;
            }

            // Still return NPF?
            try {
                mEditor.putInt(PREF_MEDIA_HYMN, mMediaType.getValue());
                mEditor.apply();
            } catch (NullPointerException e) {
                Timber.e("SharePref NPE: %s (%s)", e.getMessage(), getActivity().getSharedPreferences(PREF_SETTINGS, 0));
            }
        }
    }

    private void checkRadioButton(MediaType mediaType)
    {
        switch (mediaType) {
            case HYMN_MEDIA:
                ((RadioButton) playerUi.findViewById(R.id.btn_media)).setChecked(true);
                break;

            case HYMN_JIAOCHANG:
                ((RadioButton) playerUi.findViewById(R.id.btn_jiaochang)).setChecked(true);
                break;

            case HYMN_BANZOU:
                ((RadioButton) playerUi.findViewById(R.id.btn_banzhou)).setChecked(true);
                break;

            case HYMN_CHANGSHI:
                ((RadioButton) playerUi.findViewById(R.id.btn_changshi)).setChecked(true);
                break;
        }
    }

    /**
     * Initialize the broadcast receiver for the media player (uri).
     * Keep the active bc receiver instance in bcRegisters list to ensure only one bc is registered
     */
    private void bcReceiverInit()
    {
        if (playerState == STATE_STOP) {
            BroadcastReceiver bcReceiver;
            if ((bcReceiver = bcRegisters.get(mUri)) != null) {
                LocalBroadcastManager.getInstance(mContentHandler).unregisterReceiver(bcReceiver);
            }

            registerMpBroadCastReceiver();
            bcRegisters.put(mUri, mReceiver);
        }
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
            if (playerState == STATE_STOP) {
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
            if ((playerState == STATE_PAUSE) || (playerState == STATE_PLAY)) {

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
            if (playerState == STATE_PLAY) {
                intent.setData(mUri);
                intent.setAction(AudioBgService.ACTION_PLAYER_PAUSE);
                mContentHandler.startService(intent);
                return;
            }
            bcReceiverInit();

            intent.setAction(AudioBgService.ACTION_PLAYER_START);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setData(mUri);
            // intent.setTypeAndNormalize("1.0");
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
            bcReceiverInit();

            Intent intent = new Intent(mContentHandler, AudioBgService.class);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setData(mUri);
            intent.putExtra(AudioBgService.PLAYBACK_POSITION, position);
            intent.setAction(AudioBgService.ACTION_PLAYER_SEEK);
            mContentHandler.startService(intent);
        }
    }

    private void setPlaybackSpeed(String speed)
    {
        Intent intent = new Intent(mContentHandler, AudioBgService.class);
        intent.setType(speed);
        intent.setAction(AudioBgService.ACTION_PLAYBACK_SPEED);
        mContentHandler.startService(intent);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        String speed = mpSpeedValues[position];
        setPlaybackSpeed(speed);

        mEditor.putString(PREF_PLAYBACK_SPEED, speed);
        mEditor.apply();
        Timber.d("Set mediaPlayer playback speed to: %sx", speed);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {
    }

    private void onLoopClick()
    {
        boolean isLoop = cbPlaybackLoop.isChecked();
        edLoopCount.setVisibility(isLoop ? View.VISIBLE : View.GONE);

        String loopValue = ViewUtil.toString(edLoopCount);
        setPlaybackLoopCount(loopValue);

        mEditor.putBoolean(PREF_PLAYBACK_LOOP, isLoop);
        mEditor.apply();
    }

    private void onLoopValueChange()
    {
        String loopValue = ViewUtil.toString(edLoopCount);
        loopValue = (loopValue == null) ? "1" : loopValue;

        edLoopCount.setText(loopValue);
        setPlaybackLoopCount(loopValue);

        mEditor.putString(PREF_PLAYBACK_LOOPCOUNT, loopValue);
        mEditor.apply();
    }

    private void setPlaybackLoopCount(String loopValue)
    {
        if (!cbPlaybackLoop.isChecked() || (loopValue == null))
            loopValue = "1";

        Intent intent = new Intent(mContentHandler, AudioBgService.class);
        intent.setType(loopValue);
        intent.setAction(AudioBgService.ACTION_PLAYBACK_LOOP);
        mContentHandler.startService(intent);
    }

    /**
     * The Media player BroadcastReceiver to animate and update player view holder info
     */
    private class MpBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // proceed only if it is the playback of the current Uri
            Uri uri = intent.getParcelableExtra(AudioBgService.PLAYBACK_URI);
            // Timber.d("Audio playback state: %s: %s", intent.getAction(), uri.getPath());
            if (uri == null || !mediaHymns.contains(uri))
                return;

            int position = intent.getIntExtra(AudioBgService.PLAYBACK_POSITION, 0);
            int audioDuration = intent.getIntExtra(AudioBgService.PLAYBACK_DURATION, 0);

            if ((playerState == STATE_PLAY) && AudioBgService.PLAYBACK_STATUS.equals(intent.getAction())) {
                if (!isSeeking)
                    playbackPosition.setText(formatTime(position));
                playbackDuration.setText(formatTime(audioDuration - position));
                playbackSeekBar.setMax(audioDuration);
                playbackSeekBar.setProgress(position);

            }
            else if (AudioBgService.PLAYBACK_STATE.equals(intent.getAction())) {
                PlaybackState playbackState = (PlaybackState) intent.getSerializableExtra(AudioBgService.PLAYBACK_STATE);
                Timber.d("Audio playback state: %s (%s/%s): %s", playbackState, position, audioDuration, uri.getPath());

                switch (playbackState) {
                    case init:
                        playerState = STATE_IDLE;
                        playbackDuration.setText(formatTime(audioDuration));
                        playbackPosition.setText(formatTime(0));
                        playbackSeekBar.setMax(audioDuration);
                        playbackSeekBar.setProgress(0);

                        mPlayerAnimate.stop();
                        playbackPlay.setImageResource(R.drawable.ic_play_stop);
                        break;

                    case play:
                        playerState = STATE_PLAY;
                        playbackSeekBar.setMax(audioDuration);
                        playerUi.clearAnimation();

                        playbackPlay.setImageDrawable(null);
                        mPlayerAnimate.start();
                        break;

                    case stop:
                        playerState = STATE_STOP;
                        /*
                         * actually bcRegisters contains the same receivers i.e. MediaGuiController.this
                         * So can just handle once by first incoming midi uri instance
                         */
                        // bcRegisters.remove(uri);
                        // mediaHymns.remove(uri);
                        // if (mediaHymns.isEmpty())
                        //    LocalBroadcastManager.getInstance(mContentHandler).unregisterReceiver(mReceiver);

                        bcRegisters.clear();

                        // Clear the hymns list on stopPlay, allowing playback to fetch new if user changes the hymn
                        Timber.d("Clear the mediaHymn Uri List");
                        mediaHymns.clear();

                        LocalBroadcastManager.getInstance(mContentHandler).unregisterReceiver(mReceiver);
                        mContentHandler.updateMediaPlayerInfo();
                        // flow through to reset player state

                    case pause:
                        if (playerState != STATE_STOP) {
                            playerState = STATE_PAUSE;
                        }
                        playbackPosition.setText(formatTime(position));
                        playbackDuration.setText(formatTime(audioDuration - position));
                        playbackSeekBar.setMax(audioDuration);
                        playbackSeekBar.setProgress(position);

                        mPlayerAnimate.stop();
                        playbackPlay.setImageResource((playerState == STATE_PAUSE)
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
