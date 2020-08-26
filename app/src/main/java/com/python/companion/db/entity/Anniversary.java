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
 * Structure representing an anniversary, a way to express the principle of time distance.
 * We use 'days', 'seconds', etc as anniversaries of time. These anniversaries form anniversaries when an integer amount
 * of them fit between a starting moment and the present moment.
 * Here, we create our own, custom anniversaries of time, to have custom anniversaries.
 * We do this by specifying our new anniversaries in terms of existing anniversaries (no need to re-invent the idea of 'time').
 *
 * @implNote Even though Room does not support unique columns, we want singular and plural names of anniversaries to be unique.
 * We enforce this in code when creating/updating anniversaries.
 */
@Entity
public class Anniversary implements TemporalUnit, EntityVisitor.Visitable {
    @PrimaryKey(autoGenerate = true)
    private long anniversaryID;

    private @NonNull String nameSingular, namePlural;
    private Duration duration;

    private long amount, precomputedamount;
    private long parentID;
    private ChronoUnit cornerstoneType;

    private boolean hasNotifications;

    private boolean canModify;

    /**
     * Shorthand function to construct a Anniversary, handling all inheritance
     * @param nameSingular Singular name for anniversary
     * @param namePlural Plural name for anniversary
     * @param amount User-inputted multiplier (e.g. for 42Days: amount=42. If 84Days is defined as 2 times 42Days, then amount=2)
     * @param parent Anniversary picked as parent
     */
    public static Anniversary createFrom(String nameSingular, String namePlural, long amount, @NonNull Anniversary parent) {
        return new Anniversary(
                        nameSingular,
                        namePlural,
                        parent.getDuration().multipliedBy(amount),
                        amount,
                        amount*parent.getPrecomputedamount(),
                        parent.getAnniversaryID(),
                        parent.getCornerstoneType(),
                        false,
                        true);
    }


    public Anniversary(long id, @NonNull String nameSingular, @NonNull String namePlural, Duration duration, long amount, long precomputedamount, long parentID, ChronoUnit cornerstoneType, boolean hasNotifications, boolean canModify) {
        this.anniversaryID = id;
        this.nameSingular = nameSingular;
        this.namePlural = namePlural;
        this.duration = duration;
        this.amount = amount;
        this.precomputedamount = precomputedamount;
        this.parentID = parentID;
        this.cornerstoneType = cornerstoneType;
        this.hasNotifications = hasNotifications;
        this.canModify = canModify;
    }

    /**
     * Initialize a date anniversary with singular, plural names, a duration and a cornerstone type.
     * The type will be used for comparisons, yielding the ability to use irregular durational types, such as months.
     * Prefer to use {@link #createFrom(String, String, long, Anniversary)} if possible instead of this function
     * @param cornerstoneType The normalized cornerstone type this represents. e.g.: ChronoUnit.MONTHS
     */
    public Anniversary(@NonNull String nameSingular, @NonNull String namePlural, Duration duration, long amount, long precomputedamount, long parentID, ChronoUnit cornerstoneType, boolean hasNotifications, boolean canModify) {
        this.nameSingular = nameSingular;
        this.namePlural = namePlural;
        this.duration = duration;
        this.amount = amount;
        this.precomputedamount = precomputedamount;
        this.parentID = parentID;
        this.cornerstoneType = cornerstoneType;
        this.hasNotifications = hasNotifications;
        this.canModify = canModify;
    }

    /**
     * Returns a basic anniversary template, with no fields filled with something intelligent.
     * Use this only if you will fill in all fields yourself at a later time
     */
    public static Anniversary template() {
        return new Anniversary(0, "", "", null, 0, 0, 0, null, false, true);
    }

    public long getAnniversaryID() {
        return anniversaryID;
    }

    public void setAnniversaryID(long anniversaryID) {
        this.anniversaryID = anniversaryID;
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

    public boolean getHasNotifications() {
        return hasNotifications;
    }

    public void setHasNotifications(boolean hasNotifications) {
        this.hasNotifications = hasNotifications;
    }

    public boolean getCanModify() {
        return canModify;
    }

    public void setCanModify(boolean canModify) {
        this.canModify = canModify;
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
