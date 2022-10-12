package org.cog.hymnchtv.persistance.migrations;

import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_MAX;

import android.database.sqlite.SQLiteDatabase;

import org.apache.http.util.EncodingUtils;
import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.MainActivity;
import org.cog.hymnchtv.R;
import org.cog.hymnchtv.mediaconfig.MediaConfig;
import org.cog.hymnchtv.mediaconfig.MediaRecord;
import org.cog.hymnchtv.persistance.DatabaseBackend;
import org.cog.hymnchtv.utils.HymnNoValidate;
import org.cog.hymnchtv.utils.TimberLog;
import org.cog.hymnchtv.utils.ViewUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import timber.log.Timber;

public class MigrationUrlRecord
{
    private static String[] hymnTypes
            = new String[]{MainActivity.HYMN_DB, MainActivity.HYMN_BB, MainActivity.HYMN_XB, MainActivity.HYMN_ER};

    // Create the table for hymn history support
    public static void purgeHymnUrl(SQLiteDatabase db)
    {
        for (String hymnType : hymnTypes) {
            // String[] args = new String[]{"HYMN_JIAOCHANG"};
            String[] args = new String[]{"HYMN_URL"};
            int count = db.delete(hymnType, MediaConfig.MEDIA_TYPE + "=?", args);
            Timber.d("Purge HYMN_URL records for %s (%s)", hymnType, count);
        }
    }

    /**
     * Import the QQ media records into the database based on asset file info
     */
    public static void importQQRecords(DatabaseBackend mDB)
    {
        String assetFile = "qq_url_import.txt";
        boolean isOverWrite = false;

        HymnsApp.showToastMessage(R.string.gui_db_import_start);
        int record = 0;
        try {
            InputStream ins = HymnsApp.getGlobalContext().getResources().getAssets().open(assetFile);
            byte[] buffer2 = new byte[ins.available()];
            if (ins.read(buffer2) == -1)
                return;

            String mResult = EncodingUtils.getString(buffer2, "utf-8");
            String[] mList = mResult.split("\r\n|\n");

            for (String mRecord : mList) {
                MediaRecord mediaRecord = MediaRecord.toRecord(mRecord);
                if (mediaRecord == null)
                    continue;

                boolean isFu = mediaRecord.isFu();
                int hymnNo = isFu ? (mediaRecord.getHymnNo() - HYMN_DB_NO_MAX) : mediaRecord.getHymnNo();
                int nui = HymnNoValidate.validateHymnNo(mediaRecord.getHymnType(), hymnNo, isFu);
                if ((nui != -1) && (isOverWrite || !mDB.getMediaRecord(mediaRecord, false))) {
                    mDB.storeMediaRecord(mediaRecord);
                    record++;
                }
                if (TimberLog.isFinestEnable)
                    Timber.d("Import media record: %s; %s(%s); %s", nui, hymnNo, record, mRecord);
            }
        } catch (IOException e) {
            Timber.w("Asset file not available: %s", e.getMessage());
        }
        HymnsApp.showToastMessage(R.string.gui_db_import_record, record);
    }
}
