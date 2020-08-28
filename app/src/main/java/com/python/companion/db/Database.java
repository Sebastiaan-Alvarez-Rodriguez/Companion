package com.python.companion.db;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.python.companion.db.dao.DAOCategory;
import com.python.companion.db.dao.DAOAnniversary;
import com.python.companion.db.dao.DAONote;
import com.python.companion.db.dao.DAOMessage;
import com.python.companion.db.entity.Anniversary;
import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;
import com.python.companion.db.entity.Message;
import com.python.companion.db.migrations.Migrate;
import com.python.companion.db.populators.AnniversaryPopulator;
import com.python.companion.db.typeconverters.ChronoUnitConverter;
import com.python.companion.db.typeconverters.DurationConverter;
import com.python.companion.db.typeconverters.InstantConverter;
import com.python.companion.db.typeconverters.LocalDateConverter;

@androidx.room.Database(entities = {Note.class, Category.class, Anniversary.class, Message.class}, version = 6)
@TypeConverters({InstantConverter.class, DurationConverter.class, ChronoUnitConverter.class, LocalDateConverter.class})
public abstract class Database extends RoomDatabase {

    private static volatile Database INSTANCE;
    public static final String DB_NAME = "app.db";

    public abstract DAONote getDAONote();
    public abstract DAOCategory getDAOCategory();
    public abstract DAOAnniversary getDAOAnniversary();
    public abstract DAOMessage getDAOMessage();
    
    /**
     * Singleton instance getter
     * @param context context to recreate database
     * @return database instance
     */
    public static Database getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (Database.class) {
                if (INSTANCE == null)
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), Database.class, DB_NAME)
                            .addMigrations(Migrate.getAll())
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    AnniversaryPopulator.populate(db);
                                }
                            })
                            .build();
            }
        }
        return INSTANCE;
    }
}