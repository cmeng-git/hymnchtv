package org.cog.hymnchtv.persistance.migrations;

import static org.cog.hymnchtv.MainActivity.HYMN_YB;
import static org.cog.hymnchtv.persistance.DatabaseBackend.HYMN_CONTENT_STATEMENT;

import android.database.sqlite.SQLiteDatabase;

public class MigrationTo5 {
    public static void addHymnYbTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + HYMN_YB);
        db.execSQL(HYMN_CONTENT_STATEMENT.replace("%s", HYMN_YB));
    }
}
