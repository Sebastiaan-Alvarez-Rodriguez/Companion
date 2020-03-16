package com.python.companion.util.measurement;

import androidx.annotation.NonNull;

import com.python.companion.db.entity.Measurement;
import com.python.companion.ui.cactus.measurement.adapter.MeasurementItem;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MeasurementUtil {
    /**
     * @return amount of integer units passed since date together
     */
    public static long distanceCurrent(@NonNull TemporalUnit unit, @NonNull LocalDate together) {
        return unit.between(together, LocalDate.now());
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

    private static long gcd_euler_recursive(long a, long b) {
        if (b != 0)
            return gcd_euler_recursive(b, a % b);
        else
            return a;
    }

    private static long gcd_euler(long a, long b) {
        while (b != 0) {
            long tmp = a;
            long tmp2 = b;
            a = tmp2;
            b = tmp % tmp2;
        }
        return a;
    }

    /**
     * @param others Other temporal units to compute intertwined integer interval for
     * @param interval intervals to look ahead. Default is 1. Zero and negative values allowed.
     *                 0 computes last interval. -1 computes interval before that, etcetera
     * @return next interval intertwined with given other units
     */
    public static LocalDate intertwineIntervalFuture(@NonNull TemporalUnit unit, @NonNull LocalDate together, @NonNull Collection<TemporalUnit> others, long interval) {
        //TODO: Guaranteed possibility. Between 2 numbers: (A*B)/gcd works
        // 06 / 20 -> 06*10=60=3*20 (gcd=2, interval computation=06*20/02=60)
        // 07 / 21 -> 03*07=21=1*21 (gcd=7, interval computation=07*21/07=21)
        // 21 / 22 -> 22*21=462=21*22 (co-prime, gcd=1)
        // Any 2 integers A and B share intervals, at most at A*B, and at least at (A*B)/gcd
        // 06 / 07 / 09 -> 06*07/1=42. 06*09/3=54/3=18. 07*09/1=63. Together: gcd=min(1,3,1)=1. computation=06*07*09/1=378
        // 06 / 12 / 24 -> 06*12/6=12. 06*24/6=4. 12*24/12=2. Together: gcd=min(6, 6, 12)=6. computation=...=24
        // 06 / 12 / 24 -> 06*12/6=12. To add 24: determine 12 / 24. 12 / 24 -> 12*24/12=24<-new interval
        // 06 / 12 / 25 -> 06*12/6=12. To add 25: determine 12 / 25. 12 / 25 -> 12*25/1=300<-new interval
        // 06 / 12 / 26 -> 06*12/6=12. To add 26: determine 12 / 26. 12 / 26 -> 12*26/2=156<-new interval
        // 06 / 12 / 27 -> 06*12/6=12. To add 27: determine 12 / 27. 12 / 27 -> 12*27/3=108<-new interval
        // 06 / 20 / 30 -> 06*20/2=60. To add 30: determine 60 / 30. 30 / 60 -> 30*60/30=60<-new interval
        // 06 / 20 / 32 -> 06*20/2=60. To add 32: determine 60 / 32. 32 / 60 -> 32*60/4=480<-new interval

        long distance = unit.getDuration().toDays();
        for (TemporalUnit other : others) {
            long dist_other = other.getDuration().toDays();
            distance = (distance*dist_other/gcd_binary(distance, dist_other));
        }
        // Here, we know the distance between every intertwined integer interval
        LocalDate returndate = together;
        LocalDate now = LocalDate.now();
        while (returndate.isBefore(now))
            returndate = returndate.plus(distance, ChronoUnit.DAYS);
        // We progressed to the first interval target after present moment.

        return returndate.plus(distance*(interval-1), ChronoUnit.DAYS);
    }
    public static LocalDate intertwineIntervalFuture(@NonNull TemporalUnit unit, @NonNull LocalDate together, @NonNull Collection<TemporalUnit> others) {
        return intertwineIntervalFuture(unit, together, others, 1);
    }

    /**
     * @param selectable Sets whether the default items are selectable (should probably be <code>false</code> if user gets 'delete selected' option
     * @return An <code>ItemAdapter</code> containing default measurements: Days, Months, Years, as defined by {@link ChronoUnit}
     */
    public static List<MeasurementItem> getDefaultMeasurements(boolean selectable) {
        ChronoUnit[] regulars = {ChronoUnit.DAYS, ChronoUnit.MONTHS, ChronoUnit.YEARS};
        String[] singulars = {"Day", "Month", "Year"};
        ArrayList<MeasurementItem> list = new ArrayList<>(3);
        for (int x = 0; x < regulars.length; ++x) {
            MeasurementItem m = new MeasurementItem();
            m.setSelectable(selectable);
            m.setMeasurement(new Measurement(singulars[x], regulars[x].toString(), regulars[x].getDuration()));
            list.add(m);
        }
        return list;
    }
}
