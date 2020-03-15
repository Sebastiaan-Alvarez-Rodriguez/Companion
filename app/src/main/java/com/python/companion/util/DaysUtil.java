package com.python.companion.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.python.companion.R;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.TemporalUnit;

public class DaysUtil {
    public static <T extends TemporalUnit> long timeTogether(T unit, Context context) throws DateTimeException {
        SharedPreferences preferences = context.getSharedPreferences(context.getString(R.string.measurement_preferences), Context.MODE_PRIVATE);
        if (!preferences.contains("date_together"))
            throw new DateTimeException("User did not set date!");
        String together = preferences.getString("date_together", "");
        return unit.between(LocalDate.parse(together), LocalDate.now());
    }

    public static <T extends TemporalUnit> long timeTogether(T unit, String date) throws DateTimeException {
        return unit.between(LocalDate.parse(date), LocalDate.now());
    }

//    public enum TimeExtensions implements TemporalUnit {
//        DAYS42("Days42", ChronoUnit.DAYS.getDuration().multipliedBy(42));
//
//        private final String name;
//        private final Duration duration;
//
//        private TimeExtensions(String name, Duration estimatedDuration) {
//            this.name = name;
//            this.duration = estimatedDuration;
//        }
//
//        @Override
//        public Duration getDuration() {
//            return duration;
//        }
//
//        @Override
//        public boolean isDurationEstimated() {
//            return true;
//        }
//
//        @Override
//        public boolean isDateBased() {
//            return true;
//        }
//
//        @Override
//        public boolean isTimeBased() {
//            return false;
//        }
//
//        @SuppressWarnings("unchecked")
//        @Override
//        public <T extends Temporal> T addTo(T temporal, long amount) {
//            return (T) temporal.plus(amount, this);
//        }
//
//        @Override
//        public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
//            return temporal1Inclusive.until(temporal2Exclusive, this);
//        }
//
//        @NotNull
//        @Override
//        public String toString() {
//            return name;
//        }
//    }
}
