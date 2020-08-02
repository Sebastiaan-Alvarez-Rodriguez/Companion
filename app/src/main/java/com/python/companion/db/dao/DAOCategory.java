package com.python.companion.db.dao;

import androidx.annotation.ColorInt;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.python.companion.db.entity.Category;

import java.util.List;

@Dao
public interface DAOCategory {
    @Insert
    void insert(Category... categories);

    @Query("UPDATE Category SET categoryColor = :categoryColor WHERE categoryName = :categoryName")
    void update(String categoryName, @ColorInt int categoryColor);

    @Query("UPDATE Category SET categoryName = :categoryName, categoryColor = :categoryColor WHERE categoryName = :prevName")
    void update(String prevName, String categoryName, @ColorInt int categoryColor);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(Category... categories);

    @Delete
    void delete(Category... categories);

    @Query("SELECT * FROM Category")
    List<Category> getAll();

    @Query("SELECT * FROM Category")
    LiveData<List<Category>> getAllLive();

    @Query("SELECT * FROM Category WHERE categoryName = :categoryName")
    Category get(String categoryName);

    @Query("SELECT * FROM Category WHERE categoryName = :categoryName")
    LiveData<Category> getLive(String categoryName);

    @Query("SELECT COUNT(*) from Category")
    long count();
}
