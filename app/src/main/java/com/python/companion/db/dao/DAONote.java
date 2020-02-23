package com.python.companion.db.dao;

import androidx.annotation.ColorInt;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;
import com.python.companion.ui.note.NoteType;

import java.util.Collection;
import java.util.List;

@SuppressWarnings("unused")
@Dao
public abstract class DAONote {
    @Insert
    public abstract void insert(Note... notes);
    @Update
    public abstract void update(Note... notes);
    @Delete
    public abstract void delete(Note... notes);
//    @Query("DELETE FROM Note WHERE name IN(:names)")
//    void delete(String... names);

    @Query("UPDATE Note SET content = :content WHERE name = :name")
    public abstract void updateContent(String name, String content);

    @Query("UPDATE Note SET name = :name, content = :content WHERE name = :prevName")
    public abstract void updateContent(String prevName, String name, String content);


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

//    @Query("SELECT isSecure, iv FROM Note WHERE name = :name")
//    public abstract Security getSecurity(String name);

    @Query("SELECT * FROM Note WHERE name = :name")
    public abstract LiveData<Note> getLive(String name);
    @Query("SELECT categoryName,categoryColor FROM Note WHERE name = :name")
    public abstract LiveData<Category> getCategoryLive(String name);

    @Query("SELECT COUNT(*) from Note")
    public abstract int count();
}
