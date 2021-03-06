package com.python.companion.db.dao;

import androidx.annotation.ColorInt;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;
import com.python.companion.ui.notes.note.NoteType;

import java.util.Collection;
import java.util.List;

@SuppressWarnings("unused")
@Dao
public abstract class DAONote {
    @Insert
    public abstract void insert(Note... notes);
    @Update
    public abstract void update(Note... notes);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void upsert(Note... notes);
    @Delete
    public abstract void delete(Note... notes);

    @Query("DELETE FROM Note WHERE secure = 1")
    public abstract void deleteSecure();

    @Transaction
    public void replace(String name, Note note) {
        delete(new Note(name, ""));
        insert(note);
    }

    @Query("UPDATE Note SET name = :name, content = :content WHERE name = :prevName")
    public abstract void updateContent(String prevName, String name, String content);

    @Query("UPDATE Note SET favorite = :isFavorite WHERE name = :name")
    public abstract void updateFavorite(String name, boolean isFavorite);

    @Query("UPDATE Note SET categoryName = :categoryName, categoryColor = :categoryColor WHERE name = :name")
    public abstract void updateCategory(String name, String categoryName, @ColorInt int categoryColor);

    @Transaction
    public void updateCategories(Collection<String> names, Category category) {
        for (String name : names)
            updateCategory(name, category.getCategoryName(), category.getCategoryColor());
    }

    @Query("UPDATE Note SET categoryName = :categoryName, categoryColor = :categoryColor WHERE categoryName = :prevCategoryName")
    public abstract void updateEntireCategory(String prevCategoryName, String categoryName, @ColorInt int categoryColor);

    @Query("UPDATE Note SET type = :type WHERE name = :name")
    public abstract void updateType(String name, @NoteType.Type int type);

    @Query("SELECT * FROM Note")
    public abstract LiveData<List<Note>> getAllLive();

    @Query("SELECT * FROM Note WHERE name = :name")
    public abstract Note get(String name);

    @Query("SELECT * FROM Note")
    public abstract List<Note> getAll();

    @Query("SELECT * FROM Note WHERE secure = 0")
    public abstract List<Note> getInsecure();

    @Query("SELECT * FROM Note WHERE secure = 1")
    public abstract List<Note> getSecure();

    @Query("SELECT * FROM Note WHERE name = :name")
    public abstract LiveData<Note> getLive(String name);
    @Query("SELECT categoryName,categoryColor FROM Note WHERE name = :name")
    public abstract LiveData<Category> getCategoryLive(String name);

    @Query("SELECT COUNT(*) FROM Note WHERE secure = 0")
    public abstract int countInsecure();

    @Query("SELECT COUNT(*) FROM Note")
    public abstract int count();
}
