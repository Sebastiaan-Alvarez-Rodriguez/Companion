package com.python.companion.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.python.companion.R;
import com.python.companion.db.entity.Anniversary;
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

public class AnniversaryUtil {
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
     * Computes the interval-th anniversary occuring after present date, asynchronously
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

    public static boolean isAnniversaryOnDate(@NonNull TemporalUnit unit, @NonNull LocalDate together, @NonNull LocalDate date) {
        long intervals = unit.between(together, date);
        return together.plus(intervals, unit).isEqual(date);
    }
    public static boolean isAnniversaryToday(@NonNull TemporalUnit unit, @NonNull LocalDate together) {
        return isAnniversaryOnDate(unit, together, LocalDate.now());
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
     * Computes distance between shared intervals for given anniversaries of same type
     * Note: Only pass anniversaries of same type!
     * Note: Returned amount of days is estimation in case of variable length temporal units such as months, years etc
     * @return distance in days between each shared interval of given anniversaries
     */
    private static long getIntertwinedDistance(Anniversary first, Anniversary... anniversaries) {
        long distance = first.getDuration().toDays();
        for (TemporalUnit other : anniversaries) {
            long dist_other = other.getDuration().toDays();
            distance = (distance*dist_other/gcd_binary(distance, dist_other));
        }
        return distance;
    }

    private static boolean onInterval(@NonNull TemporalUnit unit, @NonNull LocalDate together, @NonNull LocalDate date) {
        return unit.between(together, date.minus(1, ChronoUnit.DAYS)) + 1 == unit.between(together, date);
    }


    /** Computes interval with a given list of anniversaries in separate thread, and returns result in provided {@code ResultListener} */
    public static void futureIntertwinedInterval(@NonNull Anniversary unit, @NonNull LocalDate together, @NonNull List<Anniversary> others, long interval, ResultListener<LocalDate> dateListener, ErrorListener errorListener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                dateListener.onResult(futureIntertwinedInterval(unit, together, others, interval));
            } catch (DateTimeException e) {
                errorListener.onError(e.getLocalizedMessage());
            }
        });
    }

    /**
     * Computes interval with a given list of anniversaries. You really should do this in the background, as it may require lots of work
     * @param others Other temporal units to compute intertwined integer interval for
     * @param interval intervals to look ahead. Default is 1. Only > 1 allowed.
     * @return next interval intertwined with given other units
     */
    @WorkerThread
    @CheckResult
    public static LocalDate futureIntertwinedInterval(@NonNull Anniversary unit, @NonNull LocalDate together, @NonNull List<Anniversary> others, long interval) throws DateTimeException {
        ArrayList<Anniversary> all = new ArrayList<>(others);
        all.add(unit);

        List<Anniversary> dayBased = all.parallelStream().filter(anniversary -> anniversary.getCornerstoneType() == ChronoUnit.DAYS).collect(Collectors.toList());
        List<Anniversary> monthBased = all.parallelStream().filter(anniversary -> anniversary.getCornerstoneType() == ChronoUnit.MONTHS).collect(Collectors.toList());
        List<Anniversary> yearBased = all.parallelStream().filter(anniversary -> anniversary.getCornerstoneType() == ChronoUnit.YEARS).collect(Collectors.toList());

        @Nullable Anniversary dayJumpAmount = null, monthJumpAmount = null, yearJumpAmount = null;
        if (dayBased.size() > 0) {
            Anniversary one = dayBased.get(dayBased.size() - 1);
            dayBased.remove(dayBased.size() - 1);
            dayJumpAmount = new Anniversary("", "", Duration.ofDays(getIntertwinedDistance(one, dayBased.toArray(new Anniversary[]{}))), 1, 1, -1, ChronoUnit.DAYS, false, false);
        }
        if (monthBased.size() > 0) {
            Anniversary one = monthBased.get(monthBased.size() - 1);
            monthBased.remove(monthBased.size() - 1);
            monthJumpAmount = new Anniversary("", "", Duration.ofDays(getIntertwinedDistance(one, monthBased.toArray(new Anniversary[]{}))), 1, 1, -1, ChronoUnit.MONTHS, false, false);
        }
        if (yearBased.size() > 0) {
            Anniversary one = yearBased.get(yearBased.size() - 1);
            yearBased.remove(yearBased.size() - 1);
            yearJumpAmount = new Anniversary("", "", Duration.ofDays(getIntertwinedDistance(one, yearBased.toArray(new Anniversary[]{}))), 1, 1, -1, ChronoUnit.YEARS, false, false);
        }

        Anniversary largest = unit;
        for (Anniversary m : all)
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
    public static LocalDate futureIntertwinedInterval(@NonNull Anniversary unit, @NonNull LocalDate together, @NonNull List<Anniversary> others) {
        return futureIntertwinedInterval(unit, together, others, 1);
    }

    /** Returns the currently stored date on which the user's relation started */
    public static @NonNull LocalDate getTogether(@NonNull Context context) {
        SharedPreferences preferences = context.getSharedPreferences(context.getString(R.string.cactus_preferences), Context.MODE_PRIVATE);
        return LocalDate.parse(preferences.getString(context.getString(R.string.cactus_preferences_key_together), "2017-11-08"));
    }

    public static void setTogether(@NonNull LocalDate date, @NonNull Context context) {
        SharedPreferences.Editor preferences = context.getSharedPreferences(context.getString(R.string.cactus_preferences), Context.MODE_PRIVATE).edit();
        preferences.putString(context.getString(R.string.cactus_preferences_key_together), date.toString());
        preferences.apply();
    }

    /** Converts default types (chronounits) to anniversaryID's */
    public static long chronoUnitToID(@NonNull ChronoUnit unit) {
        switch (unit) {
            case DAYS:
                return -2;
            case MONTHS:
                return -3;
            case YEARS:
                return -4;
        }
        throw new IllegalArgumentException("Given ChronoUnit '"+unit.name()+"' is not supported");
    }

    public static long NoneAnniversaryID() {
        return -1;
    }

    public static boolean isBaseAnniversary(@NonNull Anniversary anniversary) {
        return anniversary.getAnniversaryID() > -5 && anniversary.getAnniversaryID() < -1;
    }

    /** Returns an instantiated Anniversary representing inputted ChronoUnit type, which can be used as a basetype */
    public static Anniversary getBaseAnniversary(@NonNull ChronoUnit unit) {
        switch (unit) {
            case DAYS:
                return new Anniversary(chronoUnitToID(unit), "Day", "Days", ChronoUnit.DAYS.getDuration(), 1, 1, chronoUnitToID(unit), ChronoUnit.DAYS, false, false);
            case MONTHS:
                return new Anniversary(chronoUnitToID(unit), "Month", "Months", ChronoUnit.MONTHS.getDuration(), 1, 1, chronoUnitToID(unit), ChronoUnit.MONTHS, false, false);
            case YEARS:
                return new Anniversary(chronoUnitToID(unit), "Year", "Years", ChronoUnit.YEARS.getDuration(), 1, 1, chronoUnitToID(unit), ChronoUnit.YEARS, false, false);
        }
        throw new IllegalArgumentException("ChronoUnit '"+unit.name()+"' is not supported as base anniversary");
    }

    public static List<Anniversary> getBaseAnniversaries() {
        ArrayList<Anniversary> returnList = new ArrayList<>(3);
        returnList.add(new Anniversary(chronoUnitToID(ChronoUnit.DAYS), "Day", "Days", ChronoUnit.DAYS.getDuration(), 1, 1, chronoUnitToID(ChronoUnit.DAYS), ChronoUnit.DAYS, false, false));
        returnList.add(new Anniversary(chronoUnitToID(ChronoUnit.MONTHS), "Month", "Months", ChronoUnit.MONTHS.getDuration(), 1, 1, chronoUnitToID(ChronoUnit.MONTHS), ChronoUnit.MONTHS, false, false));
        returnList.add(new Anniversary(chronoUnitToID(ChronoUnit.YEARS), "Year", "Years", ChronoUnit.YEARS.getDuration(), 1, 1, chronoUnitToID(ChronoUnit.YEARS), ChronoUnit.YEARS, false, false));
        return returnList;
    }

    /** Returns corresponding ChronoUnit represented by passed baseAnniversary. Note: Only provide base-anniversaries constructed with {@link #getBaseAnniversary(ChronoUnit)} */
    public static ChronoUnit getBaseChronoUnit(@NonNull Anniversary baseAnniversary) {
        if (!isBaseAnniversary(baseAnniversary))
            throw new IllegalArgumentException("Cannot get base ChronoUnit for non-base anniversary '"+ baseAnniversary.getNameSingular()+"'");
        long id = baseAnniversary.getAnniversaryID();
        if (id == -2)
            return ChronoUnit.DAYS;
        else if (id == -3)
            return ChronoUnit.MONTHS;
        else
            return ChronoUnit.YEARS;
    }

    public static String getBaseChronoUnitSingular(ChronoUnit unit) {
        String tmp = ChronoUnit.DAYS.toString();
        return tmp.substring(0, tmp.length()-1);
    }

    /** Returns suffix type for the "number'th" number (e.g. 1 -> 1st, 33 -> 23rd) */
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
