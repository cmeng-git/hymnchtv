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
     * Convert the database media record info to user-friendly display in List record
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

        String mediaRecord;
        if (filePath != null) {
            mediaRecord = String.format(Locale.CHINA, "%s#%s: %s:\n%s: %s",
                    mHymnType, getHymnNoFu(), mMediaType, FILEPATH, filePath);
        }
        else if (uriLink != null) {
            mediaRecord = String.format(Locale.CHINA, "%s#%s: %s:\nuri: %s",
                    mHymnType, getHymnNoFu(), mMediaType, uriLink);
        }
        else {
            mediaRecord = String.format(Locale.CHINA, "%s#%s: %s:\nuri: %s\n%s: %s",
                    mHymnType, getHymnNoFu(), mMediaType, null, FILEPATH, null);
        }
        return mediaRecord;
    }

    public static MediaRecord toRecord(String mRecord)
    {
        mRecord = mRecord.trim().replaceAll("[ ]*[,\t][ ]*", ",");
        String[] recordItem = mRecord.split(",");

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
     * Convert the media records in the database a exportable string
     *
     * @return MediaRecord string for database export
     */
    public String getExportString()
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
}
