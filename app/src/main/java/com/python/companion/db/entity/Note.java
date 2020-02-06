package com.python.companion.db.entity;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;

/*
 * Class representing a note
 */
@SuppressWarnings("unused")
//@Entity(indices = {@Index(value = {"name"}, unique = true)})
@Entity(primaryKeys = {"name"})
public class Note {
    private @NonNull String name, content;

    public Note(@NonNull String name, @NonNull String content) {
        this.name = name;
        this.content = content;
    }
    /**
     * Function to get associated name
     * @return instance name
     */
    @CheckResult
    public @NonNull String getName() {
        return name;
    }

    /**
     * Function to set associated name
     * @param name New name
     */
    public void setName(@NonNull String name) {
        this.name = name;
    }

    /**
     * Function to get note content
     * @return object's streetname
     */
    @CheckResult
    public @NonNull String getContent() {
        return content;
    }

    /**
     * Function to set note content
     * @param content New content
     */
    public void setContent(@NonNull String content) {
        this.content = content;
    }

    /**
     * Function to compare an object to this supplier on equality
     * @param obj the object to be compared to this supplier
     * @return true if there is equality, false on inequality or if the object is not a supplier
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Note))
            return false;

        Note other = (Note) obj;
        return this.name.equals(other.name);
    }

    /**
     * @see String#hashCode()
     * @return hashCode of the object's id
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
