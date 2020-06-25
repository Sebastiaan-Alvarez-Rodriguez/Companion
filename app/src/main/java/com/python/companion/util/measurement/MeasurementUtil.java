package com.python.companion.util.measurement;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.python.companion.db.entity.Measurement;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
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
     * @param interval intervals to look ahead. Default is 1. Zero and negative values allowed.
     *                 0 computes last interval. -1 computes interval before that, etcetera
     * @return next interval for this unit
     */
    public static LocalDate futureInterval(@NonNull TemporalUnit unit, @NonNull LocalDate together, long interval) {
        long intervalsBefore = unit.between(together, LocalDate.now());
        return unit.addTo(together, intervalsBefore+interval);
    }
    public static LocalDate futureInterval(@NonNull TemporalUnit unit, @NonNull LocalDate together) {
        return futureInterval(unit,together, 1);
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
    /**
     * @param others Other temporal units to compute intertwined integer interval for
     * @param interval intervals to look ahead. Default is 1. Only > 1 allowed.
     * @return next interval intertwined with given other units
     */
    @WorkerThread
    public static LocalDate futureIntertwinedInterval(@NonNull Measurement unit, @NonNull LocalDate together, @NonNull List<Measurement> others, long interval) {
        ArrayList<Measurement> all = new ArrayList<>(others);
        all.add(unit);

        List<Measurement> dayBased = all.parallelStream().filter(measurement -> measurement.getCornerstoneType() == ChronoUnit.DAYS).collect(Collectors.toList());
        List<Measurement> monthBased = all.parallelStream().filter(measurement -> measurement.getCornerstoneType() == ChronoUnit.MONTHS).collect(Collectors.toList());
        List<Measurement> yearBased = all.parallelStream().filter(measurement -> measurement.getCornerstoneType() == ChronoUnit.YEARS).collect(Collectors.toList());

        @Nullable Measurement dayJumpAmount = null, monthJumpAmount = null, yearJumpAmount = null;
        if (dayBased.size() > 0) {
            Measurement one = dayBased.get(dayBased.size() - 1);
            dayBased.remove(dayBased.size() - 1);
            dayJumpAmount = new Measurement("", "", Duration.ofDays(getIntertwinedDistance(one, dayBased.toArray(new Measurement[]{}))), ChronoUnit.DAYS);
        }
        if (monthBased.size() > 0) {
            Measurement one = monthBased.get(monthBased.size() - 1);
            monthBased.remove(monthBased.size() - 1);
            monthJumpAmount = new Measurement("", "", Duration.ofDays(getIntertwinedDistance(one, monthBased.toArray(new Measurement[]{}))), ChronoUnit.MONTHS);
        }
        if (yearBased.size() > 0) {
            Measurement one = yearBased.get(yearBased.size() - 1);
            yearBased.remove(yearBased.size() - 1);
            yearJumpAmount = new Measurement("", "", Duration.ofDays(getIntertwinedDistance(one, yearBased.toArray(new Measurement[]{}))), ChronoUnit.YEARS);
        }

        Measurement largest = unit;
        for (Measurement m : all)
            if (m.compareTo(largest) > 0)
                largest = m;

        for (Measurement m : all)
            Log.e("MeasurementUtil", "Measurement: "+m.getNamePlural());
        Log.e("MeasurementUtil", "Largest: "+largest.getNamePlural());
        if (dayJumpAmount != null)
            Log.e("MeasurementUtil", "Working with shared day based length: "+dayJumpAmount.getDuration().toDays()+" days");
        if (monthJumpAmount != null)
            Log.e("MeasurementUtil", "Working with shared month based length: "+monthJumpAmount.getDuration().toDays()/ChronoUnit.MONTHS.getDuration().toDays()+" months");
        if (yearJumpAmount != null)
            Log.e("MeasurementUtil", "Working with shared year based length: "+yearJumpAmount.getDuration().toDays()/ChronoUnit.YEARS.getDuration().toDays()+" years");

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

    /**
     * @return An <code>ItemAdapter</code> containing default measurements: Days, Months, Years, as defined by {@link ChronoUnit}
     */
    public static List<Measurement> getDefaultMeasurements() {
        ChronoUnit[] regulars = {ChronoUnit.DAYS, ChronoUnit.MONTHS, ChronoUnit.YEARS};
        String[] singulars = {"Day", "Month", "Year"};
        ArrayList<Measurement> list = new ArrayList<>(3);
        for (int x = 0; x < regulars.length; ++x)
            list.add(new Measurement(singulars[x], regulars[x].toString(), regulars[x].getDuration(), regulars[x]));
        return list;
    }
}
