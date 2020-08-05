package com.python.companion.db.migrations;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.python.companion.db.populators.Populator;

public class Migrate {

    public static Migration[] getAll() {
        return new Migration[]{m1_2(), m2_3(), m3_4()};
    }

    public static Migration m1_2() {
        return new Migration(1, 2) {
            @Override
            public void migrate(@NonNull SupportSQLiteDatabase database) {
                database.execSQL("ALTER TABLE Note ADD COLUMN favorite INTEGER DEFAULT 0 NOT NULL");
            }
        };
    }

    public static Migration m2_3() {
        return new Migration(2, 3) {
            @Override
            public void migrate(@NonNull SupportSQLiteDatabase database) {
                database.execSQL("BEGIN TRANSACTION;");
                database.execSQL("DROP TABLE IF EXISTS Measurement");
                database.execSQL("CREATE TABLE IF NOT EXISTS `Measurement` (`measurementID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nameSingular` TEXT NOT NULL, `namePlural` TEXT NOT NULL, `duration` INTEGER, `amount` INTEGER NOT NULL, `precomputedamount` INTEGER NOT NULL, `parentID` INTEGER NOT NULL, `cornerstoneType` TEXT)");
                database.execSQL("COMMIT;");
            }
        };
    }

    public static Migration m3_4() {
        return new Migration(3, 4) {
            @Override
            public void migrate(@NonNull SupportSQLiteDatabase database) {
                database.execSQL("BEGIN TRANSACTION;");
                database.execSQL("CREATE TABLE IF NOT EXISTS `Notify` (`notifyID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `measurementID` INTEGER NOT NULL, `notifyDate` INTEGER NOT NULL, `jubileumDate` INTEGER NOT NULL, `amount` INTEGER NOT NULL, `cornerstoneType` TEXT)");
                database.execSQL("DROP TABLE IF EXISTS Measurement");
                database.execSQL("CREATE TABLE IF NOT EXISTS `Measurement` (`measurementID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nameSingular` TEXT NOT NULL, `namePlural` TEXT NOT NULL, `duration` INTEGER, `amount` INTEGER NOT NULL, `precomputedamount` INTEGER NOT NULL, `parentID` INTEGER NOT NULL, `cornerstoneType` TEXT, `hasNotifications` INTEGER NOT NULL, `canModify` INTEGER NOT NULL)");
                database.execSQL("COMMIT;");
                Populator.populate(database);
            }
        };
    }
}
