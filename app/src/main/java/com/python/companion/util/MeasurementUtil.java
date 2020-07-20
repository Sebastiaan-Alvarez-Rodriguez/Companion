package com.python.companion.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

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
            dayJumpAmount = new Measurement("", "", Duration.ofDays(getIntertwinedDistance(one, dayBased.toArray(new Measurement[]{}))), 1, 1, -1, ChronoUnit.DAYS);
        }
        if (monthBased.size() > 0) {
            Measurement one = monthBased.get(monthBased.size() - 1);
            monthBased.remove(monthBased.size() - 1);
            monthJumpAmount = new Measurement("", "", Duration.ofDays(getIntertwinedDistance(one, monthBased.toArray(new Measurement[]{}))), 1, 1, -1, ChronoUnit.MONTHS);
        }
        if (yearBased.size() > 0) {
            Measurement one = yearBased.get(yearBased.size() - 1);
            yearBased.remove(yearBased.size() - 1);
            yearJumpAmount = new Measurement("", "", Duration.ofDays(getIntertwinedDistance(one, yearBased.toArray(new Measurement[]{}))), 1, 1, -1, ChronoUnit.YEARS);
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

    /**
     * @return An <code>ItemAdapter</code> containing default measurements: Days, Months, Years, as defined by {@link ChronoUnit}
     */
    public static List<Measurement> getDefaultMeasurements() {
        ChronoUnit[] regulars = {ChronoUnit.DAYS, ChronoUnit.MONTHS, ChronoUnit.YEARS};
        String[] singulars = {"Day", "Month", "Year"};
        ArrayList<Measurement> list = new ArrayList<>(3);
        for (int x = 0; x < regulars.length; ++x)
            list.add(new Measurement(ChronoUnitToID(regulars[x]), singulars[x], regulars[x].toString(), regulars[x].getDuration(), 1, 1, -1, regulars[x]));
        return list;
    }

    public static boolean isDefault(long id) {
        return id >=-3 && id <= -1;
    }

    public static String IDtoName(long id, int amount) {
        if (id < 0) {
            final String[] singulars = {"Day", "Month", "Year"};
            final String[] plurals = {"Days", "Months", "Years"};
            int idx = (int) ((id * -1) - 1);
            return (amount == 1) ? singulars[idx] : plurals[idx];
        }
        throw new RuntimeException("Unsupported id ("+id+")");
    }

    public static long ChronoUnitToID(@NonNull ChronoUnit unit) {
        switch (unit) {
            case DAYS:
                return -1;
            case MONTHS:
                return -2;
            case YEARS:
                return -3;
        }
        throw new RuntimeException("Unsupported ChronoUnit ("+unit.toString()+")");
    }
}
