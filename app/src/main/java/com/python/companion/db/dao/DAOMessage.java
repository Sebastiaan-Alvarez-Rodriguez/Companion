package com.python.companion.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.python.companion.db.entity.Message;

import java.time.LocalDate;
import java.util.List;

@Dao
public abstract class DAOMessage {
    @Insert
    public abstract void insert(Message... messages);

    @Update
    public abstract void update(Message... messages);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void upsert(Message... messages);
    @Delete
    public abstract void delete(Message... messages);

    @Query("SELECT * FROM Message WHERE anniversaryID = :anniversaryID")
    public abstract List<Message> getMessagesForAnniversary(long anniversaryID);

    @Query("SELECT * FROM Message WHERE anniversaryID = :anniversaryID")
    public abstract LiveData<List<Message>> getMessagesForAnniversaryLive(long anniversaryID);

    /** Returns all messages with a date before or on given date */
    @Query("SELECT * FROM Message WHERE messageDate <= :date")
    public abstract List<Message> getDues(LocalDate date);

    /** Shorthand for {@link #getDues(LocalDate)}, with today as date argument (gets all due messages on this day) */
    @Transaction
    public List<Message> getDues() {
        return getDues(LocalDate.now());
    }

    /** Returns a message for a specific anniversary (using anniversaryID), where message is scheduled on given date */
    @Query("SELECT * FROM Message WHERE anniversaryID = :anniversaryID AND countdown = 0 AND messageDate = :messageDate")
    public abstract Message findConflicting(long anniversaryID, LocalDate messageDate);

    /** Returns the countdown message for a specific anniversary (using anniversaryID) */
    @Query("SELECT * FROM Message WHERE anniversaryID = :anniversaryID AND countdown = 1")
    public abstract Message findCountdown(long anniversaryID);

    /** Returns {@code true} if there is at least one message scheduled for given anniversary, false otherwise*/
    @Query("SELECT EXISTS(SELECT 1 FROM Message WHERE anniversaryID = :anniversaryID)")
    public abstract boolean checkHasMessages(long anniversaryID);
}
