package com.python.companion.db.populators;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.python.companion.db.typeconverters.ChronoUnitConverter;
import com.python.companion.db.typeconverters.DurationConverter;
import com.python.companion.util.AnniversaryUtil;

import java.time.temporal.ChronoUnit;

public class AnniversaryPopulator {
    public static void populate(@NonNull SupportSQLiteDatabase db) {
        long day_duration = DurationConverter.durationToDays(ChronoUnit.DAYS.getDuration());
        long month_duration = DurationConverter.durationToDays(ChronoUnit.MONTHS.getDuration());
        long year_duration = DurationConverter.durationToDays(ChronoUnit.YEARS.getDuration());

        long dayID = AnniversaryUtil.chronoUnitToID(ChronoUnit.DAYS);
        long monthID = AnniversaryUtil.chronoUnitToID(ChronoUnit.MONTHS);
        long yearID = AnniversaryUtil.chronoUnitToID(ChronoUnit.YEARS);

        String days = ChronoUnitConverter.chronoUnitToString(ChronoUnit.DAYS);
        String months = ChronoUnitConverter.chronoUnitToString(ChronoUnit.MONTHS);
        String years = ChronoUnitConverter.chronoUnitToString(ChronoUnit.YEARS);
        db.execSQL("BEGIN TRANSACTION;");
        db.execSQL("INSERT OR IGNORE INTO Anniversary (anniversaryID,nameSingular,namePlural,duration,amount,precomputedamount,parentID,cornerstoneType,hasNotifications,canModify) VALUES("+dayID+", 'Day', 'Days', "+day_duration+", 1, 1, "+dayID+", '"+days+"', 0, 0);");
        db.execSQL("INSERT OR IGNORE INTO Anniversary VALUES("+monthID+", 'Month', 'Months', "+month_duration+", 1, 1, "+monthID+", '"+months+"', 0, 0);");
        db.execSQL("INSERT OR IGNORE INTO Anniversary VALUES("+yearID+", 'Year', 'Years', "+year_duration+", 1, 1, "+yearID+", '"+years+"', 0, 0);");
        db.execSQL("COMMIT;");
    }
}
