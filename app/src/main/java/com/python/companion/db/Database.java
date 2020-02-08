package com.python.companion.db;


import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.python.companion.db.dao.DAONote;
import com.python.companion.db.entity.Note;

@androidx.room.Database(entities = {Note.class}, version = 1, exportSchema = false)
public abstract class Database extends RoomDatabase {

    private static volatile Database INSTANCE;
    public static final String DB_NAME = "app.db";

    public abstract DAONote getDAONote();

    /**
     * Singleton instance getter
     * @param context context to recreate database
     * @return database instance
     */
    public static Database getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (Database.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), Database.class, DB_NAME).build();
                }
            }
        }
        return INSTANCE;
    }
}