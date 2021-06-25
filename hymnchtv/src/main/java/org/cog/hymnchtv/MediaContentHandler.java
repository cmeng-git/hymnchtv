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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.URLUtil;

import androidx.fragment.app.Fragment;

import org.apache.http.util.TextUtils;
import org.cog.hymnchtv.mediaconfig.MediaRecord;
import org.cog.hymnchtv.persistance.DatabaseBackend;
import org.cog.hymnchtv.persistance.FileBackend;

import java.io.File;
import java.util.List;

import timber.log.Timber;

import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MediaExoPlayerFragment.ATTR_MEDIA_URL;
import static org.cog.hymnchtv.MediaExoPlayerFragment.URL_YOUTUBE;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_MAX;

/**
 * The class handles the actual content source address decoding for the user selected hymn
 *
 * @author Eng Chong Meng
 */
public class MediaContentHandler
{
    private final DatabaseBackend mDB = DatabaseBackend.getInstance(HymnsApp.getGlobalContext());

    private final Context mContext = HymnsApp.getGlobalContext();

    private static ContentHandler mContentHandler;
    private MediaExoPlayerFragment mExoPlayer;
    private YoutubePlayerFragment mYoutubePlayer;

    public static MediaContentHandler getInstance(ContentHandler contentHandler)
    {
        mContentHandler = contentHandler;
        return new MediaContentHandler();
    }

    /**
     * Get the hymn media information:
     * a. play back locally if it is a video media OR
     * b. return the uriList if available for the media content handler.
     *
     * @return true if already handled locally in playback or uriList is not empty
     */
    public boolean getMediaUris(String hymnTable, int hymnNo, MediaType mediaType, List<Uri> uriList)
    {
        boolean isHandled;

        boolean isFu = hymnTable.equals(HYMN_DB) && (hymnNo > HYMN_DB_NO_MAX);
        MediaRecord mediaRecord = new MediaRecord(hymnTable, hymnNo, isFu, mediaType);
        if (mDB.getMediaRecord(mediaRecord, true)) {
            if (!(isHandled = getUriList(mediaRecord.getMediaFilePath(), uriList))) {
                isHandled = getUriList(mediaRecord.getMediaUri(), uriList);
            }
            return (isHandled || !uriList.isEmpty());
        }
        return false;
    }

    private boolean getUriList(String uriString, List<Uri> uriList)
    {
        if (!TextUtils.isEmpty(uriString)) {
            // uriString is an external URL.
            if (URLUtil.isValidUrl(uriString)) {
                playMediaUrl(uriString);
                return true;
            }
            File uriFile = new File(uriString);
            if (uriFile.exists()) {
                Uri uri = Uri.parse(uriString);
                String mimeType = FileBackend.getMimeType(mContext, uri);
                // Let playMediaUrl handle unknown or video mimeType
                if (TextUtils.isEmpty(mimeType) || mimeType.contains("video")) {
                    playMediaUrl(uriString);
                    return true;
                }
                // Otherwise, pass handling to local audio service
                else {
                    uriList.add(uri);
                    return true;
                }
            }
        }
        return false;
    }

    public void playMediaUrl(String videoUrl)
    {
        Uri uri = Uri.parse(videoUrl);
        String mimeType = FileBackend.getMimeType(mContext, uri);
        if ((!TextUtils.isEmpty(mimeType) && (mimeType.contains("video") || mimeType.contains("audio")))
                || videoUrl.matches(URL_YOUTUBE)) {
            playEmbeddedExo(videoUrl);
        }
        else {
            Intent openIntent = new Intent(Intent.ACTION_VIEW);
            openIntent.setDataAndType(uri, mimeType);
            openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PackageManager manager = mContext.getPackageManager();
            List<ResolveInfo> info = manager.queryIntentActivities(openIntent, 0);
            if (info.size() == 0) {
                openIntent.setDataAndType(uri, "*/*");
            }
            try {
                mContext.startActivity(openIntent);
            } catch (ActivityNotFoundException e) {
                // showToastMessage(R.string.service_gui_FILE_OPEN_NO_APPLICATION);
            }
        }
    }

    /**
     * Playback video in embedded fragment for lyrics coexistence
     *
     * @param videoUrl url for playback
     */
    private void playEmbeddedExo(String videoUrl)
    {
        Bundle bundle = new Bundle();
        bundle.putString(ATTR_MEDIA_URL, videoUrl);

        View playerContainer = mContentHandler.findViewById(R.id.player_container);
        playerContainer.setVisibility(View.VISIBLE);
        mContentHandler.showPlayerUi(false);

        if (videoUrl.matches(URL_YOUTUBE)) {
            mYoutubePlayer = YoutubePlayerFragment.getInstance(bundle);
            mContentHandler.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.player_container, mYoutubePlayer)
                    .addToBackStack(null)
                    .commit();
        }
        else {
            mExoPlayer = MediaExoPlayerFragment.getInstance(bundle);
            mContentHandler.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.player_container, mExoPlayer)
                    .addToBackStack(null)
                    .commit();
        }
    }

    /**
     * Release the exoPlayer resource on end
     */
    public void releasePlayer()
    {
        // remove the exoPlayer fragment
        Fragment playerView = mContentHandler.getSupportFragmentManager().findFragmentById(R.id.player_container);
        mContentHandler.getSupportFragmentManager().beginTransaction().remove(playerView).commit();

        if (mExoPlayer != null) {
            mExoPlayer.releasePlayer();
            mExoPlayer = null;
        }
        else if (mYoutubePlayer != null) {
            mYoutubePlayer.release();
            mYoutubePlayer = null;
        }
    }
}

