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
}
