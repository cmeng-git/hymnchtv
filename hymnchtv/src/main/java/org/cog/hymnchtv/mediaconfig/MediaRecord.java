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
package org.cog.hymnchtv.mediaconfig;

import android.os.Environment;
import android.text.TextUtils;

import org.cog.hymnchtv.MediaType;
import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Locale;

import timber.log.Timber;

import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_MAX;

/**
 * The class provide handlers for the media record
 * @see MediaConfig for the format of the media record
 *
 * @author Eng Chong Meng
 */
public class MediaRecord
{
    public static final String DOWNLOAD_FP = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    public static final String DOWNLOAD_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getName();
    public static final String FILEPATH = "filepath";

    private final String mHymnType;
    private final int mHymnNo;
    private final boolean mIsFu;
    private final MediaType mMediaType;
    private String mMediaUri;
    private String mFilePath;

    public MediaRecord(String hymnType, int hymnNo, boolean isFu, MediaType mediaType)
    {
        this(hymnType, hymnNo, isFu, mediaType, null, null);
    }

    public MediaRecord(String hymnType, int hymnNo, boolean isFu, MediaType mediaType, String mediaUri, String filePath)
    {
        mHymnType = hymnType;
        mHymnNo = hymnNo;
        mIsFu = isFu;
        mMediaType = mediaType;
        mMediaUri = mediaUri;
        mFilePath = filePath;
    }

    public void setMediaUri(String mediaUri)
    {
        mMediaUri = mediaUri;
    }

    public void setFilePath(String filePath)
    {
        mFilePath = filePath;
    }

    public String getHymnType()
    {
        return mHymnType;
    }

    public int getHymnNo()
    {
        return mHymnNo;
    }

    public boolean isFu()
    {
        return mIsFu;
    }

    public MediaType getMediaType()
    {
        return mMediaType;
    }

    public String getMediaUri()
    {
        if (TextUtils.isEmpty(mMediaUri) || "null".equalsIgnoreCase(mMediaUri))
            return null;

        return mMediaUri;
    }

    public String getMediaFilePath()
    {
        if (TextUtils.isEmpty(mFilePath) || "null".equalsIgnoreCase(mFilePath))
            return null;

        return mFilePath;
    }

    public String getHymnNoFu()
    {
        String hymnNo = String.format(Locale.CHINA, "%04d", mHymnNo);
        if (mIsFu && mHymnType.equals(HYMN_DB)) {
            hymnNo = "附" + (mHymnNo - HYMN_DB_NO_MAX);
        }
        return hymnNo;
    }

    /**
     * Convert the give string which containing full parameters for conversion to MediaRecord
     *
     * @param mRecord the exported string
     * @return the converted MediaRecord, or null for invalid mRecord string
     */
    public static MediaRecord toRecord(String mRecord)
    {
        mRecord = mRecord.trim().replaceAll("[ ]*[,\t][ ]*", ",");
        String[] recordItem = mRecord.split(",");

        // must has all the 6 parameters of the MediaRecord
        if (recordItem.length < 6)
            return null;

        // Fu hymnNo is added to HYMN_DB_NO_MAX before storing in DB
        boolean isFu = "1".equals(recordItem[2]);
        int hymnNo = Integer.parseInt(recordItem[1]);
        if (isFu && (hymnNo <= HYMN_DB_NO_MAX)) {
            hymnNo += HYMN_DB_NO_MAX;
        }

        return new MediaRecord(
                recordItem[0], hymnNo, isFu,
                Enum.valueOf(MediaType.class, recordItem[3]),
                recordItem[4],
                recordItem[5]);
    }

    /**
     * Convert the media records in the database to an exportable string
     *
     * @return MediaRecord string for database export
     */
    public String toExportString()
    {
        String mediaRecord = null;
        if (getMediaFilePath() != null) {
            mediaRecord = String.format(Locale.CHINA, "%s,%d,%d,%s,%s,%s\r\n",
                    mHymnType, mHymnNo, mIsFu ?1:0, mMediaType, null, mFilePath);
        }
        else if (getMediaUri() != null) {
            mediaRecord = String.format(Locale.CHINA, "%s,%d,%d,%s,%s,%s\r\n",
                    mHymnType, mHymnNo, mIsFu?1:0, mMediaType, mMediaUri, null);
        }
        return mediaRecord;
    }

    /**
     * Convert the database MediaRecord info to user-friendly display list record
     *
     * @return MediaRecord in user readable String
     */
    public @NotNull String toString()
    {
        // Decode the uri link for friendly user UI
        String uriLink = getMediaUri();
        if (uriLink != null) {
            try {
                uriLink = URLDecoder.decode(uriLink, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Timber.w("Exception in url decode %s: %s", uriLink, e.getMessage());
            }
        }

        String filePath = getMediaFilePath();
        if (filePath != null) {
            filePath = filePath.replace(DOWNLOAD_FP, DOWNLOAD_DIR);
        }

        String mediaRecordString;
        if (filePath != null) {
            mediaRecordString = String.format(Locale.CHINA, "%s#%s: %s:\n%s: %s",
                    mHymnType, getHymnNoFu(), mMediaType, FILEPATH, filePath);
        }
        else if (uriLink != null) {
            mediaRecordString = String.format(Locale.CHINA, "%s#%s: %s:\nuri: %s",
                    mHymnType, getHymnNoFu(), mMediaType, uriLink);
        }
        else {
            mediaRecordString = String.format(Locale.CHINA, "%s#%s: %s:\nuri: %s\n%s: %s",
                    mHymnType, getHymnNoFu(), mMediaType, null, FILEPATH, null);
        }
        return mediaRecordString;
    }
}
