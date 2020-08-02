package com.python.companion.db;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.python.companion.db.dao.DAOCategory;
import com.python.companion.db.dao.DAOMeasurement;
import com.python.companion.db.dao.DAONote;
import com.python.companion.db.dao.DAONotify;
import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Measurement;
import com.python.companion.db.entity.Note;
import com.python.companion.db.entity.Notify;
import com.python.companion.db.migrations.Migrate;
import com.python.companion.db.typeconverters.ChronoUnitConverter;
import com.python.companion.db.typeconverters.DurationConverter;
import com.python.companion.db.typeconverters.InstantConverter;
import com.python.companion.db.typeconverters.LocalDateConverter;

import java.time.temporal.ChronoUnit;

@androidx.room.Database(entities = {Note.class, Category.class, Measurement.class, Notify.class}, version = 3)
@TypeConverters({InstantConverter.class, DurationConverter.class, ChronoUnitConverter.class, LocalDateConverter.class})
public abstract class Database extends RoomDatabase {

    private static volatile Database INSTANCE;
    public static final String DB_NAME = "app.db";

    public abstract DAONote getDAONote();
    public abstract DAOCategory getDAOCategory();
    public abstract DAOMeasurement getDAOMeasurement();
    public abstract DAONotify getDAONotify();
    
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
                                    long day_duration = DurationConverter.durationToDays(ChronoUnit.DAYS.getDuration());
                                    long month_duration = DurationConverter.durationToDays(ChronoUnit.MONTHS.getDuration());
                                    long year_duration = DurationConverter.durationToDays(ChronoUnit.YEARS.getDuration());

                                    String days = ChronoUnitConverter.chronoUnitToString(ChronoUnit.DAYS);
                                    String months = ChronoUnitConverter.chronoUnitToString(ChronoUnit.MONTHS);
                                    String years = ChronoUnitConverter.chronoUnitToString(ChronoUnit.YEARS);
                                    db.execSQL("BEGIN TRANSACTION;");
                                    db.execSQL("INSERT OR IGNORE INTO Measurements VALUES(0, 'Day', 'Days', "+day_duration+", 1, 1, 0, "+days+", 0, 0);");
                                    db.execSQL("INSERT OR IGNORE INTO Measurements VALUES(1, 'Month', 'Months', "+month_duration+", 1, 1, 1, "+months+", 0, 0);");
                                    db.execSQL("INSERT OR IGNORE INTO Measurements VALUES(2, 'Year', 'Years', "+year_duration+", 1, 1, 2, "+years+", 0, 0);");
                                    db.execSQL("COMMIT;");
                                }
                            })
                            .build();
            }
        }
        return INSTANCE;
    }
}