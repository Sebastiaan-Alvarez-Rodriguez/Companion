package com.python.companion.db.entity;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.python.companion.util.MeasurementUtil;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
public class Notify {
    @PrimaryKey(autoGenerate = true)
    private long notifyID;

    /** ID of jubileum we have this notification for*/
    @ForeignKey(entity = Measurement.class, parentColumns = "measurementID", childColumns = "jubileumID", onDelete = ForeignKey.CASCADE)
    private long jubileumID;

    /** Date on which we must notify the user */
    private @NonNull LocalDate notifyDate;

    /** Amount of units this notification is sent before the anniversary date */
    private long amount;

    /** Unit type to give distance to jubileum in (e.g. notifyDate = jubileumDate - amount * type) */
    @ForeignKey(entity = Measurement.class, parentColumns = "measurementID", childColumns = "measurementID", onDelete = ForeignKey.CASCADE)
    private ChronoUnit type;


    @Ignore
    public Notify(long jubileumID, @NonNull LocalDate notifyDate, long amount, @NonNull ChronoUnit type) {
        this.jubileumID = jubileumID;
        this.notifyDate = notifyDate;
        this.amount = amount;
        this.type = type;
    }

    public Notify(long notifyID, long jubileumID, @NonNull LocalDate notifyDate, long amount, @NonNull ChronoUnit type) {
        this(jubileumID, notifyDate, amount, type);
        this.notifyID = notifyID;
    }

    /**
     * Quickly assemble a Notify instance for a moment before given base measurement's next jubileum
     * @param base Measurement we make a Notify instance for
     * @param amount Amount of units to subtract from jubileum date
     * @param picked Unit used to subtract from jubileum date. Make sure this unit is a base type
     * @return Constructed Notify
     */
    public static @NonNull Notify from(@NonNull Context context, Measurement base, long amount, @NonNull Measurement picked) {
        LocalDate jubileumDate = MeasurementUtil.futureInterval(base, MeasurementUtil.getTogether(context), 1);
        LocalDate notifyDate = jubileumDate.minus(amount, picked);

        if (notifyDate.isBefore(LocalDate.now())) // If we are too late to notify for this jubileum
            notifyDate.plus(1, base); // set to the next jubileum

        return new Notify(base.getMeasurementID(), notifyDate, amount, MeasurementUtil.getBaseChronoUnit(picked));
    }

    /**
     * Quickly assemble a Notify instance for given base measurement's next jubileum, on the date of the jubileum
     * @param base Measurement we make a Notify instance for
     * @return Constructed Notify
     */
    public static @NonNull Notify from(@NonNull Context context, Measurement base) {
        LocalDate jubileumDate = MeasurementUtil.futureInterval(base, MeasurementUtil.getTogether(context), 1);
        return new Notify(base.getMeasurementID(), jubileumDate, 0, ChronoUnit.DAYS);
    }

    public long getNotifyID() {
        return notifyID;
    }

    public void setNotifyID(long notifyID) {
        this.notifyID = notifyID;
    }

    public long getJubileumID() {
        return jubileumID;
    }

    public void setJubileumID(long jubileumID) {
        this.jubileumID = jubileumID;
    }

    public @NonNull LocalDate getNotifyDate() {
        return notifyDate;
    }

    public void setNotifyDate(@NonNull LocalDate notifyDate) {
        this.notifyDate = notifyDate;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public ChronoUnit getType() {
        return type;
    }

    public void setType(ChronoUnit type) {
        this.type = type;
    }
}
