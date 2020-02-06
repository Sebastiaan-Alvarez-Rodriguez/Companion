package com.python.companion.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.python.companion.db.entity.Note;

import java.util.List;

@SuppressWarnings("unused")
@Dao
public interface DAONote {
    @Query("DELETE FROM Note WHERE name IN(:names)")
    void delete(String... names);

    @Query("UPDATE Note SET content = :content WHERE name = :name")
    void update(String name, String content);

    @Query("SELECT * FROM Note")
    LiveData<List<Note>> getAllLive();

    @Query("SELECT * FROM Note WHERE name = :name")
    Note get(String name);

    @Query("SELECT * FROM Note WHERE name = :name")
    LiveData<Note> getLive(String name);

    @Query("SELECT COUNT(*) from Note")
    int count();
}
