package org.cog.hymnchtv.persistance.migrations;

import static org.cog.hymnchtv.MainActivity.HYMN_XG;
import static org.cog.hymnchtv.persistance.DatabaseBackend.HYMN_CONTENT_STATEMENT;

import android.database.sqlite.SQLiteDatabase;

public class MigrationTo4 {
    public static void addHymnXgTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + HYMN_XG);
        db.execSQL(HYMN_CONTENT_STATEMENT.replace("%s", HYMN_XG));
    }
}
