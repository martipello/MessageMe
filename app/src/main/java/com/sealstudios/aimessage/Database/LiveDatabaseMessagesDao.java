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
public interface LiveDatabaseMessagesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(DatabaseMessage databaseMessage);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMultipleMessages(List<DatabaseMessage> databaseMessages);

    @Query("SELECT * FROM databasemessage WHERE senderId = :sender OR senderId = :recipient ORDER BY time_stamp ASC")
    LiveData<List<DatabaseMessage>> getAll(String sender , String recipient);

    @Query("SELECT * FROM databasemessage WHERE senderId = :sender AND data_type = :type ORDER BY time_stamp ASC")
    LiveData<List<DatabaseMessage>> getAllByType(String sender, String type);

    @Query("SELECT * FROM databasemessage WHERE senderId = :sender AND time_stamp = :date ORDER BY time_stamp ASC")
    LiveData<List<DatabaseMessage>> findByDate(String sender, String date);

    @Query("SELECT * FROM databasemessage WHERE senderId = :sender ORDER BY time_stamp ASC")
    LiveData<List<DatabaseMessage>> findById(String sender);

    @Query("SELECT * FROM databasemessage WHERE senderId = :sender ORDER BY time_stamp ASC")
    LiveData<List<DatabaseMessage>> findByName(String sender);

    @Query("SELECT * FROM databasemessage WHERE senderId = :senderId OR recipientId = :senderId AND recipientId = :recipientId OR senderId = :recipientId  AND messageId = :messageId")
    LiveData<DatabaseMessage> returnById(String recipientId, String senderId, String messageId);

    @Query("SELECT * FROM databasemessage WHERE senderId = :senderId AND recipientId = :recipientId  AND sent_received < 2")
    List<DatabaseMessage> returnUnread(String recipientId, String senderId);

    @Query("SELECT * FROM databasemessage WHERE senderId = :sender OR recipientId = :sender AND message LIKE :message ORDER BY time_stamp ASC")
    LiveData<List<DatabaseMessage>> returnByText(String sender , String message);

    @Update
    void updateMessage(DatabaseMessage databaseMessage);

    @Delete
    void deleteMessage(DatabaseMessage databaseMessage);

    @Delete
    void deleteMultipleMessages(List<DatabaseMessage> databaseMessageList);
}
