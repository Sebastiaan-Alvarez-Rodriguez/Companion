package com.python.companion.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity(primaryKeys = {"measurementID", "notifyDate"})
public class Notify {
    @ForeignKey(entity = Measurement.class, parentColumns = "measurementID", childColumns = "measurementID", onDelete = ForeignKey.CASCADE)
    private long measurementID;

    private @NonNull LocalDate notifyDate, jubileumDate;

    private long amount;
    private ChronoUnit cornerstoneType;

    public Notify(long measurementID, @NonNull LocalDate jubileumDate, @NonNull LocalDate notifyDate) {
        this.measurementID = measurementID;
        this.notifyDate = notifyDate;
        this.jubileumDate = jubileumDate;
    }

    public long getMeasurementID() {
        return measurementID;
    }

    public void setMeasurementID(long measurementID) {
        this.measurementID = measurementID;
    }

    public @NonNull LocalDate getNotifyDate() {
        return notifyDate;
    }

    public void setNotifyDate(@NonNull LocalDate notifyDate) {
        this.notifyDate = notifyDate;
    }

    public @NonNull LocalDate getJubileumDate() {
        return jubileumDate;
    }

    public void setJubileumDate(@NonNull LocalDate jubileumDate) {
        this.jubileumDate = jubileumDate;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public ChronoUnit getCornerstoneType() {
        return cornerstoneType;
    }

    public void setCornerstoneType(ChronoUnit cornerstoneType) {
        this.cornerstoneType = cornerstoneType;
    }

    /** Returns a unique string for notifications. Can also be used to cancel notifications */
    public String getUniqueID() {
        return String.valueOf(measurementID)+ ChronoUnit.DAYS.between(notifyDate, jubileumDate);
    }
}
