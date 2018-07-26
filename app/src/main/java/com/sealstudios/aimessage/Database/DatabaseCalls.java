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
import java.util.List;

@Entity
public class DatabaseCalls implements Parcelable {

    public static final Creator CREATOR = new Creator() {
        public DatabaseCalls createFromParcel(Parcel parcel) {
            return new DatabaseCalls(parcel);
        }

        public DatabaseCalls[] newArray(int size) {
            return new DatabaseCalls[size];
        }
    };

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = Constants.CALL_ID)
    private String call_id;

    @ColumnInfo(name = Constants.CALL_CALLER_NAME)
    private String call_caller_name;

    @ColumnInfo(name = Constants.CALL_CALLER_ID)
    private String call_caller_id;

    @ColumnInfo(name = Constants.CALL_CALLED_NAME)
    private String call_called_name;

    @ColumnInfo(name = Constants.CALL_CALLED_ID)
    private String call_called_id;

    @ColumnInfo(name = Constants.CALL_STATUS)
    private String call_status;

    @ColumnInfo(name = Constants.CALL_TIME_STAMP)
    @TypeConverters({Converters.class})
    private Date call_time_stamp;

    public DatabaseCalls() {
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

    @NonNull
    public String getCall_id() {
        return call_id;
    }

    public void setCall_id(@NonNull String call_id) {
        this.call_id = call_id;
    }

    public String getCall_caller_name() {
        return call_caller_name;
    }

    public void setCall_caller_name(String call_caller_name) {
        this.call_caller_name = call_caller_name;
    }

    public String getCall_caller_id() {
        return call_caller_id;
    }

    public void setCall_caller_id(String call_caller_id) {
        this.call_caller_id = call_caller_id;
    }

    public String getCall_called_name() {
        return call_called_name;
    }

    public void setCall_called_name(String call_called_name) {
        this.call_called_name = call_called_name;
    }

    public String getCall_called_id() {
        return call_called_id;
    }

    public void setCall_called_id(String call_called_id) {
        this.call_called_id = call_called_id;
    }

    public String getCall_status() {
        return call_status;
    }

    public void setCall_status(String call_status) {
        this.call_status = call_status;
    }

    public Date getCall_time_stamp() {
        return call_time_stamp;
    }

    public void setCall_time_stamp(Date call_time_stamp) {
        this.call_time_stamp = call_time_stamp;
    }

    @Override
    public boolean equals(Object otherUserObject) {
        if (!(otherUserObject instanceof DatabaseCalls)) {
            return false;
        }
        DatabaseCalls that = (DatabaseCalls) otherUserObject;
        return this.call_id.equals(that.call_id)
                && this.call_caller_id.equals(that.call_caller_id)
                && this.call_called_id.equals(that.call_called_id)
                && this.call_caller_name.equals(that.call_caller_name)
                && this.call_called_name.equals(that.call_called_name)
                && this.call_status.equals(that.call_status)
                //&& this.call_time_stamp.getTime() == that.call_time_stamp.getTime()
                ;

    }

    private DatabaseCalls(Parcel in) {
        this.call_id = in.readString();
        this.call_caller_id = in.readString();
        this.call_called_id = in.readString();
        this.call_caller_name = in.readString();
        this.call_called_name = in.readString();
        this.call_status = in.readString();
        long tmpDate = in.readLong();
        this.call_time_stamp = tmpDate == -1 ? null : new Date(tmpDate);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.call_id);
        dest.writeString(this.call_caller_id);
        dest.writeString(this.call_called_id);
        dest.writeString(this.call_caller_name);
        dest.writeString(this.call_called_name);
        dest.writeString(this.call_status);
        dest.writeLong(this.call_time_stamp != null ? this.call_time_stamp.getTime() : -1);
    }

    public static class DatabaseCallsList {
        private List<DatabaseCalls> databaseCallsList;

        public List<DatabaseCalls> getUserCallsList() {
            return databaseCallsList;
        }
    }
}