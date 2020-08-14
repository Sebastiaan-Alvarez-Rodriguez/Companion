package com.python.companion.db.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.python.companion.db.entity.Measurement;
import com.python.companion.db.pojo.measurement.MeasurementWithParentNames;

import java.util.ArrayDeque;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Dao
public abstract class DAOMeasurement {
    @Insert
    public abstract void insert(Measurement... measurements);

    /**
     * Updating is protected, because inheritance trees must be modified in a specific way before updates can be allowed
     * Use {@link #updateInherit(Measurement, boolean, boolean, boolean)} to update a measurement, while guaranteeing structural tree integrity
     */
    @Update
    protected abstract void update(Measurement... measurements);

    @Transaction
    public void upsert(Measurement... measurements) {
        for (Measurement m : measurements) {
            @Nullable Measurement existing = getBySingularOrPlural(m.getNameSingular(), m.getNamePlural());
            if (existing == null) { // There currently exists no conflicting note in the database
                insert(m);
            } else { // A conflicting item exists, update it and any children
                updateInherit(m, existing);
            }
        }
    }
    /**
     * Deleting is protected, because inheritance trees must be modified in a specific way before deletion can be allowed
     * Use {@link #deleteInherit(Measurement)} to delete a measurement, while guaranteeing structural tree integrity
     */
    @Delete
    protected abstract void delete(Measurement... measurements);

    @Query("SELECT * FROM Measurement")
    public abstract List<Measurement> getAll();

    @Query("SELECT * FROM Measurement")
    public abstract LiveData<List<Measurement>> getAllLive();

    /**
     * Constructed using: https://www.sqlitetutorial.net/sqlite-self-join/
     * @return List of measurements, with parent singular and plural names
     */
    @Query("SELECT m1.*, m2.nameSingular AS parentSingular, m2.namePlural AS parentPlural FROM Measurement m1 LEFT JOIN Measurement m2 ON m1.parentID = m2.measurementID")
    public abstract LiveData<List<MeasurementWithParentNames>> getAllNamedLive();

    @Query("UPDATE Measurement SET hasNotifications = :hasNotifications WHERE measurementID = :measurementID")
    public abstract void setHasNotifications(long measurementID, boolean hasNotifications);

    @Query("SELECT * FROM Measurement WHERE nameSingular = :nameSingular")
    public abstract Measurement getBySingular(String nameSingular);

    @Query("SELECT * FROM Measurement WHERE namePlural = :namePlural")
    public abstract Measurement getByPlural(String namePlural);

    @Query("SELECT * FROM Measurement WHERE nameSingular = :nameSingular OR namePlural = :namePlural")
    public abstract Measurement getBySingularOrPlural(String nameSingular, String namePlural);

    @Transaction
    public MeasurementWithParentNames getBySingularOrPluralNamed(String nameSingular, String namePlural) {
        @Nullable Measurement found = getBySingularOrPlural(nameSingular, namePlural);
        if (found == null)
            return null;
        return findByIDNamed(found.getMeasurementID());
    }

    @Query("SELECT * FROM Measurement WHERE measurementID = :id")
    public abstract Measurement findByID(long id);

    /** Maps a stream of ID's to their measurements. ID's which are not found are mapped to {@code null} on their respective indices */
    @Transaction
    public List<Measurement> findByID(Stream<Long> ids) {
        return ids.map(this::findByID).collect(Collectors.toList());
    }

    @Query("SELECT m1.*, m2.nameSingular AS parentSingular, m2.namePlural AS parentPlural FROM Measurement m1 LEFT JOIN Measurement m2 ON m1.parentID = m2.measurementID WHERE m1.measurementID = :id")
    public abstract MeasurementWithParentNames findByIDNamed(long id);

    @Query("SELECT * FROM Measurement WHERE parentID = :id")
    public abstract List<Measurement> findChildren(long id);


    /** Shorthand function for {@link #updateInherit(Measurement, boolean, boolean, boolean)}, maintaining tree structure integrity */
    public boolean updateInherit(@NonNull Measurement cur, @NonNull Measurement old) {
        cur.setMeasurementID(old.getMeasurementID()); // Needed for the update-call to actually update
        boolean checkRecursion = (cur.getParentID() != old.getParentID());
        boolean changeCornerstone = (cur.getCornerstoneType() != old.getCornerstoneType());// Change cornerstonetype of children, too (and their duration)
        boolean changeAmount = (cur.getAmount() != old.getAmount());// handle amount change, and precomputedamount in self and children (and durations)
        return updateInherit(cur, checkRecursion, changeCornerstone, changeAmount);
    }

    /**
     * Update measurement, simultaneously updating children and/or parents, depending on what is needed
     * @param cur Current measurement
     * @param checkRecursion Check whether user tries to make a measurement depend on itself. Should be checked iff {@code cur} has new parentID.
     * @param changeCornerstone Change cornerstonetype of all children of {@code cur}. Shoulb be done iff {@code cur} has new cornerstonetype
     * @param changeAmount Change precomputed
     * @return {@code true} when update occurred, {@code false} otherwise (e.g. when user tries to make measurement depend on itself
     */
    @Transaction
    protected boolean updateInherit(@NonNull Measurement cur, boolean checkRecursion, boolean changeCornerstone, boolean changeAmount) {
        if (checkRecursion) {
            if (cur.getMeasurementID() == cur.getParentID()) // Direct recursion
                return false;

            @Nullable Measurement m = findByID(cur.getParentID());
            while (m != null) { // No cornerstone parent
                if (m.getMeasurementID() == cur.getMeasurementID()) // User tries to make recursive definition
                    return false;
                m = findByID(m.getParentID());
            }
        }
        if (changeCornerstone || changeAmount) {
            ArrayDeque<Measurement> queue = new ArrayDeque<>();
            queue.add(cur);

            while (!queue.isEmpty()) {
                Measurement parent = queue.poll();
                for (Measurement child : findChildren(parent.getMeasurementID())) {
                    if (changeCornerstone)
                        child.setCornerstoneType(cur.getCornerstoneType());
                    if (checkRecursion || changeAmount) { // if parentID changed or amount changed, must recompute precomputedAmount
                        long newAmount = child.getAmount() * parent.getPrecomputedamount();
                        child.setPrecomputedamount(newAmount);
                        child.setDuration(child.getCornerstoneType().getDuration().multipliedBy(newAmount));
                    }
                    update(child);
                    queue.add(child);
                }
            }
        }
        update(cur);
        return true;
    }

    @Transaction
    public void deleteInherit(@NonNull Measurement measurement) {
        for (Measurement child : findChildren(measurement.getMeasurementID())) {
            child.setParentID(measurement.getParentID());
            child.setAmount(child.getAmount()*measurement.getAmount());
            update(child);
        }
        delete(measurement);
    }

    @Query("SELECT COUNT(*) FROM MEASUREMENT")
    public abstract int count();
}