package com.python.companion.db.entity;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;

import com.python.companion.R;
import com.python.companion.ui.notes.note.NoteType;
import com.python.companion.util.migration.EntityVisitor;

import java.time.Instant;

@SuppressWarnings("unused")
@Entity(primaryKeys = {"name"})
public class Note implements EntityVisitor.Visitable {
    private @NonNull String name, content;

    @Embedded @NonNull
    private Category category;

    private boolean secure;
    private byte[] iv;

    private Instant modified;

    private @NoteType.Type int type;

    private boolean favorite;

    public Note(@NonNull String name, @NonNull String content, @NonNull Category category, boolean secure, byte[] iv, int type, boolean favorite) {
        this.name = name;
        this.content = content;
        this.category = category;
        this.secure = secure;
        this.iv = iv;
        this.modified = Instant.now();
        this.type = type;
        this.favorite = favorite;
    }

    @Ignore
    public Note(@NonNull String name, @NonNull String content, @NonNull Category category, boolean secure, byte[] iv, Instant modified, int type, boolean favorite) {
        this(name, content, category, secure, iv, type, favorite);
        this.modified = modified;
    }

    @Ignore
    public Note(@NonNull String name, @NonNull String content) {
        this.name = name;
        this.content = content;

        this.category = new Category("<default>", R.color.colorPrimary);
        this.secure = false;
        this.iv = null;

        modified = Instant.now();
        type = NoteType.TYPE_NORMAL;
        favorite = false;
    }

    @Ignore
    public Note(@NonNull Note other) {
        this(other.name, other.content, new Category(other.category), other.secure, other.iv, other.modified, other.type, other.favorite);
    }

    /**
     * Returns a basic note template, with no fields filled with something intelligent.
     * Use this only if you will fill in all fields yourself at a later time
     */
    public static Note template() {
        return new Note("", "");
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
    public @NonNull Category getCategory() {
        return category;
    }

    /**
     * Function to set note category
     * @param category New category
     */
    public void setCategory(@NonNull Category category) {
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
    public @Nullable byte[] getIv() {
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
     * @return content type to render.
     * @see NoteType for possible types
     */
    public int getType() {
        return type;
    }

    /**
     * Sets content type to render.
     * @param type new type
     */
    public void setType(int type) {
        this.type = type;
    }

    /** @return whether this item is 'favorite' or not. Favorite items appear at the top of note listings in UI */
    public boolean isFavorite() {
        return favorite;
    }

    /**
     * Sets whether this item is 'favorite' or not. Favorite items appear at the top of note listings in UI
     * @param favorite {@code true} if this note is favorite, {@code false} otherwise
     */
    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
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

    @Override
    public void accept(@NonNull EntityVisitor visitor) {
        visitor.visit(this);
    }
}
