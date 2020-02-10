package com.python.companion.db.dao;

import androidx.annotation.ColorInt;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;

import java.util.List;

@SuppressWarnings("unused")
@Dao
public interface DAONote {
    @Insert
    void insert(Note... notes);
    @Delete
    void delete(Note... notes);
//    @Query("DELETE FROM Note WHERE name IN(:names)")
//    void delete(String... names);

    @Query("UPDATE Note SET content = :content WHERE name = :name")
    void updateContent(String name, String content);

    @Query("UPDATE Note SET name = :name, content = :content WHERE name = :prevName")
    void updateContent(String prevName, String name, String content);

    @Query("UPDATE Note SET categoryName = :categoryName, categoryColor = :categoryColor WHERE name = :name")
    void updateCategory(String name, String categoryName, @ColorInt int categoryColor);

    @Query("UPDATE Note SET categoryName = :categoryName, categoryColor = :categoryColor WHERE categoryName = :prevCategoryName")
    void updateEntireCategory(String prevCategoryName, String categoryName, @ColorInt int categoryColor);

    @Query("SELECT * FROM Note")
    LiveData<List<Note>> getAllLive();

    @Query("SELECT * FROM Note WHERE name = :name")
    Note get(String name);

    @Query("SELECT * FROM Note WHERE name = :name")
    LiveData<Note> getLive(String name);

    @Query("SELECT categoryName,categoryColor FROM Note WHERE name = :name")
    LiveData<Category> getCategoryLive(String name);

    @Query("SELECT COUNT(*) from Note")
    int count();
}
