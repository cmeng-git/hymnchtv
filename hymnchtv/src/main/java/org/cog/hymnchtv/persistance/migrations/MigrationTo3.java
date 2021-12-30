package org.cog.hymnchtv.persistance.migrations;

import static org.cog.hymnchtv.persistance.DatabaseBackend.CREATE_HYMN_QQ_LINK;

import android.database.sqlite.SQLiteDatabase;

import org.cog.hymnchtv.MainActivity;

public class MigrationTo3
{
    // Create the table for hymn history support
    public static void createHymnQQTable(SQLiteDatabase db)
    {
        db.execSQL("DROP TABLE IF EXISTS " + MainActivity.HYMN_QQ);
        db.execSQL(CREATE_HYMN_QQ_LINK);
    }
}
