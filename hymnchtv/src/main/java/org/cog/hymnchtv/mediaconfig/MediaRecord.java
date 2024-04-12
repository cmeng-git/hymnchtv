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

import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_MAX;

import android.os.Environment;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Locale;

import org.cog.hymnchtv.MediaType;
import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

/**
 * The class provide handlers for the media record
 *
 * @author Eng Chong Meng
 * @see MediaConfig for the format of the media record
 */
public class MediaRecord {
    public static final String DOWNLOAD_FP = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    public static final String DOWNLOAD_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getName();

    protected final String mHymnType;
    protected final int mHymnNo;
    protected final boolean mIsFu;
    protected MediaType mMediaType;
    protected String mMediaUri;
    protected String mFilePath;

    public MediaRecord(String hymnType, int hymnNo, boolean isFu, MediaType mediaType) {
        this(hymnType, hymnNo, isFu, mediaType, null, null);
    }

    public MediaRecord(String hymnType, int hymnNo, boolean isFu, MediaType mediaType, String mediaUri, String filePath) {
        mHymnType = hymnType;
        mHymnNo = hymnNo;
        mIsFu = isFu;
        mMediaType = mediaType;
        mMediaUri = mediaUri;
        mFilePath = filePath;
    }

    public void setMediaUri(String mediaUri) {
        mMediaUri = mediaUri;
    }

    public void setFilePath(String filePath) {
        mFilePath = filePath;
    }

    public String getHymnType() {
        return mHymnType;
    }

    public int getHymnNo() {
        return mHymnNo;
    }

    public boolean isFu() {
        return mIsFu;
    }

    public static boolean isFu(String hymnType, int hymnNo) {
        return HYMN_DB.equals(hymnType) && (hymnNo > HYMN_DB_NO_MAX);
    }

    public MediaType getMediaType() {
        return mMediaType;
    }

    public String getMediaUri() {
        if (TextUtils.isEmpty(mMediaUri) || "null".equalsIgnoreCase(mMediaUri))
            return null;

        return mMediaUri;
    }

    public String getMediaFilePath() {
        if (TextUtils.isEmpty(mFilePath) || "null".equalsIgnoreCase(mFilePath))
            return null;

        return mFilePath;
    }

    /**
     * 附 hymn number must always greater than HYMN_DB_NO_MAX in DB database.
     *
     * @return 附x or the actual hymno
     */
    public String getHymnNoFu() {
        String hymnNo = String.format(Locale.CHINA, "%04d", mHymnNo);
        if (mIsFu && mHymnType.equals(HYMN_DB)) {
            hymnNo = "附" + (mHymnNo - HYMN_DB_NO_MAX);
        }
        return hymnNo;
    }

    /**
     * Convert the give string which containing full parameters for conversion to MediaRecord
     * <p>
     * // ListView item for HYMN_JIAOCHANG and HYMN_MEDIA
     * hymn_db:#0001: HYMN_JIAOCHANG\n
     * uri: http://mp.weixin.qq.com/s?__biz=MzUwOTc2ODcxNA==&amp;amp;mid=2247486824&amp;amp;idx=5&amp;amp;sn=97d137a5a4ddb1b0b778087ac84770b7&amp;amp;chksm=f90c680dce7be11b4a3445af8dbe636b5a3d921ce910427609684bff2e5d95d0cda0696af419&amp;amp;scene=21#wechat_redirect\n
     * fp: null
     * <p>
     * hymn_db:#0002: HYMN_MEDIA
     * uri: https://youtu.be/DDvUVzR2-_Q
     * fp: null
     *
     * @param mString can be the exported string or ListView string
     *
     * @return the converted MediaRecord, or null for invalid mRecord string
     */
    public static MediaRecord toRecord(String mString) {
        if (TextUtils.isEmpty(mString))
            return null;

        // Use to split either a MediaRecord ListView item ":[# ]|\\nuri: |\\nfp: ", OR an exported string "[ ]*[,\\t][ ]*"
        String[] recordItem = mString.trim().split(":[# ]|\\nuri: |\\nfp: |[ ]*[,\\t][ ]*");

        // must has at least 5 parameters to create the MediaRecord
        if (recordItem.length < 5)
            return null;

        String hymnType = recordItem[0];
        String sHymnNo = recordItem[1];

        // Different algorithm to determine if isFu record
        boolean isFu = (recordItem.length == 6) && "1".equals(recordItem[2]);
        if (sHymnNo.startsWith("附")) {
            sHymnNo = sHymnNo.substring(1);
            isFu = true;
        }
        // Fu hymnNo is added to HYMN_DB_NO_MAX before storing in DB
        int hymnNo = Integer.parseInt(sHymnNo);
        if (isFu && (hymnNo <= HYMN_DB_NO_MAX)) {
            hymnNo += HYMN_DB_NO_MAX;
        }
        else {
            isFu = isFu(hymnType, hymnNo);
        }

        // MediaRecord exported string
        if (recordItem.length == 6) {
            return new MediaRecord(
                    hymnType, hymnNo, isFu,
                    Enum.valueOf(MediaType.class, recordItem[3]),
                    recordItem[4],
                    recordItem[5]);
        }
        // ListView has only 5 parameters i.e. isFu is not shown
        else {
            return new MediaRecord(
                    hymnType, hymnNo, isFu,
                    Enum.valueOf(MediaType.class, recordItem[2]),
                    recordItem[3],
                    recordItem[4]);
        }
    }

    /**
     * Convert the media records in the database to a string formatted for later import
     *
     * @return MediaRecord string for database export
     */
    public String toExportString() {
        return String.format(Locale.CHINA, "%s,%d,%d,%s,%s,%s\r\n",
                mHymnType, mHymnNo, mIsFu ? 1 : 0, mMediaType, getMediaUri(), getMediaFilePath());
    }

    /**
     * Convert the database MediaRecord info to user-friendly display list record
     *
     * @return MediaRecord in user readable String
     */
    public @NotNull String toString() {
        // Decode the uri link for friendly user UI
        String uriLink = getMediaUri();
        if (uriLink != null) {
            try {
                uriLink = URLDecoder.decode(uriLink, "UTF-8");
            } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                Timber.w("Exception in url decode %s: %s", uriLink, e.getMessage());
            }
        }

        String filePath = getMediaFilePath();
        if (filePath != null) {
            filePath = filePath.replace(DOWNLOAD_FP, DOWNLOAD_DIR);
        }

        return String.format(Locale.CHINA, "%s:#%s: %s\nuri: %s\nfp: %s",
                mHymnType, getHymnNoFu(), mMediaType, uriLink, filePath);
    }
}
