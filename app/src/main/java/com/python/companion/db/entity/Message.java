package com.python.companion.db.entity;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.python.companion.util.AnniversaryUtil;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Object representing a scheduled message, which will be displayed to the user
 * some amount of time before an anniversary, or on the anniversary itself.
 */
@Entity
public class Message {
    @PrimaryKey(autoGenerate = true)
    private long messageID;

    /** ID of anniversary we have this message for*/
    @ForeignKey(entity = Anniversary.class, parentColumns = "anniversaryID", childColumns = "anniversaryID", onDelete = ForeignKey.CASCADE)
    private long anniversaryID;

    /** Date on which we must Message the user */
    private @NonNull LocalDate messageDate;

    /** Amount of units this notification is sent before the anniversary date */
    private long amount;

    /** Unit type to give distance to anniversary in (e.g. MessageDate = anniversary_date - amount * type) */
    private ChronoUnit type;

    /** {@code true} if we count down every day from given messagedate to the anniversary*/
    private boolean countdown;

    @Ignore
    public Message(long anniversaryID, @NonNull LocalDate MessageDate, long amount, @NonNull ChronoUnit type, boolean countdown) {
        this.anniversaryID = anniversaryID;
        this.messageDate = MessageDate;
        this.amount = amount;
        this.type = type;
        this.countdown = countdown;
    }

    public Message(long messageID, long anniversaryID, @NonNull LocalDate messageDate, long amount, @NonNull ChronoUnit type, boolean countdown) {
        this(anniversaryID, messageDate, amount, type, countdown);
        this.messageID = messageID;
    }

    /**
     * Quickly assemble a Message for a given anniversary (base), some amount of units before next anniversary
     * @param base Anniversary we make a Message for
     * @param amount Amount of units to subtract from anniversary date
     * @param picked Unit used to subtract from anniversary date. Note: Make sure this unit is a base type
     * @param countdown Denotes whether constructed message will be a countdown message, providing a message every day until anniversary date
     * @return Constructed Message
     */
    public static @NonNull Message from(@NonNull Context context, Anniversary base, long amount, @NonNull Anniversary picked, boolean countdown) {
        LocalDate anniversaryDate = AnniversaryUtil.futureInterval(base, AnniversaryUtil.getTogether(context), 1);
        LocalDate MessageDate = anniversaryDate.minus(amount, picked);

        if (MessageDate.isBefore(LocalDate.now())) // If we are too late to Message for this anniversary
            MessageDate.plus(1, base); // set to the next anniversary

        return new Message(base.getAnniversaryID(), MessageDate, amount, AnniversaryUtil.getBaseChronoUnit(picked), countdown);
    }
    public static @NonNull Message from(@NonNull Context context, Anniversary base, long amount, @NonNull ChronoUnit picked, boolean countdown) {
        return from(context, base, amount, AnniversaryUtil.getBaseAnniversary(picked), countdown);
    }

    /**
     * Quickly assemble a Message for given base anniversary's next anniversary, on the date of the anniversary
     * @param Anniversary Anniversary we make a Message for
     * @return Constructed Message
     */
    public static @NonNull Message from(@NonNull Context context, Anniversary Anniversary, boolean countdown) {
        LocalDate anniversaryDate = AnniversaryUtil.futureInterval(Anniversary, AnniversaryUtil.getTogether(context), 1);
        return new Message(Anniversary.getAnniversaryID(), anniversaryDate, 0, ChronoUnit.DAYS, countdown);
    }

    public long getMessageID() {
        return messageID;
    }

    public void setMessageID(long MessageID) {
        this.messageID = MessageID;
    }

    public long getAnniversaryID() {
        return anniversaryID;
    }

    public void setAnniversaryID(long anniversaryID) {
        this.anniversaryID = anniversaryID;
    }

    public @NonNull LocalDate getMessageDate() {
        return messageDate;
    }

    public void setMessageDate(@NonNull LocalDate MessageDate) {
        this.messageDate = MessageDate;
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

    public boolean hasCountdown() {
        return countdown;
    }

    public void setCountdown(boolean countdown) {
        this.countdown = countdown;
    }
}
