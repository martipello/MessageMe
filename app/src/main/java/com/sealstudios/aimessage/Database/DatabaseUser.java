package com.sealstudios.aimessage.Database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverter;
import android.arch.persistence.room.TypeConverters;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.sealstudios.aimessage.Utils.Constants;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
public class DatabaseUser implements Parcelable{

    public static final Creator CREATOR = new Creator() {
        public DatabaseUser createFromParcel(Parcel parcel) {
            return new DatabaseUser(parcel);
        }

        public DatabaseUser[] newArray(int size) {
            return new DatabaseUser[size];
        }
    };

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = Constants.FS_ID)
    private String user_id;

    @ColumnInfo(name = Constants.FS_NAME)
    private String user_name;

    @ColumnInfo(name = Constants.FS_STATUS)
    private String user_status;

    @ColumnInfo(name = Constants.FS_NUMBER)
    private String user_number;

    @ColumnInfo(name = Constants.FS_IMAGE)
    private String user_image;

    @ColumnInfo(name = Constants.FS_SMALL_IMAGE)
    private String user_small_image;

    @ColumnInfo(name = Constants.FS_TIME_STAMP)
    @TypeConverters({Converters.class})
    private Date user_time_stamp;

    @ColumnInfo(name = Constants.FS_RECENT_MSG)
    private String user_recent_message;

    @ColumnInfo(name = Constants.FS_MSG_TIME_STAMP)
    @TypeConverters({Converters.class})
    private Date msg_time_stamp;

    @ColumnInfo(name = Constants.FS_BLOCKED)
    private Boolean blocked;

    @ColumnInfo(name = Constants.FS_UNREAD)
    private int unread;

    public DatabaseUser() {
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

    public String getUser_name() {
        return this.user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getUser_status() {
        return this.user_status;
    }

    public void setUser_status(String user_status) {
        this.user_status = user_status;
    }

    public String getUser_id() {
        return this.user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getUser_number() {
        return this.user_number;
    }

    public void setUser_number(String user_number) {
        this.user_number = user_number;
    }

    public String getUser_image() {
        return this.user_image;
    }

    public void setUser_image(String user_image) {
        this.user_image = user_image;
    }

    public String getUser_small_image() {
        return user_small_image;
    }

    public void setUser_small_image(String user_small_image) {
        this.user_small_image = user_small_image;
    }

    public Date getUser_time_stamp() {
        return this.user_time_stamp;
    }

    public void setUser_time_stamp(Date user_time_stamp) {
        this.user_time_stamp = user_time_stamp;
    }

    public String getUser_recent_message() {
        return this.user_recent_message;
    }

    public void setUser_recent_message(String user_recent_message) {
        this.user_recent_message = user_recent_message;
    }

    public Boolean getBlocked() {
        return this.blocked;
    }

    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }

    public int getUnread() {
        return this.unread;
    }

    public void setUnread(int unread) {
        this.unread = unread;
    }

    public Date getMsg_time_stamp() {
        return this.msg_time_stamp;
    }

    public void setMsg_time_stamp(Date msg_time_stamp) {
        this.msg_time_stamp = msg_time_stamp;
    }

    public Map<String, Object> toMap(DatabaseUser userObject){
        Map<String, Object> user = new HashMap<>();
        user.put(Constants.FS_NAME, userObject.getUser_name());
        user.put(Constants.FS_STATUS, userObject.getUser_status());
        user.put(Constants.FS_NUMBER, userObject.getUser_number());
        user.put(Constants.FS_IMAGE, userObject.getUser_image());
        user.put(Constants.FS_BLOCKED, userObject.getBlocked());
        user.put(Constants.FS_SMALL_IMAGE, userObject.getUser_image());
        user.put(Constants.FS_RECENT_MSG, userObject.getUser_recent_message());
        user.put(Constants.FS_MSG_TIME_STAMP, userObject.getMsg_time_stamp());
        user.put(Constants.FS_TIME_STAMP, userObject.getUser_time_stamp());
        user.put(Constants.FS_ID,userObject.getUser_id());
        return user;
    }

    @Override
    public boolean equals(Object otherUserObject) {
        if (!(otherUserObject instanceof DatabaseUser)) {
            return false;
        }
        DatabaseUser that = (DatabaseUser) otherUserObject;
        if (this.user_recent_message != null && that.user_recent_message != null){
            return this.user_name.equals(that.user_name)
                    && this.user_status.equals(that.user_status)
                    && this.user_id.equals(that.user_id)
                    && this.user_number.equals(that.user_number)
                    && this.user_image.equals(that.user_image)
                    && this.user_small_image.equals(that.user_small_image)
                    && this.user_recent_message.equals(that.user_recent_message)
                    && this.unread == that.unread
                    && this.user_time_stamp.equals(that.user_time_stamp)
                    && this.msg_time_stamp.equals(that.msg_time_stamp)
                    ;
        }
        else{
            return this.user_name.equals(that.user_name)
                    && this.user_status.equals(that.user_status)
                    && this.user_id.equals(that.user_id)
                    && this.user_number.equals(that.user_number)
                    && this.user_image.equals(that.user_image)
                    && this.user_small_image.equals(that.user_small_image)
                    && this.unread == that.unread
                    && this.user_time_stamp.equals(that.user_time_stamp)
                    ;
        }

    }

    private DatabaseUser(Parcel in){
        this.user_name = in.readString();
        this.user_status =  in.readString();
        this.user_id = in.readString();
        this.user_number = in.readString();
        this.user_image = in.readString();
        this.user_small_image = in.readString();
        long tmpDate = in.readLong();
        this.user_time_stamp = tmpDate == -1 ? null : new Date(tmpDate);
        this.user_recent_message = in.readString();
        long tmpMsgDate = in.readLong();
        this.msg_time_stamp = tmpMsgDate == -1 ? null : new Date(tmpMsgDate);
        this.blocked = in.readInt() != 0;
        this.unread = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.user_name);
            dest.writeString(this.user_status);
            dest.writeString(this.user_id);
            dest.writeString(this.user_number);
            dest.writeString(this.user_image);
            dest.writeString(this.user_small_image);
            dest.writeLong(this.user_time_stamp != null ? this.user_time_stamp.getTime() : -1);
            dest.writeString(this.user_recent_message);
            dest.writeLong(this.msg_time_stamp != null ? this.msg_time_stamp.getTime() : -1);
            if (this.blocked == null){
                this.blocked = false;
            }
            dest.writeInt(this.blocked ? 1 : 0);
            dest.writeInt(this.unread);
    }

    public static class DatabaseUserList {
        private List<DatabaseUser> databaseUserList;
        public List<DatabaseUser> getUserObjectList() {
            return databaseUserList;
        }
    }
}