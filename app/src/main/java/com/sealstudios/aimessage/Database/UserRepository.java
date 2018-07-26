package com.sealstudios.aimessage.Database;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sealstudios.aimessage.Utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

/**
 * Created by marti on 29/06/2018.
 */

public class UserRepository {

    private final LiveDatabaseUserDao databaseUserDao;
    private LiveData<DatabaseUser> contact;
    private LiveData<List<DatabaseUser>> userList;
    private static FirebaseFirestore firedb;
    private static FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private static final Object LOCK = new Object();
    private MaterialDialog materialDialog;
    private MaterialDialog.Builder builder;
    private String materialDialogBuilderContent;
    private ContentResolver contentResolver;
    private String userId;
    private static String userNumber;
    private LiveData<List<DatabaseUser>> data;
    private LiveData<DatabaseUser> userData;
    private ListenerRegistration listenerRegistration;
    CollectionReference dbReference;

    public UserRepository(Application application) {
        SharedPreferences pref = application.getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE);
        userId = pref.getString(Constants.FS_ID,"id");
        userNumber = pref.getString(Constants.FS_NUMBER,"number");
        LiveDbOpenHelper db = LiveDatabaseBuilder.getUserDatabase(application);
        databaseUserDao = db.userDaoLive();
        userList = databaseUserDao.getAll();
        contentResolver = application.getContentResolver();
        firedb = getFirebaseInstance();
        firebaseStorage = getFirebaseStorageInstance();
        storageReference = firebaseStorage.getReferenceFromUrl(Constants.STORAGE_REF);
        builder = new MaterialDialog.Builder(application);
        dbReference = FirebaseFirestore.getInstance().collection(Constants.USERS);
    }

    public UserRepository(Context context) {
        SharedPreferences pref = context.getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE);
        userId = pref.getString(Constants.FS_ID,"id");
        userNumber = pref.getString(Constants.FS_NUMBER,"number");
        LiveDbOpenHelper db = LiveDatabaseBuilder.getUserDatabase(context);
        databaseUserDao = db.userDaoLive();
    }

    public synchronized static FirebaseFirestore getFirebaseInstance() {
        if (firedb == null) {
            synchronized (LOCK) {
                if (firedb == null) {
                    firedb = FirebaseFirestore.getInstance();
                }
            }
        }
        return firedb;
    }

    public synchronized static FirebaseStorage getFirebaseStorageInstance() {
        if (firebaseStorage == null) {
            synchronized (LOCK) {
                if (firebaseStorage == null) {
                    firebaseStorage = FirebaseStorage.getInstance();
                }
            }
        }
        return firebaseStorage;
    }

    public LiveData<List<DatabaseUser>> getMyUser() {
        //maybe add a call to firebase to get all our contacts here
        //this will mean it is pulled at initiation and on any change
        //compare contact from firestore with contact from Room and save if they dont match
        //as this is observable i shouldnt have to do anything else
        listenerRegistration = dbReference.document(userId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null){}
                if (documentSnapshot != null){
                    DatabaseUser user = documentSnapshot.toObject(DatabaseUser.class);
                    if (user != null){
                        DatabaseUser users = returnUserById(user.getUser_id());
                        if (users != null){
                            if (!users.equals(user)) {
                                updateUser(user , "");
                                Log.d("UserRepo" , "update user");
                            }
                        }else{
                            Log.d("UserRepo" , "Room user is null");
                        }
                    }
                }
            }
        });
        if(data == null){
            data = databaseUserDao.getAll();
        }
        return data;
        //return databaseContactsDao.getAll();
    }

    private void checkUserChangedFireStore(DatabaseUser user) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                FirebaseFirestore.getInstance().collection(Constants.USERS)
                        .document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                                DatabaseUser users = task.getResult().toObject(DatabaseUser.class);
                                assert users != null;
                                if (!users.equals(user)) {
                                    updateUser(user , "");
                                }
                        }
                    }
                });
            }
        }).start();
    }

    private void insertUserOnline(DatabaseUser databaseUser, String token) {
        new insertUserOnlineAsyncTask(token).execute(databaseUser);
    }

    private void updateUserOnline(DatabaseUser databaseUser , String token) {
        new updateUserOnlineAsyncTask(token).execute(databaseUser);
    }

    public LiveData<DatabaseUser> getById(String id) {
        return databaseUserDao.findById(id);
    }

    public LiveData<DatabaseUser> getUserById(String id) {

        FirebaseFirestore.getInstance().collection(Constants.USERS).document(id).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null){}
                if (documentSnapshot != null){
                    DatabaseUser user = documentSnapshot.toObject(DatabaseUser.class);
                    if (user != null){
                        checkUserChangedFireStore(user);
                    }
                }
            }
        });
        if(userData == null){
            userData = databaseUserDao.findById(id);
        }
        return userData;
    }

    public List<DatabaseUser> getUserByName(String name) throws ExecutionException, InterruptedException {
        //return databaseContactsDao.findByName(name);
        return new searchUserByName(databaseUserDao).execute(name).get();
    }

    public DatabaseUser returnUserById(String id) {
        try {
            return new returnUserById(databaseUserDao).execute(id).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteUser(DatabaseUser databaseUser) {
        new deleteAsyncTask(databaseUserDao).execute(databaseUser);
    }

    public void updateUser(DatabaseUser databaseUser, String token) {
        new updateAsyncTask(databaseUserDao , token).execute(databaseUser);
    }

    public void insertUser(DatabaseUser databaseUser, String token) {
        new insertAsyncTask(databaseUserDao, token).execute(databaseUser);
    }

    private static class updateUserOnlineAsyncTask extends AsyncTask<DatabaseUser, Void, Void> {
        private String token;


        updateUserOnlineAsyncTask(String token) {
            this.token = token;
        }

        @Override
        protected Void doInBackground(final DatabaseUser... params) {
            Map<String, Object> user = new HashMap<>();
            DatabaseUser userObject = params[0];
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
            if (!token.isEmpty()){
                user.put(Constants.FS_TOKEN,token);
            }
            FirebaseFirestore.getInstance().collection(Constants.USERS)
                    .document(userObject.getUser_id())
                    .update(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                }
            });
            return null;
        }
    }

    private static class insertUserOnlineAsyncTask extends AsyncTask<DatabaseUser, Void, Void> {
        private String token;

        insertUserOnlineAsyncTask(String token) {
            this.token = token;
        }

        @Override
        protected Void doInBackground(final DatabaseUser... params) {
            Map<String, Object> user = new HashMap<>();
            DatabaseUser userObject = params[0];
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
            if (token != null){
                user.put(Constants.FS_TOKEN,token);
            }
            FirebaseFirestore.getInstance().collection(Constants.USERS)
                    .document(userObject.getUser_id())
                    .set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                }
            });
            return null;
        }
    }

    private static class insertAsyncTask extends AsyncTask<DatabaseUser, Void, Void> {

        private LiveDatabaseUserDao mAsyncTaskDao;
        private String token;

        insertAsyncTask(LiveDatabaseUserDao dao, String token) {
            mAsyncTaskDao = dao;
            this.token = token;
        }

        @Override
        protected Void doInBackground(final DatabaseUser... params) {
            mAsyncTaskDao.insertUser(params[0]);
            new insertUserOnlineAsyncTask(token).execute(params[0]);
            return null;
        }
    }

    public static class updateAsyncTask extends AsyncTask<DatabaseUser, Void, Void> {

        private LiveDatabaseUserDao mAsyncTaskDao;
        private String token;

        updateAsyncTask(LiveDatabaseUserDao dao,String token) {
            mAsyncTaskDao = dao;
            this.token = token;
        }

        @Override
        protected Void doInBackground(final DatabaseUser... params) {
            mAsyncTaskDao.insertUser(params[0]);
            new updateUserOnlineAsyncTask(token).execute(params[0]);
            return null;
        }
    }

    private static class deleteAsyncTask extends AsyncTask<DatabaseUser, Void, Void> {

        private LiveDatabaseUserDao mAsyncTaskDao;

        deleteAsyncTask(LiveDatabaseUserDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final DatabaseUser... params) {
            mAsyncTaskDao.deleteUser(params[0]);
            return null;
        }
    }

    private static class searchUserByName extends AsyncTask<String, Void, ArrayList<DatabaseUser>> {

        private LiveDatabaseUserDao mDao;

        searchUserByName(LiveDatabaseUserDao dao){
            this.mDao = dao;
        }

        @Override
        protected ArrayList<DatabaseUser> doInBackground(String... params) {
            ArrayList<DatabaseUser> contactsArrayList = new ArrayList<>();
            mDao.findByName("%" + params[0] + "%");
            return contactsArrayList;
        }
    }

    public static class returnUserById extends AsyncTask<String, Void, DatabaseUser> {

        private LiveDatabaseUserDao mDao;

        returnUserById(LiveDatabaseUserDao dao){
            this.mDao = dao;
        }

        @Override
        protected DatabaseUser doInBackground(String... params) {
            return mDao.returnUserById(params[0]);
        }
    }

    public void removeListener(){
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
