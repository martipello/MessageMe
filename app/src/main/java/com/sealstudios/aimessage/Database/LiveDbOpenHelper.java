package com.sealstudios.aimessage.Database;


import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {DatabaseUser.class, DatabaseContacts.class,DatabaseMessage.class,DatabaseCalls.class}, version = 1)
public abstract class LiveDbOpenHelper extends RoomDatabase {
        public abstract LiveDatabaseUserDao userDaoLive();
        public abstract LiveDatabaseContactsDao contactDaoLive();
        public abstract LiveDatabaseMessagesDao messagesDaoLive();
        public abstract LiveDatabaseCallsDao callsDaoLive();
}


