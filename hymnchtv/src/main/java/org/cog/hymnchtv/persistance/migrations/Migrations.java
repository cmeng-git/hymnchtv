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
package org.cog.hymnchtv.persistance.migrations;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

public class Migrations
{
    @SuppressWarnings("fallthrough")
    public static void upgradeDatabase(@NonNull SQLiteDatabase db, MigrationsHelper migrationsHelper) {
        switch (db.getVersion()) {
            case 1:
                MigrationTo2.createHymnHistoryTable(db);
            case 2:
                // Purge and relocate all qq records; access via jiaoChang button for >= v1.7.6 release
                MigrationTo3.purgeHymnUrl(db);
                MigrationTo3.createHymnEnglishTable(db);
            case 3:
                MigrationTo4.addHymnXgTable(db);
        }
    }
}
