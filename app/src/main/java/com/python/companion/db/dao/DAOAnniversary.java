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

import com.python.companion.db.entity.Anniversary;
import com.python.companion.db.pojo.anniversary.AnniversaryWithParentNames;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Dao
public abstract class DAOAnniversary {
    @Insert
    public abstract void insert(Anniversary... anniversaries);

    /**
     * Updating is protected, because inheritance trees must be modified in a specific way before updates can be allowed
     * Use {@link #updateInherit(Anniversary, boolean, boolean, boolean)} to update an anniversary, while guaranteeing structural tree integrity
     */
    @Update
    protected abstract void update(Anniversary... anniversaries);

    @Transaction
    public void upsert(Anniversary... anniversaries) {
        for (Anniversary m : anniversaries) {
            @Nullable Anniversary existing = getBySingularOrPlural(m.getNameSingular(), m.getNamePlural());
            if (existing == null) { // There currently exists no conflicting note in the database
                insert(m);
            } else { // A conflicting item exists, update it and any children
                updateInherit(m, existing);
            }
        }
    }
    /**
     * Deleting is protected, because inheritance trees must be modified in a specific way before deletion can be allowed
     * Use {@link #deleteInherit(Anniversary)} to delete an anniversary, while guaranteeing structural tree integrity
     */
    @Delete
    protected abstract void delete(Anniversary... anniversaries);

    @Query("SELECT * FROM Anniversary")
    public abstract List<Anniversary> getAll();

    @Query("SELECT * FROM Anniversary")
    public abstract LiveData<List<Anniversary>> getAllLive();

    /**
     * Constructed using: https://www.sqlitetutorial.net/sqlite-self-join/
     * @return List of anniversaries, with parent singular and plural names
     */
    @Query("SELECT m1.*, m2.nameSingular AS parentSingular, m2.namePlural AS parentPlural FROM Anniversary m1 LEFT JOIN Anniversary m2 ON m1.parentID = m2.anniversaryID")
    public abstract LiveData<List<AnniversaryWithParentNames>> getAllNamedLive();

    @Query("UPDATE Anniversary SET hasNotifications = :hasNotifications WHERE anniversaryID = :anniversaryID")
    public abstract void setHasNotifications(long anniversaryID, boolean hasNotifications);

    @Query("SELECT * FROM Anniversary WHERE nameSingular = :nameSingular")
    public abstract Anniversary getBySingular(String nameSingular);

    @Query("SELECT * FROM Anniversary WHERE namePlural = :namePlural")
    public abstract Anniversary getByPlural(String namePlural);

    @Query("SELECT * FROM Anniversary WHERE nameSingular = :nameSingular OR namePlural = :namePlural")
    public abstract Anniversary getBySingularOrPlural(String nameSingular, String namePlural);

    @Transaction
    public AnniversaryWithParentNames getBySingularOrPluralNamed(String nameSingular, String namePlural) {
        @Nullable Anniversary found = getBySingularOrPlural(nameSingular, namePlural);
        if (found == null)
            return null;
        return findByIDNamed(found.getAnniversaryID());
    }

    @Query("SELECT * FROM Anniversary WHERE anniversaryID = :id")
    public abstract Anniversary findByID(long id);

    /** Maps a stream of ID's to their anniversaries. ID's which are not found are mapped to {@code null} on their respective indices */
    @Transaction
    public List<Anniversary> findByID(Stream<Long> ids) {
        return ids.map(this::findByID).collect(Collectors.toList());
    }

    @Query("SELECT m1.*, m2.nameSingular AS parentSingular, m2.namePlural AS parentPlural FROM Anniversary m1 LEFT JOIN Anniversary m2 ON m1.parentID = m2.anniversaryID WHERE m1.anniversaryID = :id")
    public abstract AnniversaryWithParentNames findByIDNamed(long id);

    @Query("SELECT * FROM Anniversary WHERE parentID = :id")
    public abstract List<Anniversary> findChildren(long id);


    /** Shorthand function for {@link #updateInherit(Anniversary, boolean, boolean, boolean)}, maintaining tree structure integrity */
    public List<Anniversary> updateInherit(@NonNull Anniversary cur, @NonNull Anniversary old) {
        cur.setAnniversaryID(old.getAnniversaryID()); // Needed for the update-call to actually update
        boolean checkRecursion = (cur.getParentID() != old.getParentID());
        boolean changeCornerstone = (cur.getCornerstoneType() != old.getCornerstoneType());// Change cornerstonetype of children, too (and their duration)
        boolean changeAmount = (cur.getAmount() != old.getAmount());// handle amount change, and precomputedamount in self and children (and durations)
        return updateInherit(cur, checkRecursion, changeCornerstone, changeAmount);
    }

    /**
     * Update anniversary, simultaneously updating children and/or parents, depending on what is needed
     * @param cur Current anniversary
     * @param checkRecursion Check whether user tries to make an anniversary depend on itself. Should be checked iff {@code cur} has new parentID.
     * @param changeCornerstone Change cornerstonetype of all children of {@code cur}. Shoulb be done iff {@code cur} has new cornerstonetype
     * @param changeAmount Change precomputed
     * @return list of updated Anniversaries when update occurred, {@code null} otherwise (e.g. when user tries to make anniversary depend on itself
     */
    @Transaction
    protected @Nullable List<Anniversary> updateInherit(@NonNull Anniversary cur, boolean checkRecursion, boolean changeCornerstone, boolean changeAmount) {
        if (checkRecursion) {
            if (cur.getAnniversaryID() == cur.getParentID()) // Direct recursion
                return null;

            @Nullable Anniversary m = findByID(cur.getParentID());
            while (m != null) { // No cornerstone parent
                if (m.getAnniversaryID() == cur.getAnniversaryID()) // User tries to make recursive definition
                    return null;
                m = findByID(m.getParentID());
            }
        }

        ArrayList<Anniversary> updated = new ArrayList();

        if (changeCornerstone || changeAmount) {
            ArrayDeque<Anniversary> queue = new ArrayDeque<>();
            queue.add(cur);

            while (!queue.isEmpty()) {
                Anniversary parent = queue.poll();
                for (Anniversary child : findChildren(parent.getAnniversaryID())) {
                    if (changeCornerstone)
                        child.setCornerstoneType(cur.getCornerstoneType());
                    if (checkRecursion || changeAmount) { // if parentID changed or amount changed, must recompute precomputedAmount
                        long newAmount = child.getAmount() * parent.getPrecomputedamount();
                        child.setPrecomputedamount(newAmount);
                        child.setDuration(child.getCornerstoneType().getDuration().multipliedBy(newAmount));
                    }
                    update(child);
                    updated.add(child);

                    queue.add(child);
                }
            }
        }
        update(cur);
        updated.add(cur);

        return updated;
    }

    @Transaction
    public void deleteInherit(@NonNull Anniversary anniversary) {
        for (Anniversary child : findChildren(anniversary.getAnniversaryID())) {
            child.setParentID(anniversary.getParentID());
            child.setAmount(child.getAmount()* anniversary.getAmount());
            update(child);
        }
        delete(anniversary); // Note: Messages belonging to this anniversary are deleted automatically
    }

    @Query("SELECT COUNT(*) FROM Anniversary")
    public abstract int count();
}