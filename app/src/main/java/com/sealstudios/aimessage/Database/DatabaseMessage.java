package com.sealstudios.aimessage.Database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverter;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import com.sealstudios.aimessage.Utils.Constants;

import java.util.Date;

@Entity
public class DatabaseMessage {

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = Constants.MSG_ID)
    private String messageId;
    @ColumnInfo(name = Constants.MSG_SENDER)
    private String senderId;
    @ColumnInfo(name = Constants.MSG_TEXT)
    private String message;
    @ColumnInfo(name = Constants.MSG_TIME_STAMP)
    @TypeConverters({Converters.class})
    private Date time_stamp;
    @ColumnInfo(name = Constants.MSG_DATA_TYPE)
    private String data_type;
    @ColumnInfo(name = Constants.MSG_DATA_URL)
    private String data_url;
    @ColumnInfo(name = Constants.MSG_RECIPIENT)
    private String recipientId;
    @ColumnInfo(name = Constants.MSG_SENDER_NAME)
    private String senderName;
    @ColumnInfo(name = Constants.MSG_SENT_RECEIVED)
    private int sent_received;
    @ColumnInfo(name = Constants.MSG_RECIPIENT_NAME)
    private String recipientName;


    public DatabaseMessage() {
    }

    public static class Converters {
        @TypeConverter
        public Date fromTimestamp(Long value) {
            return value == null ? null : new Date(value);
        }

        @TypeConverter
        public Long dateToTimestamp(Date date) {
            if (date == null) {
                return null;
            } else {
                return date.getTime();
            }
        }
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getTime_stamp() {
        return time_stamp;
    }

    public void setTime_stamp(Date time_stamp) {
        this.time_stamp = time_stamp;
    }

    public String getData_type() {
        return data_type;
    }

    public void setData_type(String data_type) {
        this.data_type = data_type;
    }

    public String getData_url() {
        return data_url;
    }

    public void setData_url(String data_url) {
        this.data_url = data_url;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public int getSent_received() {
        return sent_received;
    }

    public void setSent_received(int sent_received) {
        this.sent_received = sent_received;
    }

    @Override
    public boolean equals(Object otherMsgObject) {
        if (!(otherMsgObject instanceof DatabaseMessage)) {
            return false;
        }
        DatabaseMessage that = (DatabaseMessage) otherMsgObject;
            return this.messageId.equals(that.messageId)
                    && this.message.equals(that.message)
                    && this.data_type.equals(that.data_type)
                    && this.data_url.equals(that.data_url)
                    && this.senderId.equals(that.senderId)
                    && this.recipientId.equals(that.recipientId)
                    && this.recipientName.equals(that.recipientName)
                    && this.senderName.equals(that.senderName)
                    && this.time_stamp.equals(that.time_stamp)
                    && this.sent_received == that.sent_received
                    ;

    }

}

