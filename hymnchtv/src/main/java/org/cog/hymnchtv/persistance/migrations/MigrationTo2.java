package org.cog.hymnchtv.persistance.migrations;

import static org.cog.hymnchtv.persistance.DatabaseBackend.CREATE_HYMN_HISTORY;

import android.database.sqlite.SQLiteDatabase;

import org.cog.hymnchtv.hymnhistory.HistoryRecord;

public class MigrationTo2 {
    // Create the table for hymn history support
    public static void createHymnHistoryTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + HistoryRecord.TABLE_NAME);
        db.execSQL(CREATE_HYMN_HISTORY);
    }
}
