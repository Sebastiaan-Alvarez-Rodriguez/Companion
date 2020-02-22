package com.python.companion.db.entity;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Entity;

import com.python.companion.R;

import java.time.Instant;

/*
 * Class representing a note
 */
@SuppressWarnings("unused")
@Entity(primaryKeys = {"name"})
public class Note {
    private @NonNull String name, content;

    @Embedded
    private Category category;

    private boolean secure;
    private byte[] iv;

    private Instant modified;

    public Note(@NonNull String name, @NonNull String content) {
        this.name = name;
        this.content = content;

        this.category = new Category("", R.color.colorPrimary);
        this.secure = false;
        this.iv = null;

        modified = Instant.now();
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
     * Function to get note category
     */
    public Category getCategory() {
        return category;
    }

    /**
     * Function to set note category
     * @param category New category
     */
    public void setCategory(Category category) {
        this.category = category;
    }

    /**
     * @return current security status
     */
    public boolean isSecure() {
        return secure;
    }
    /**
     * Function to set note security. Note that setting security to 'none',
     * while it actually is secure will make you lose note content forever, and possibly generates crashes
     * @param secure New security state
     */
    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    /**
     * @return current Initialization Vector for block chain cipher encryption schemes
     */
    public byte[] getIv() {
        return iv;
    }

    /**
     * Sets Initialization Vector for block chain cipher encryption schemes.
     * Note that storing initialization vectors (and salts) is no security breach
     * @param iv New iv
     */
    public void setIv(byte[] iv) {
        this.iv = iv;
    }


    /**
     * Function to get note last modification date
     */
    public Instant getModified() {
        return modified;
    }

    /**
     * Function to set modified date and time
     * @param modified New date and time
     */
    public void setModified(Instant modified) {
        this.modified = modified;
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
