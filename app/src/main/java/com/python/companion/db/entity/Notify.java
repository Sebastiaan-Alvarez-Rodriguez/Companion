package com.python.companion.db.entity;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.python.companion.util.MeasurementUtil;

import java.time.LocalDate;

@Entity
public class Notify {
    @PrimaryKey(autoGenerate = true)
    private long notifyID;

    /** ID of jubileum we have this notification for 'e.g. 42day-jubileum'*/
    @ForeignKey(entity = Measurement.class, parentColumns = "measurementID", childColumns = "jubileumID", onDelete = ForeignKey.CASCADE)
    private long jubileumID;

    /** Date on which we must notify the user */
    private @NonNull LocalDate notifyDate;

    /** Amount of units to notify before the actual date. Used to make editing possible */
    private long amount;

    /** ID of Measurement user expresses */
    @ForeignKey(entity = Measurement.class, parentColumns = "measurementID", childColumns = "measurementID")
    private long measurementID;


    @Ignore
    public Notify(long jubileumID, @NonNull LocalDate notifyDate, long amount, long measurementID) {
        this.jubileumID = jubileumID;
        this.notifyDate = notifyDate;
        this.amount = amount;
        this.measurementID = measurementID;
    }

    public Notify(long notifyID, long jubileumID, @NonNull LocalDate notifyDate, long amount, long measurementID) {
        this.notifyID = notifyID;
        this.jubileumID = jubileumID;
        this.notifyDate = notifyDate;
        this.amount = amount;
        this.measurementID = measurementID;
    }

    /**
     * Quickly assemble a Notify instance for given base measurement's next jubileum, amount of given Measurement before
     * @param base Measurement we make a Notify instance for
     * @param amount Amount of Measurement to subtract from jubileum date
     * @param picked Picked measurement, used to subtract <code>amount</code> measurements from the jubileum date
     * @return Constructed Notify
     */
    public static @NonNull Notify from(@NonNull Context context, Measurement base, long amount, @NonNull Measurement picked) {
        LocalDate jubileumDate = MeasurementUtil.futureInterval(base, MeasurementUtil.getTogether(context), 1);
        LocalDate notifyDate = jubileumDate.minus(amount, picked);

        if (notifyDate.isBefore(LocalDate.now())) // If we are too late to notify for this jubileum
            notifyDate.plus(1, base); // set to the next jubileum
        return new Notify(base.getMeasurementID(), notifyDate, amount, picked.getMeasurementID());
    }

    /**
     * Quickly assemble a Notify instance for given base measurement's next jubileum, on the date of the jubileum
     * @param base Measurement we make a Notify instance for
     * @return Constructed Notify
     */
    public static @NonNull Notify from(@NonNull Context context, Measurement base) {
        LocalDate jubileumDate = MeasurementUtil.futureInterval(base, MeasurementUtil.getTogether(context), 1);
        return new Notify(base.getMeasurementID(), jubileumDate, 0, MeasurementUtil.NoneMeasurementID());
    }

    /** Basic copy constructor */
    public static @NonNull Notify from(@NonNull Notify n) {
        return new Notify(n.getNotifyID(), n.getJubileumID(), n.getNotifyDate(), n.getAmount(), n.getMeasurementID());
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

    public long getMeasurementID() {
        return measurementID;
    }

    public void setMeasurementID(long measurementID) {
        this.measurementID = measurementID;
    }
}
