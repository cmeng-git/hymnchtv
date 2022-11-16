package org.cog.hymnchtv.persistance.migrations;

import static org.cog.hymnchtv.persistance.DatabaseBackend.CREATE_HYMN_ENGLISH;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_MAX;

import android.database.sqlite.SQLiteDatabase;

import org.apache.http.util.EncodingUtils;
import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.MainActivity;
import org.cog.hymnchtv.R;
import org.cog.hymnchtv.mediaconfig.LyricsEnglishRecord;
import org.cog.hymnchtv.mediaconfig.MediaConfig;
import org.cog.hymnchtv.mediaconfig.MediaRecord;
import org.cog.hymnchtv.persistance.DatabaseBackend;
import org.cog.hymnchtv.utils.HymnNoValidate;
import org.cog.hymnchtv.utils.TimberLog;

import java.io.IOException;
import java.io.InputStream;

import timber.log.Timber;

public class MigrationTo3 {
    public static final String assetUrlFile = "url_import.txt";


    // Create the table for lyrics English support
    public static void createHymnEnglishTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + LyricsEnglishRecord.TABLE_NAME);
        db.execSQL(CREATE_HYMN_ENGLISH);
    }

    // Purge the table old HYMN_URL record
    public static void purgeHymnUrl(SQLiteDatabase db) {
        String[] hymnTypes = new String[]{MainActivity.HYMN_DB, MainActivity.HYMN_BB, MainActivity.HYMN_XB, MainActivity.HYMN_ER};
        for (String hymnType : hymnTypes) {
            String[] args = new String[]{"HYMN_URL"};
            int count = db.delete(hymnType, MediaConfig.MEDIA_TYPE + "=?", args);
            Timber.d("Purge HYMN_URL records for %s (%s)", hymnType, count);
        }
    }

    // Create the table for hymn history support
    public static void purgeHymnJC(DatabaseBackend mdb) {
        String[] hymnTypes = new String[]{MainActivity.HYMN_XB, MainActivity.HYMN_ER};
        SQLiteDatabase db = mdb.getWritableDatabase();

        for (String hymnType : hymnTypes) {
            // String[] args = new String[]{"HYMN_JIAOCHANG"};
            String[] args = new String[]{"HYMN_JIAOCHANG"};
            int count = db.delete(hymnType, MediaConfig.MEDIA_TYPE + "=?", args);
            Timber.d("Purge HYMN_URL records for %s (%s)", hymnType, count);
        }
    }

    /**
     * Import the media records into the database based on asset file info
     */
    public static void importUrlRecords() {
        try {
            InputStream inputStream = HymnsApp.getGlobalContext().getResources().getAssets().open(assetUrlFile);
            MediaConfig.importUrlRecords(inputStream, false);
        } catch (IOException e) {
            Timber.w("Asset file not available: %s", e.getMessage());
        }
    }
}
