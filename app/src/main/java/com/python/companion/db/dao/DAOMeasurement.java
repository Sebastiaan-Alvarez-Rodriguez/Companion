package com.python.companion.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.python.companion.db.entity.Measurement;

import java.util.List;

@Dao
public interface DAOMeasurement {
    @Insert
    void insert(Measurement... measurements);

    @Delete
    void delete(Measurement... measurements);

    @Query("SELECT * FROM Measurement")
    LiveData<List<Measurement>> getAllLive();

    @Query("SELECT * FROM Measurement WHERE namePlural = :namePlural")
    Measurement get(String namePlural);

    @Query("SELECT COUNT(*) FROM MEASUREMENT")
    int count();
}
