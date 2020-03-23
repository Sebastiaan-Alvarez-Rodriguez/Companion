package com.python.companion.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;

@Entity(primaryKeys = {"namePlural"})
public class Measurement implements TemporalUnit {
    private @NonNull String nameSingular, namePlural;
    private Duration duration;

    private ChronoUnit cornerstoneType;

    /**
     * Initialize a date measurement with singular, plural names, a duration and a cornerstone type.
     * The type will be used for comparisons, yielding the ability to use irregular durational types, such as months.
     * @param cornerstoneType The normalized cornerstone type this represents. e.g.: ChronoUnit.MONTHS
     */
    public Measurement(@NonNull String nameSingular, @NonNull String namePlural, Duration duration, ChronoUnit cornerstoneType) {
        this.nameSingular = nameSingular;
        this.namePlural = namePlural;
        this.duration = duration;
        this.cornerstoneType = cornerstoneType;
    }

    @NonNull
    public String getNameSingular() {
        return nameSingular;
    }

    public void setNameSingular(@NonNull String nameSingular) {
        this.nameSingular = nameSingular;
    }

    @NonNull
    public String getNamePlural() {
        return namePlural;
    }

    public void setNamePlural(@NonNull String namePlural) {
        this.namePlural = namePlural;
    }

    @Override
    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public ChronoUnit getCornerstoneType() {
        return cornerstoneType;
    }

    public void setCornerstoneType(ChronoUnit cornerstoneType) {
        this.cornerstoneType = cornerstoneType;
    }

    @Override
    public boolean isDurationEstimated() {
        return this.compareTo(ChronoUnit.DAYS) >= 0;
    }

    @Override
    public boolean isDateBased() {
        return this.compareTo(ChronoUnit.DAYS) >= 0;
    }

    @Override
    public boolean isTimeBased() {
        return this.compareTo(ChronoUnit.DAYS) < 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Temporal> R addTo(R temporal, long amount) {
        return (R) temporal.plus((amount*duration.toDays())/cornerstoneType.getDuration().toDays(), cornerstoneType);
    }

//    /**
//     * Calculates the amount of time until another date in terms of the specified unit.
//     * <p>
//     * This calculates the amount of time between two {@code LocalDate}
//     * objects in terms of a single {@code TemporalUnit}.
//     * The start and end points are {@code this} and the specified date.
//     * The result will be negative if the end is before the start.
//     * The {@code Temporal} passed to this method is converted to a
//     * {@code LocalDate} using {@link #from(TemporalAccessor)}.
//     * For example, the amount in days between two dates can be calculated
//     * using {@code startDate.until(endDate, DAYS)}.
//     * <p>
//     * The calculation returns a whole number, representing the number of
//     * complete units between the two dates.
//     * For example, the amount in months between 2012-06-15 and 2012-08-14
//     * will only be one month as it is one day short of two months.
//     * <p>
//     * There are two equivalent ways of using this method.
//     * The first is to invoke this method.
//     * The second is to use {@link TemporalUnit#between(Temporal, Temporal)}:
//     * <pre>
//     *   // these two lines are equivalent
//     *   amount = start.until(end, MONTHS);
//     *   amount = MONTHS.between(start, end);
//     * </pre>
//     * The choice should be made based on which makes the code more readable.
//     * <p>
//     * The calculation is implemented in this method for {@link ChronoUnit}.
//     * The units {@code DAYS}, {@code WEEKS}, {@code MONTHS}, {@code YEARS},
//     * {@code DECADES}, {@code CENTURIES}, {@code MILLENNIA} and {@code ERAS}
//     * are supported. Other {@code ChronoUnit} values will throw an exception.
//     * <p>
//     * If the unit is not a {@code ChronoUnit}, then the result of this method
//     * is obtained by invoking {@code TemporalUnit.between(Temporal, Temporal)}
//     * passing {@code this} as the first argument and the converted input temporal
//     * as the second argument.
//     * <p>
//     * This instance is immutable and unaffected by this method call.
//     *
//     * @param endExclusive  the end date, exclusive, which is converted to a {@code LocalDate}, not null
//     * @param unit  the unit to measure the amount in, not null
//     * @return the amount of time between this date and the end date
//     * @throws DateTimeException if the amount cannot be calculated, or the end
//     *  temporal cannot be converted to a {@code LocalDate}
//     * @throws UnsupportedTemporalTypeException if the unit is not supported
//     * @throws ArithmeticException if numeric overflow occurs
//     */
//    @Override
//    public long until(Temporal endExclusive, TemporalUnit unit) {
//        LocalDate end = LocalDate.from(endExclusive);
//        if (unit instanceof ChronoUnit) {
//            switch ((ChronoUnit) unit) {
//                case DAYS: return daysUntil(end);
//                case WEEKS: return daysUntil(end) / 7;
//                case MONTHS: return monthsUntil(end);
//                case YEARS: return monthsUntil(end) / 12;
//                case DECADES: return monthsUntil(end) / 120;
//                case CENTURIES: return monthsUntil(end) / 1200;
//                case MILLENNIA: return monthsUntil(end) / 12000;
//                case ERAS: return end.getLong(ERA) - getLong(ERA);
//            }
//            throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
//        }
//        return unit.between(this, end);
//    }

    @Override // together (localdate), now (localdate)
    public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
        // e.g. 2 months range, 5 months passed: until()-> 5 * 30 / 60 = 2
        return temporal1Inclusive.until(temporal2Exclusive, cornerstoneType) * cornerstoneType.getDuration().toDays() / duration.toDays();
    }

    public int compareTo(TemporalUnit other) {
        return this.duration.compareTo(other.getDuration());
    }

    @NonNull
    @Override
    public String toString() {
        return namePlural;
    }
}
