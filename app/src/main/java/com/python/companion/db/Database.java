package com.python.companion.db;


import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.python.companion.db.dao.DAOCategory;
import com.python.companion.db.dao.DAOMeasurement;
import com.python.companion.db.dao.DAONote;
import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Measurement;
import com.python.companion.db.entity.Note;
import com.python.companion.db.typeconverters.DateConverter;
import com.python.companion.db.typeconverters.DurationConverter;

@androidx.room.Database(entities = {Note.class, Category.class, Measurement.class}, version = 1, exportSchema = false)
@TypeConverters({DateConverter.class, DurationConverter.class})
public abstract class Database extends RoomDatabase {

    private static volatile Database INSTANCE;
    public static final String DB_NAME = "app.db";

    public abstract DAONote getDAONote();
    public abstract DAOCategory getDAOCategory();
    public abstract DAOMeasurement getDAOMeasurement();

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