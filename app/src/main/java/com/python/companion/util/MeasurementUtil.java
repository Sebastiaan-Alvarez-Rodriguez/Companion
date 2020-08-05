package com.python.companion.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.python.companion.R;
import com.python.companion.db.entity.Measurement;
import com.python.companion.util.genericinterfaces.ErrorListener;
import com.python.companion.util.genericinterfaces.ResultListener;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MeasurementUtil {
    /** @return amount of integer units passed since date together */
    public static long distanceCurrent(@NonNull TemporalUnit unit, @NonNull LocalDate together) {
        return unit.between(together, LocalDate.now());
    }

    /** @return amount of integer units between present moment and given date*/
    public static long computeDistance(@NonNull TemporalUnit unit, @NonNull LocalDate date) {
        return unit.between(LocalDate.now(), date);
    }
    /** Wrapper for {@link #computeDistance(TemporalUnit, LocalDate)} to return distance in days */
    public static long computeDistance(@NonNull LocalDate date) {
        return computeDistance(ChronoUnit.DAYS, date);
    }

    /**
     * Computes the interval-th jubileum occuring after present date, asynchronously
     * @param interval intervals to look ahead. Default is 1 (upcoming interval). Zero and negative values allowed.
     *                 0 computes last interval. -1 computes interval before that, etcetera
     * @param dateListener Receives computed localdate on success
     * @param errorListener Receives localized error message on failure
     */
    public static void futureInterval(@NonNull TemporalUnit unit, @NonNull LocalDate together, long interval, @NonNull ResultListener<LocalDate> dateListener, @NonNull ErrorListener errorListener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                long intervalsBefore = unit.between(together, LocalDate.now());
                dateListener.onResult(unit.addTo(together, intervalsBefore + interval));
            } catch (DateTimeException e) {
                errorListener.onError(e.getLocalizedMessage());
            }
        });
    }
    /** Sequential alernative for {@link #futureInterval(TemporalUnit, LocalDate, long, ResultListener, ErrorListener)} */
    @CheckResult
    public static LocalDate futureInterval(@NonNull TemporalUnit unit, @NonNull LocalDate together, long interval) throws DateTimeException {
        long intervalsBefore = unit.between(together, LocalDate.now());
        return unit.addTo(together, intervalsBefore + interval);
    }

    private static long gcd_binary(long a, long b) {
        if (a == 0)
            return b;
        if (b == 0)
            return a;
        int shift = Long.numberOfTrailingZeros((a | b));
        a >>= Long.numberOfTrailingZeros(a);
        do {
            b >>= Long.numberOfTrailingZeros(b);
            if (a > b) {
                long tmp = a;
                a = b;
                b = tmp;
            }
            b -= a;
        } while (b != 0);
        return a << shift;
    }

    /**
     * Computes distance between shared intervals for given measurements of same type
     * Note: Only pass measurements of same type!
     * Note: Returned amount of days is estimation in case of variable length temporal units such as months, years etc
     * @return distance in days between each shared interval of given measurements
     */
    private static long getIntertwinedDistance(Measurement first, Measurement... measurements) {
        long distance = first.getDuration().toDays();
        for (TemporalUnit other : measurements) {
            long dist_other = other.getDuration().toDays();
            distance = (distance*dist_other/gcd_binary(distance, dist_other));
        }
        return distance;
    }

    private static boolean onInterval(@NonNull TemporalUnit unit, @NonNull LocalDate together, @NonNull LocalDate date) {
        return unit.between(together, date.minus(1, ChronoUnit.DAYS)) + 1 == unit.between(together, date);
    }


    public static void futureIntertwinedInterval(@NonNull Measurement unit, @NonNull LocalDate together, @NonNull List<Measurement> others, long interval, ResultListener<LocalDate> dateListener, ErrorListener errorListener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                dateListener.onResult(futureIntertwinedInterval(unit, together, others, interval));
            } catch (DateTimeException e) {
                errorListener.onError(e.getLocalizedMessage());
            }
        });
    }

    /**
     * Computes interval with a given list of measurements. You really should do this in the background, as it may require lots of work
     * @param others Other temporal units to compute intertwined integer interval for
     * @param interval intervals to look ahead. Default is 1. Only > 1 allowed.
     * @return next interval intertwined with given other units
     */
    @WorkerThread
    public static LocalDate futureIntertwinedInterval(@NonNull Measurement unit, @NonNull LocalDate together, @NonNull List<Measurement> others, long interval) throws DateTimeException {
        ArrayList<Measurement> all = new ArrayList<>(others);
        all.add(unit);

        List<Measurement> dayBased = all.parallelStream().filter(measurement -> measurement.getCornerstoneType() == ChronoUnit.DAYS).collect(Collectors.toList());
        List<Measurement> monthBased = all.parallelStream().filter(measurement -> measurement.getCornerstoneType() == ChronoUnit.MONTHS).collect(Collectors.toList());
        List<Measurement> yearBased = all.parallelStream().filter(measurement -> measurement.getCornerstoneType() == ChronoUnit.YEARS).collect(Collectors.toList());

        @Nullable Measurement dayJumpAmount = null, monthJumpAmount = null, yearJumpAmount = null;
        if (dayBased.size() > 0) {
            Measurement one = dayBased.get(dayBased.size() - 1);
            dayBased.remove(dayBased.size() - 1);
            dayJumpAmount = new Measurement("", "", Duration.ofDays(getIntertwinedDistance(one, dayBased.toArray(new Measurement[]{}))), 1, 1, -1, ChronoUnit.DAYS, false, false);
        }
        if (monthBased.size() > 0) {
            Measurement one = monthBased.get(monthBased.size() - 1);
            monthBased.remove(monthBased.size() - 1);
            monthJumpAmount = new Measurement("", "", Duration.ofDays(getIntertwinedDistance(one, monthBased.toArray(new Measurement[]{}))), 1, 1, -1, ChronoUnit.MONTHS, false, false);
        }
        if (yearBased.size() > 0) {
            Measurement one = yearBased.get(yearBased.size() - 1);
            yearBased.remove(yearBased.size() - 1);
            yearJumpAmount = new Measurement("", "", Duration.ofDays(getIntertwinedDistance(one, yearBased.toArray(new Measurement[]{}))), 1, 1, -1, ChronoUnit.YEARS, false, false);
        }

        Measurement largest = unit;
        for (Measurement m : all)
            if (m.compareTo(largest) > 0)
                largest = m;

        LocalDate answer = largest.addTo(together, largest.between(together, LocalDate.now()));
        for (long x = 1; x <= interval; ++x) {
            answer = largest.addTo(answer, 1);
            while ((dayJumpAmount != null && !onInterval(dayJumpAmount, together, answer))
                    || (monthJumpAmount != null && !onInterval(monthJumpAmount, together, answer))
                    || (yearJumpAmount != null && !onInterval(yearJumpAmount, together, answer)))
                answer = largest.addTo(answer, 1);
        }
        return answer;
    }

    /**
     * @param others Other temporal units to compute intertwined integer interval for
     * @return next interval intertwined with given other units
     */
    public static LocalDate futureIntertwinedInterval(@NonNull Measurement unit, @NonNull LocalDate together, @NonNull List<Measurement> others) {
        return futureIntertwinedInterval(unit, together, others, 1);
    }

    public static @NonNull LocalDate getTogether(@NonNull Context context) {
        SharedPreferences preferences = context.getSharedPreferences(context.getString(R.string.cactus_preferences), Context.MODE_PRIVATE);
        return LocalDate.parse(preferences.getString(context.getString(R.string.cactus_preferences_key_together), "2017-11-08"));
    }

    public static void setTogether(@NonNull LocalDate date, @NonNull Context context) {
        SharedPreferences.Editor preferences = context.getSharedPreferences(context.getString(R.string.cactus_preferences), Context.MODE_PRIVATE).edit();
        preferences.putString(context.getString(R.string.cactus_preferences_key_together), date.toString());
        preferences.apply();
    }

    /** Converts default types (chronounits) to measurementID's, used to store them in the database */
    public static long chronoUnitToID(@NonNull ChronoUnit unit) {
        switch (unit) {
            case DAYS:
                return -2;
            case MONTHS:
                return -3;
            case YEARS:
                return -4;
        }
        throw new RuntimeException("Unsupported ChronoUnit ("+unit.toString()+")");
    }

    public static Measurement getBaseMeasurement(@NonNull ChronoUnit unit) {
        switch (unit) {
            case DAYS:
                return new Measurement(chronoUnitToID(unit), "Day", "Days", ChronoUnit.DAYS.getDuration(), 1, 1, chronoUnitToID(unit), ChronoUnit.DAYS, false, false);
            case MONTHS:
                return new Measurement(chronoUnitToID(unit), "Month", "Months", ChronoUnit.MONTHS.getDuration(), 1, 1, chronoUnitToID(unit), ChronoUnit.MONTHS, false, false);
            case YEARS:
                return new Measurement(chronoUnitToID(unit), "Year", "Years", ChronoUnit.YEARS.getDuration(), 1, 1, chronoUnitToID(unit), ChronoUnit.YEARS, false, false);
        }
        return Measurement.template();
    }

    public static long NoneMeasurementID() {
        return -1;
    }


    /** Returns suffix type for the "number'th" number */
    public static String getDayOfMonthSuffix(int n) {
        if (n >= 11 && n <= 13)
            return "th";
        switch (n % 10) {
            case 1:  return "st";
            case 2:  return "nd";
            case 3:  return "rd";
            default: return "th";
        }
    }
}
