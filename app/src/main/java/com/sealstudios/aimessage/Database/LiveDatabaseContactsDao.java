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
public interface LiveDatabaseContactsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertContact(DatabaseContacts user);

    @Insert
    void insertMultipleContacts(List<DatabaseContacts> users);

    @Query("SELECT * FROM databasecontacts ORDER BY msg_time_stamp ASC")
    LiveData<List<DatabaseContacts>> getAll();

    @Query("SELECT * FROM databasecontacts ORDER BY user_time_stamp ASC")
    LiveData<List<DatabaseContacts>> getAllByStatus();

    @Query("SELECT * FROM databasecontacts WHERE user_id IN (:userIds)")
    LiveData<List<DatabaseContacts>> loadAllByIds(String[] userIds);

    @Query("SELECT * FROM databasecontacts WHERE user_name LIKE :name ORDER BY user_name")
    LiveData<List<DatabaseContacts>> findByName(String name);

    @Query("SELECT * FROM databasecontacts WHERE user_number = :number")
    LiveData<List<DatabaseContacts>> findByNumber(String number);

    @Query("SELECT * FROM databasecontacts WHERE user_id = :user_id")
    LiveData<DatabaseContacts> findById(String user_id);

    @Query("SELECT * FROM databasecontacts WHERE user_id = :user_id")
    DatabaseContacts returnById(String user_id);

    @Update
    void updateContact(DatabaseContacts contact);

    @Delete
    void deleteUser(DatabaseContacts contact);
}
