package com.python.companion.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.python.companion.util.migration.EntityVisitor;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;

/**
 * Structure representing a measurement, a way to express the principle of time.
 * We use 'days', 'seconds', etc as measurements of time. Here, we create our own, custom measurements.
 * We do this by specifying our new measurement in terms of existing measurements (no need to re-invent the idea of 'time').
 *
 * @implNote Even though Room does not support unique columns, we want singular and plural names of measurements to be unique.
 * We enforce this in code on creation of and when updating measurements.
 */
@Entity
public class Measurement implements TemporalUnit, EntityVisitor.Visitable {
    @PrimaryKey(autoGenerate = true)
    long measurementID;

    private @NonNull String nameSingular, namePlural;
    private Duration duration;

    private long amount, precomputedamount;
    private long parentID;
    private ChronoUnit cornerstoneType;

    /**
     * Shorthand function to construct a Measurement, handling all inheritance
     * @param nameSingular Singular name for measurement
     * @param namePlural Plural name for measurement
     * @param amount User-inputted multiplier (e.g. for 42Days: amount=42. If 84Days is defined as 2 times 42Days, then amount=2)
     * @param parent Measurement picked as parent
     */
    public static Measurement createFrom(String nameSingular, String namePlural, long amount, @NonNull Measurement parent) {
        return new Measurement(
                        nameSingular,
                        namePlural,
                        parent.getDuration().multipliedBy(amount),
                        amount,
                        amount*parent.getPrecomputedamount(),
                        parent.getMeasurementID(),
                        parent.getCornerstoneType());
    }


    public Measurement(long id, @NonNull String nameSingular, @NonNull String namePlural, Duration duration, long amount, long precomputedamount, long parentID, ChronoUnit cornerstoneType) {
        this.measurementID = id;
        this.nameSingular = nameSingular;
        this.namePlural = namePlural;
        this.duration = duration;
        this.amount = amount;
        this.precomputedamount = precomputedamount;
        this.parentID = parentID;
        this.cornerstoneType = cornerstoneType;
    }

    /**
     * Initialize a date measurement with singular, plural names, a duration and a cornerstone type.
     * The type will be used for comparisons, yielding the ability to use irregular durational types, such as months.
     * Prefer to use {@link #createFrom(String, String, long, Measurement)} if possible instead of this function
     * @param cornerstoneType The normalized cornerstone type this represents. e.g.: ChronoUnit.MONTHS
     */
    public Measurement(@NonNull String nameSingular, @NonNull String namePlural, Duration duration, long amount, long precomputedamount, long parentID, ChronoUnit cornerstoneType) {
        this.nameSingular = nameSingular;
        this.namePlural = namePlural;
        this.duration = duration;
        this.amount = amount;
        this.precomputedamount = precomputedamount;
        this.parentID = parentID;
        this.cornerstoneType = cornerstoneType;
    }

    public long getMeasurementID() {
        return measurementID;
    }

    public void setMeasurementID(long measurementID) {
        this.measurementID = measurementID;
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

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public long getPrecomputedamount() {
        return precomputedamount;
    }

    public void setPrecomputedamount(long precomputedamount) {
        this.precomputedamount = precomputedamount;
    }

    public long getParentID() {
        return parentID;
    }

    public void setParentID(long parentID) {
        this.parentID = parentID;
    }

    public ChronoUnit getCornerstoneType() {
        return cornerstoneType;
    }

    public void setCornerstoneType(ChronoUnit cornerstoneType) {
        this.cornerstoneType = cornerstoneType;
    }

    @Override
    public boolean isDurationEstimated() {
        return this.cornerstoneType.isDurationEstimated();
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
        return (R) temporal.plus(amount*precomputedamount, cornerstoneType);
    }

    @Override
    public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
        return temporal1Inclusive.until(temporal2Exclusive, cornerstoneType) / precomputedamount;
    }

    public int compareTo(TemporalUnit other) {
        return duration.compareTo(other.getDuration());
    }

    @NonNull
    @Override
    public String toString() {
        return namePlural;
    }

    @Override
    public void accept(@NonNull EntityVisitor visitor) {
        visitor.visit(this);
    }
}
