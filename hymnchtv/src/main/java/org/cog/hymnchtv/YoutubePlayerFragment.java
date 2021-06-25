package org.cog.hymnchtv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerFullScreenListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerUtils;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.ui.menu.MenuItem;

import org.cog.hymnchtv.utils.FullScreenHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Random;

import timber.log.Timber;

import static org.cog.hymnchtv.MainActivity.PREF_SETTINGS;
import static org.cog.hymnchtv.MediaExoPlayerFragment.ATTR_MEDIA_URL;
import static org.cog.hymnchtv.MediaExoPlayerFragment.ATTR_MEDIA_URLS;
import static org.cog.hymnchtv.MediaGuiController.PREF_PLAYBACK_SPEED;

public class YoutubePlayerFragment extends Fragment
{
    private YouTubePlayerView youTubePlayerView;
    private FullScreenHelper fullScreenHelper;

    private String mediaUrl = null;
    private static ArrayList<String> mediaUrls = new ArrayList<>();
    static {
        mediaUrls.add("vCKCkc8llaM");
        mediaUrls.add("LvetJ9U_tVY");
        mediaUrls.add("S0Q4gqBUs7c");
        mediaUrls.add("zOa-rSM4nms");
    }

    // Playback ratio of normal speed constant.
    private static final float rateMin = 0.6f;
    private static float rateMax = 1.4f;
    private static final float rateStep = 0.1f;
    private float mSpeed = 1.0f;

    // private static final String[] mpSpeedValues = HymnsApp.getAppResources().getStringArray(R.array.mp_speed_value);

    private FragmentActivity mContext;
    private SharedPreferences mSharedPref;

    public YoutubePlayerFragment()
    {
    }

    @Override
    public void onAttach(@NonNull @NotNull Context context)
    {
        super.onAttach(context);
        mContext = (FragmentActivity) context;
        fullScreenHelper = new FullScreenHelper(mContext);
    }

    /**
     * Create a new instance of MediaExoPlayerFragment, providing "bundle" as an argument.
     */
    public static YoutubePlayerFragment getInstance(Bundle args)
    {
        YoutubePlayerFragment youtubePlayer = new YoutubePlayerFragment();
        youtubePlayer.setArguments(args);
        return youtubePlayer;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.youtube_player_fragment, container, false);
        youTubePlayerView = view.findViewById(R.id.youtube_player_view);

        Bundle args = getArguments();
        if (args != null) {
            mediaUrl = args.getString(ATTR_MEDIA_URL);
            mediaUrls = args.getStringArrayList(ATTR_MEDIA_URLS);
        }
        initYouTubePlayerView();
        mSharedPref = mContext.getSharedPreferences(PREF_SETTINGS, 0);
        return view;
    }

    private void initYouTubePlayerView()
    {
        // hymnchtv crashes when enabled
        // initPlayerMenu();

        // The player will automatically release itself when the fragment is destroyed.
        // The player will automatically pause when the fragment is stopped
        // If you don't add YouTubePlayerView as a lifecycle observer, you will have to release it manually.
        getLifecycle().addObserver(youTubePlayerView);

        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener()
        {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer)
            {
                String videoId = mediaUrl.substring(mediaUrl.lastIndexOf('/') + 1);
                YouTubePlayerUtils.loadOrCueVideo(youTubePlayer, getLifecycle(), videoId, 0f);

                addActionsToPlayer(youTubePlayer);
                initPlaybackSpeed(youTubePlayer);
                addFullScreenListenerToPlayer();
            }

            @Override
            public void onError(YouTubePlayer youTubePlayer, PlayerConstants.PlayerError error)
            {
                Timber.w("Youtube url: %s, playback failed: %s", mediaUrl, error);
                // Error message will be shown in player view
                // HymnsApp.showToastMessage(R.string.gui_error_playback, error);
                // playVideoUrlExt(mediaUrl);
            }

            @Override
            public void onPlaybackRateChange(YouTubePlayer youTubePlayer, String rate)
            {
                mSpeed = Float.parseFloat(rate);
                Toast.makeText(mContext, mContext.getString(R.string.gui_playback_rate, rate), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Shows the menu button in the player and adds an item to it.
     */
    private void initPlayerMenu()
    {
        youTubePlayerView.getPlayerUiController()
                .showMenuButton(true)
                .getMenu()
                .addItem(new MenuItem("menu item1", R.drawable.ic_speed,
                        view -> Toast.makeText(mContext, "item1 clicked", Toast.LENGTH_SHORT).show())
                )
                .addItem(new MenuItem("menu item2", R.drawable.ic_mood_black_24dp,
                        view -> Toast.makeText(mContext, "item2 clicked", Toast.LENGTH_SHORT).show())
                )
                .addItem(new MenuItem("menu item no icon",
                        view -> Toast.makeText(mContext, "item no icon clicked", Toast.LENGTH_SHORT).show())
                );
    }

    private void addFullScreenListenerToPlayer()
    {
        youTubePlayerView.addFullScreenListener(new YouTubePlayerFullScreenListener()
        {
            @Override
            public void onYouTubePlayerEnterFullScreen()
            {
                fullScreenHelper.enterFullScreen();
            }

            @Override
            public void onYouTubePlayerExitFullScreen()
            {
                fullScreenHelper.exitFullScreen();
            }
        });
    }

    /**
     * This method adds a new custom action to the player.
     * Custom actions are shown next to the Play/Pause button in the middle of the player.
     */
    private void addActionsToPlayer(YouTubePlayer youTubePlayer)
    {
        Drawable rewindActionIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_rewind);
        Drawable fordwardActionIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_forward);
        Drawable nextActionIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_next);

        Drawable rateIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_speed);

        assert rewindActionIcon != null;
        assert fordwardActionIcon != null;
        assert nextActionIcon != null;
        assert rateIcon != null;

        youTubePlayerView.getPlayerUiController().setRewindAction(rewindActionIcon,
                view -> youTubePlayer.advanceTo(-5.0f));

        youTubePlayerView.getPlayerUiController().setFordwardAction(fordwardActionIcon,
                view -> youTubePlayer.advanceTo(15.0f));

        // Set a click listener on the "Play next video" button
        if (mediaUrls != null && !mediaUrls.isEmpty()) {
            youTubePlayerView.getPlayerUiController().setNextAction(nextActionIcon,
                    view -> YouTubePlayerUtils.loadOrCueVideo(youTubePlayer, getLifecycle(), getNextVideoId(), 0f));
        }

        youTubePlayerView.getPlayerUiController().setRateIncAction(rateIcon,
                view -> {
                    float tmp = mSpeed + rateStep;
                    float rate = tmp <= rateMax ? tmp : mSpeed;
                    youTubePlayer.setPlaybackRate(rate);
                });

        youTubePlayerView.getPlayerUiController().setRateDecAction(rateIcon,
                view -> {
                    float tmp = mSpeed - rateStep;
                    float rate = tmp >= rateMin ? tmp : mSpeed;
                    youTubePlayer.setPlaybackRate(rate);
                });
    }

    /**
     * Initialize the Media Player playback speed to the user defined setting
     */
    public void initPlaybackSpeed(YouTubePlayer youTubePlayer)
    {
        String speed = mSharedPref.getString(PREF_PLAYBACK_SPEED, "1.0");
        mSpeed = Float.parseFloat(speed);

        //        for (int i = 0; i < mpSpeedValues.length; i++) {
        //            if (mpSpeedValues[i].equals(speed)) {
        //                 HymnsApp.showToastMessage("Set playback rate to: " + speed);
        //                 playbackSpeed.setSelection(i);
        //                break;
        //            }
        //        }
        youTubePlayer.setPlaybackRate(mSpeed);
    }

    private String getNextVideoId()
    {
        Random random = new Random();
        String videoUrl;
        if (mediaUrls != null && !mediaUrls.isEmpty()) {
            videoUrl = mediaUrls.get(random.nextInt(mediaUrls.size()));
        }
        else {
            videoUrl = mediaUrl;
        }
        Timber.d("Get next video ID: %s", videoUrl);
        return videoUrl.substring(videoUrl.lastIndexOf('/') + 1);
    }

    /**
     * Play the specified videoUrl using android Intent.ACTION_VIEW
     *
     * @param videoUrl videoUrl not playable by ExoPlayer
     */
    private void playVideoUrlExt(String videoUrl)
    {
        // remove the youtube player fragment
        mContext.getSupportFragmentManager().beginTransaction().remove(this).commit();

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Manually release the youtube player when user pressing backKey
     */
    public void release()
    {
        // Audio media player speed is (0.5 > mSpeed < 1.5)
        SharedPreferences.Editor mEditor = mSharedPref.edit();
        if ((mEditor != null) && (mSpeed >= rateMin && mSpeed <= rateMax)) {
            String speed = Float.toString(mSpeed);
            mEditor.putString(PREF_PLAYBACK_SPEED, speed);
            mEditor.apply();
        }
        youTubePlayerView.release();
    }
}
