package com.sealstudios.aimessage.Database;

import android.arch.persistence.room.Room;
import android.content.Context;

import com.sealstudios.aimessage.Utils.Constants;


public class LiveDatabaseBuilder {

    private static LiveDbOpenHelper userDbHelper;
    private static final Object LOCK = new Object();

    public synchronized static LiveDbOpenHelper getUserDatabase(Context context) {
        if (userDbHelper == null) {
            synchronized (LOCK) {
                if (userDbHelper == null) {
                    userDbHelper = Room.databaseBuilder(context,
                            LiveDbOpenHelper.class, Constants.USERS).build();
                }
            }
        }
        return userDbHelper;
    }

}
