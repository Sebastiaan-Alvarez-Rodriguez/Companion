package com.python.companion.db.migrations;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migrate {

    public static Migration[] getAll() {
        return new Migration[]{m1_2()};
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
                //TODO: go from measurement with duration and chrono-unit cornerstonetype to measurement with
                // amount, measurement cornerstonetype, boolean for indicating whether type is precise or not, and still a duration
            }
        };
    }
}
