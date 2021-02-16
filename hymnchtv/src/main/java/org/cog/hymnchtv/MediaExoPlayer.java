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
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Size;
import androidx.fragment.app.FragmentActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.util.MimeTypes;

import org.apache.http.util.TextUtils;
import org.cog.hymnchtv.persistance.FileBackend;

import java.util.ArrayList;
import java.util.List;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;
import timber.log.Timber;

// import java.lang.reflect.Field;

/**
 * The class handles the actual content source address decoding for the user selected hymn
 * see https://developer.android.com/codelabs/exoplayer-intro#0
 *
 * @author Eng Chong Meng
 */
public class MediaExoPlayer extends FragmentActivity implements AdapterView.OnItemSelectedListener
{
    // Tag for the instance state bundle.
    public static final String ATTR_MEDIA_URL = "mediaUrl";
    public static final String ATTR_MEDIA_URLS = "mediaUrls";

    private static final String START_POSITION = "start_position";
    public static final String PB_SPEED = "playback_speed";
    public static final String PB_PITCH = "playback_pitch";

    private static final String sampleUrl = "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4";

    // Start playback video url
    private String mediaUrl = sampleUrl;
    private ArrayList<String> mediaUrls = null;

    // Playback position (in milliseconds).
    private long startPositionMs = 0;
    private float mSpeed = 1.0f;
    private float mPitch = 1.0f;

    private SimpleExoPlayer mExoPlayer = null;
    private StyledPlayerView mPlayerView;
    private PlaybackStateListener playbackStateListener;

    private Spinner playbackSpeed;
    private static final String[] mpSpeedValues = HymnsApp.getAppResources().getStringArray(R.array.mp_speed_value);

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_player_exo_ui);
        mPlayerView = findViewById(R.id.exoplayerView);

        // Need to set text color in Hymnchtv; although ExoStyledControls.ButtonText specifies while
        TextView rewindButtonTextView = findViewById(com.google.android.exoplayer2.ui.R.id.exo_rew_with_amount);
        rewindButtonTextView.setTextColor(Color.WHITE);

        TextView fastForwardButtonTextView = findViewById(com.google.android.exoplayer2.ui.R.id.exo_ffwd_with_amount);
        fastForwardButtonTextView.setTextColor(Color.WHITE);

        playbackSpeed = findViewById(R.id.playback_speed);
        playbackSpeed.setOnItemSelectedListener(this);

        if (savedInstanceState != null) {
            mediaUrl = savedInstanceState.getString(ATTR_MEDIA_URL);
            mediaUrls = savedInstanceState.getStringArrayList(ATTR_MEDIA_URLS);
            startPositionMs = savedInstanceState.getLong(START_POSITION);
            mSpeed = savedInstanceState.getFloat(PB_SPEED);
            mPitch = savedInstanceState.getFloat(PB_PITCH);
        }
        else {
            Bundle bundle = getIntent().getExtras();
            mediaUrl = bundle.getString(ATTR_MEDIA_URL);
            mediaUrls = bundle.getStringArrayList(ATTR_MEDIA_URLS);
        }
        playbackStateListener = new PlaybackStateListener();
    }

    /**
     * Save a copy of the current player info to the instance state bundle for:
     * a. Playback video url
     * b. Current playback position using getCurrentPosition (in milliseconds).
     *
     * if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
     *   onSaveInstanceState is called after onPause but before onStop
     * if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
     *   onSaveInstanceState is called only after onStop
     * So call releasePlayer() in onPause to save startPositionMs = mExoPlayer.getCurrentPosition();
     *
     * @param outState Bundle
     */
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putString(ATTR_MEDIA_URL, mediaUrl);
        outState.putStringArrayList(ATTR_MEDIA_URLS, mediaUrls);
        outState.putLong(START_POSITION, startPositionMs);
        outState.putFloat(PB_SPEED, mSpeed);
        outState.putFloat(PB_PITCH, mPitch);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        hideSystemUi();
        // Load the media each time onStart() is called.
        initializePlayer();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        releasePlayer();
    }

    private void initializePlayer()
    {
        if (mExoPlayer == null) {
            mExoPlayer = new SimpleExoPlayer.Builder(this).build();
            mExoPlayer.addListener(playbackStateListener);
            mPlayerView.setPlayer(mExoPlayer);
        }

        for (int i = 0; i < mpSpeedValues.length; i++) {
            if (mpSpeedValues[i].equals(Float.toString(mSpeed))) {
                playbackSpeed.setSelection(i);
                break;
            }
        }

        if ((mediaUrls == null) || mediaUrls.isEmpty()) {
            MediaItem mediaItem = buildMediaItem(mediaUrl);
            if (mediaItem != null)
                playMedia(mediaItem);
        }
        else {
            playVideoUrls();
        }
    }

    /**
     * Prepare and play the specified mediaItem
     *
     * @param mediaItem for playback
     */
    private void playMedia(MediaItem mediaItem)
    {
        if (mediaItem != null) {
            mExoPlayer.setMediaItem(mediaItem, startPositionMs);
            setSpeedPitch(mSpeed, mPitch);
            mExoPlayer.setPlayWhenReady(true);
            mExoPlayer.prepare();
        }
    }

    /**
     * Prepare and playback a list of given video URLs if not empty
     */
    private void playVideoUrls()
    {
        if ((mediaUrls != null) && !mediaUrls.isEmpty()) {
            List<MediaItem> mediaItems = new ArrayList<>();
            for (String tmpUrl : mediaUrls) {
                mediaItems.add(buildMediaItem(tmpUrl));
            }
            mExoPlayer.setMediaItems(mediaItems);
            mExoPlayer.setPlayWhenReady(true);
            mExoPlayer.prepare();
        }
    }

    /**
     * Build and return the mediaItem or
     * Proceed to play if it is a youtube link; return null;
     *
     * @param mediaUrl for building the mediaItem
     * @return built mediaItem
     */
    private MediaItem buildMediaItem(String mediaUrl)
    {
        MediaItem mediaItem = null;

        Uri uri = Uri.parse(mediaUrl);
        String mimeType = FileBackend.getMimeType(this, uri);
        if (!TextUtils.isEmpty(mimeType) && (mimeType.contains("video") || mimeType.contains("audio"))) {
            mediaItem = MediaItem.fromUri(mediaUrl);
        }
        else if (mediaUrl.matches("http[s]*://[w.]*youtu[.]*be.*")) {
            playYoutubeUrl(mediaUrl);
        }
        else {
            mediaItem = new MediaItem.Builder()
                    .setUri(mediaUrl)
                    .setMimeType(MimeTypes.APPLICATION_MPD)
                    .build();
        }
        return mediaItem;
    }

    /**
     * see https://github.com/HaarigerHarald/android-youtubeExtractor
     * see https://github.com/flagbug/YoutubeExtractor
     *
     * @param youtubeLink the given youtube playback link
     */
    @SuppressLint("StaticFieldLeak")
    private void playYoutubeUrl(String youtubeLink)
    {
        //        try {
        //            Field field = YouTubeExtractor.class.getDeclaredField("LOGGING");
        //            field.setAccessible(true);
        //            field.set(field, true);
        //        } catch (NoSuchFieldException | IllegalAccessException e) {
        //            Timber.w("Exception: %s", e.getMessage());
        //        }

        try {
            new YouTubeExtractor(this)
            {
                @Override
                public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta)
                {
                    if (ytFiles != null) {
                        int itag = ytFiles.keyAt(0); //22; get the first available itag
                        String downloadUrl = ytFiles.get(itag).getUrl();
                        MediaItem mediaItem = MediaItem.fromUri(downloadUrl);
                        playMedia(mediaItem);
                    }
                    else {
                        HymnsApp.showToastMessage(R.string.gui_error_playback);
                        playVideoUrlExt(youtubeLink);
                    }
                }
            }.extract(youtubeLink, true, true);
        } catch (Exception e) {
            Timber.e("YouTubeExtractor Exception: %s", e.getMessage());
        }
    }

    /**
     * Media play-back takes a lot of resources, so everything should be stopped and released at this time.
     * Release all media-related resources. In a more complicated app this
     * might involve unregistering listeners or releasing audio focus.
     */
    private void releasePlayer()
    {
        if (mExoPlayer != null) {
            // Timber.d("Media Player stopping: %s", mExoPlayer);
            startPositionMs = mExoPlayer.getCurrentPosition();
            mExoPlayer.setPlayWhenReady(false);
            mExoPlayer.removeListener(playbackStateListener);
            mExoPlayer.release();
            mExoPlayer = null;
        }
    }

    /**
     * Playback speed
     * *
     *
     * @param speed playback speed: default 1.0f
     * @param pitch audio pitch，default 1.0f
     */
    public void setSpeedPitch(@Size(min = 0) float speed, @Size(min = 0) float pitch)
    {
        mSpeed = speed;
        mPitch = pitch;
        PlaybackParameters playbackParameters = new PlaybackParameters(speed, pitch);
        if (mExoPlayer != null) {
            mExoPlayer.setPlaybackParameters(playbackParameters);
        }
    }

    @Override
    // (view == null) when rotated on first call, follow by second call with value
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        if (view != null) {
            ((TextView) view).setTextColor(getResources().getColor(R.color.textColorWhite));
            ((TextView) view).setTextSize(14);
            mSpeed = Float.parseFloat(mpSpeedValues[position]);
            setSpeedPitch(mSpeed, mPitch);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {
    }

    /**
     * playback in full screen
     */
    private void hideSystemUi()
    {
        mPlayerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    /**
     * ExoPlayer playback state listener
     */
    private class PlaybackStateListener implements Player.EventListener
    {
        @Override
        public void onPlaybackStateChanged(int playbackState)
        {
            switch (playbackState) {
                case ExoPlayer.STATE_IDLE:
                    HymnsApp.showToastMessage(R.string.gui_error_playback);
                    break;

                case ExoPlayer.STATE_BUFFERING:
                    break;

                case ExoPlayer.STATE_READY:
                    break;

                case ExoPlayer.STATE_ENDED:
                    HymnsApp.showToastMessage(R.string.gui_playback_completed);
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * Play the specified videoUrl using android Intent.ACTION_VIEW
     *
     * @param videoUrl videoUrl not playable by ExoPlayer
     */
    private void playVideoUrlExt(String videoUrl)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}

