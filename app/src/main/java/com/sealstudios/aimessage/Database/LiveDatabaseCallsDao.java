package com.sealstudios.aimessage.Database;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;
import android.telecom.Call;

import java.util.List;

@Dao
public interface LiveDatabaseCallsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCall(DatabaseCalls call);
    @Insert
    void insertMultipleCalls(List<DatabaseCalls> users);

    @Query("SELECT * FROM databasecalls ORDER BY call_time_stamp ASC")
    LiveData<List<DatabaseCalls>> getAllCalls();

    @Query("SELECT * FROM databasecalls WHERE call_caller_id IN (:userIds)")
    LiveData<List<DatabaseCalls>> loadAllCallsByIds(String[] userIds);

    @Query("SELECT * FROM databasecalls WHERE call_caller_name LIKE :name  OR call_called_name LIKE :name ORDER BY call_time_stamp")
    LiveData<List<DatabaseCalls>> findCallsByName(String name);

    @Query("SELECT * FROM databasecalls WHERE call_caller_id = :user_id OR call_called_id = :user_id")
    LiveData<DatabaseCalls> findCallsById(String user_id);

    @Transaction
    @Query("SELECT * FROM databasecalls ORDER BY call_time_stamp ASC")
    LiveData<List<CallObject>> loadCallsWithImages();

    @Transaction
    @Query("SELECT * FROM databasecalls WHERE call_caller_name LIKE :name  OR call_called_name LIKE :name ORDER BY call_time_stamp")
    LiveData<List<CallObject>> loadCallsWithImagesByName(String name);

    @Update
    void updateCall(DatabaseCalls calls);

    @Delete
    void deleteCall(DatabaseCalls calls);
}
