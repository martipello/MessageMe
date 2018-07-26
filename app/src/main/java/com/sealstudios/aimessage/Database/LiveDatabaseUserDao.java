package com.sealstudios.aimessage.Database;


import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface LiveDatabaseUserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(DatabaseUser user);

    @Insert
    void insertMultipleUsers(List<DatabaseUser> users);

    @Query("SELECT * FROM databaseuser")
    LiveData<List<DatabaseUser>> getAll();

    @Query("SELECT * FROM databaseuser WHERE user_id IN (:userIds)")
    LiveData<List<DatabaseUser>> loadAllByIds(String[] userIds);

    @Query("SELECT * FROM databaseuser WHERE user_name LIKE :name")
    LiveData<DatabaseUser> findByName(String name);

    @Query("SELECT * FROM databaseuser WHERE user_id = :user_id")
    LiveData<DatabaseUser> findById(String user_id);

    @Query("SELECT * FROM databaseuser WHERE user_id = :user_id")
    DatabaseUser returnUserById(String user_id);

    @Update
    void updateUser(DatabaseUser user);

    @Delete
    void deleteUser(DatabaseUser user);
}
