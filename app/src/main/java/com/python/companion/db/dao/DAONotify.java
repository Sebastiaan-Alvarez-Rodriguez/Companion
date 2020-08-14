package com.python.companion.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.python.companion.db.entity.Notify;
import com.python.companion.db.pojo.notify.NotifyWithMeasurementNames;

import java.time.LocalDate;
import java.util.List;

@Dao
public abstract class DAONotify {
    @Insert
    public abstract void insert(Notify... notifications);

    @Update
    public abstract void update(Notify... notifies);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void upsert(Notify... notifies);
    @Delete
    public abstract void delete(Notify... notifies);

    @Query("SELECT n.*, m.nameSingular AS measurementSingular, m.namePlural AS measurementPlural FROM Notify n LEFT JOIN Measurement m ON m.measurementID = n.measurementID WHERE jubileumID = :jubileumID")
    public abstract LiveData<List<NotifyWithMeasurementNames>> getForJubileumNamed(long jubileumID);

    /** Returns all notifies with a date before or on given date */
    @Query("SELECT * FROM Notify WHERE notifyDate <= :date")
    public abstract List<Notify> getDues(LocalDate date);

    /** Shorthand for {@link #getDues(LocalDate)}, with today as date argument (gets all due notifies on this day) */
    @Transaction
    public List<Notify> getDues() {
        return getDues(LocalDate.now());
    }

    /** Returns a notification for a specific jubileum (using jubileumID), where notification is scheduled on given date */
    @Query("SELECT * FROM Notify WHERE jubileumID = :jubileumID AND notifyDate = :notifyDate")
    public abstract Notify findConflicting(long jubileumID, LocalDate notifyDate);

    /** Returns {@code true} if there is at least one notification for given measurement, false otherwise*/
    @Query("SELECT EXISTS(SELECT 1 FROM Notify WHERE jubileumID = :measurementID)")
    public abstract boolean checkHasNotifications(long measurementID);
}
