package com.python.companion.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Update;

import com.python.companion.db.entity.Notify;

@Dao
public abstract class DAONotify {
    @Insert
    public abstract void insert(Notify... notifications);

    @Update
    public abstract void update(Notify... notifies);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void upsert(Notify... notifies);
    @Delete
    public abstract void delete(Notify... notifies);
}
