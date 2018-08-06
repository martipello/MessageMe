package com.sealstudios.aimessage.Database;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sealstudios.aimessage.Utils.Constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import static android.support.constraint.Constraints.TAG;


public class ContactRepository {

    private final LiveDatabaseContactsDao databaseContactsDao;
    private StorageReference storageReference;
    private MaterialDialog.Builder builder;
    private ContentResolver contentResolver;
    private String userId;
    private static String userNumber;
    private LiveData<List<DatabaseContacts>> data;
    private LiveData<DatabaseContacts> contact;
    private ListenerRegistration listenerRegistration;
    CollectionReference contactReference;


    public ContactRepository(Application application) {
        SharedPreferences pref = application.getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE);
        userId = pref.getString(Constants.FS_ID, "id");
        userNumber = pref.getString(Constants.FS_NUMBER, "number");
        LiveDbOpenHelper db = LiveDatabaseBuilder.getUserDatabase(application);
        databaseContactsDao = db.contactDaoLive();
        contentResolver = application.getContentResolver();
        storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(Constants.STORAGE_REF);
        builder = new MaterialDialog.Builder(application);
        contactReference = FirebaseFirestore.getInstance().collection(Constants.USERS).document(userId).collection(Constants.CONTACTS);
    }

    public ContactRepository(Context context) {
        SharedPreferences pref = context.getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE);
        userId = pref.getString(Constants.FS_ID, "id");
        LiveDbOpenHelper db = LiveDatabaseBuilder.getUserDatabase(context);
        databaseContactsDao = db.contactDaoLive();
        storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(Constants.STORAGE_REF);
        contactReference = FirebaseFirestore.getInstance().collection(Constants.USERS).document(userId).collection(Constants.CONTACTS);
    }

    public LiveData<List<DatabaseContacts>> getAllContacts() {
        //maybe add a call to firebase to get all our contacts here
        //this will mean it is pulled at initiation and on any change
        //compare contact from firestore with contact from Room and save if they dont match
        //as this is observable i shouldnt have to do anything else
        Query query = contactReference.orderBy("msg_time_stamp");
        listenerRegistration = query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }
                for (QueryDocumentSnapshot doc : value) {
                    DatabaseContacts contact = doc.toObject(DatabaseContacts.class);
                    DatabaseContacts databaseContacts = returnContactById(contact.getUser_id());
                    if (databaseContacts != null) {
                        if (!databaseContacts.equals(contact)) {
                            Log.d("CntctRepo", "update user");
                            updateContact(contact);
                            //insertContactOnline(contact);
                        } else {
                            //Log.d("contactchanged", "contact hasnt changed");
                        }
                    } else {
                        //Log.d("contactchanged", "contact null");
                    }

                }
            }
        });

        if (data == null) {
            data = databaseContactsDao.getAll();
        }
        return data;
        //return databaseContactsDao.getAll();
    }

    private void insertContactOnline(DatabaseContacts databaseUser) {
        new insertContactOnlineAsyncTask(userId).execute(databaseUser);
    }

    private void updateContactOnline(DatabaseContacts databaseUser) {
        new updateContactOnlineAsyncTask(userId).execute(databaseUser);
    }

    public LiveData<DatabaseContacts> getContactById(String id) {

        listenerRegistration = contactReference.document(id).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (documentSnapshot != null){
                    DatabaseContacts firestoreContact = documentSnapshot.toObject(DatabaseContacts.class);
                    if (firestoreContact != null){
                        DatabaseContacts databaseContacts = returnContactById(firestoreContact.getUser_id());
                        if (!databaseContacts.equals(firestoreContact)){
                            updateContact(firestoreContact);
                            Log.d("contactchanged", "updated contact");
                        } else {
                            Log.d("contactchanged", "contact hasnt changed");
                        }
                    } else {
                        Log.d("contactchanged", "firestoreContact null");
                    }
                } else {
                    Log.d("contactchanged", "documentSnapshot null");
                }

            }
        });

        if (contact == null) {
            contact = databaseContactsDao.findById(id);
        }
        return contact;
    }

    public DatabaseContacts returnContactById(String id) {
        try {
            return new returnContactById(databaseContactsDao).execute(id).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LiveData<List<DatabaseContacts>> getContactByName(String name) {
        return databaseContactsDao.findByName(name);
    }

    public List<DatabaseContacts> getAllWithUnread() {
        try {
            return new returnUnreadContact(databaseContactsDao).execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteContact(DatabaseContacts databaseUser) {
        new deleteAsyncTask(databaseContactsDao).execute(databaseUser);
    }

    public void updateContact(DatabaseContacts databaseUser) {
        new updateAsyncTask(databaseContactsDao).execute(databaseUser);
    }

    public void insertContact(DatabaseContacts databaseUser) {
        new insertAsyncTask(databaseContactsDao).execute(databaseUser);
    }

    private static class updateContactOnlineAsyncTask extends AsyncTask<DatabaseContacts, Void, Void> {
        private String userId;

        updateContactOnlineAsyncTask(String userId) {
            this.userId = userId;
        }

        @Override
        protected Void doInBackground(final DatabaseContacts... params) {
            Map<String, Object> user = new HashMap<>();
            DatabaseContacts userObject = params[0];
            user.put(Constants.FS_NAME, userObject.getUser_name());
            user.put(Constants.FS_STATUS, userObject.getUser_status());
            user.put(Constants.FS_IMAGE, userObject.getUser_image());
            user.put(Constants.FS_BLOCKED, userObject.getBlocked());
            user.put(Constants.FS_SMALL_IMAGE, userObject.getUser_image());
            user.put(Constants.FS_RECENT_MSG, userObject.getUser_recent_message());
            user.put(Constants.FS_MSG_TIME_STAMP, userObject.getMsg_time_stamp());
            user.put(Constants.FS_TIME_STAMP, userObject.getUser_time_stamp());
            FirebaseFirestore.getInstance().collection(Constants.USERS)
                    .document(userId)
                    .collection(Constants.CONTACTS)
                    .document(userObject.getUser_id())
                    .update(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                }
            });
            return null;
        }
    }

    private static class insertContactOnlineAsyncTask extends AsyncTask<DatabaseContacts, Void, Void> {
        private String userId;

        insertContactOnlineAsyncTask(String userId) {
            this.userId = userId;
        }

        @Override
        protected Void doInBackground(final DatabaseContacts... params) {
            Map<String, Object> user = new HashMap<>();
            DatabaseContacts userObject = params[0];
            user.put(Constants.FS_NAME, userObject.getUser_name());
            user.put(Constants.FS_STATUS, userObject.getUser_status());
            user.put(Constants.FS_IMAGE, userObject.getUser_image());
            user.put(Constants.FS_BLOCKED, userObject.getBlocked());
            user.put(Constants.FS_SMALL_IMAGE, userObject.getUser_image());
            user.put(Constants.FS_RECENT_MSG, userObject.getUser_recent_message());
            user.put(Constants.FS_MSG_TIME_STAMP, userObject.getMsg_time_stamp());
            user.put(Constants.FS_TIME_STAMP, userObject.getUser_time_stamp());
            FirebaseFirestore.getInstance().collection(Constants.USERS)
                    .document(userId)
                    .collection(Constants.CONTACTS)
                    .document(userObject.getUser_id())
                    .update(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                }
            });
            return null;
        }
    }

    private static class insertAsyncTask extends AsyncTask<DatabaseContacts, Void, Void> {

        private LiveDatabaseContactsDao mAsyncTaskDao;

        insertAsyncTask(LiveDatabaseContactsDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final DatabaseContacts... params) {
            mAsyncTaskDao.insertContact(params[0]);
            return null;
        }
    }

    private static class updateAsyncTask extends AsyncTask<DatabaseContacts, Void, Void> {

        private LiveDatabaseContactsDao mAsyncTaskDao;

        updateAsyncTask(LiveDatabaseContactsDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final DatabaseContacts... params) {
            mAsyncTaskDao.updateContact(params[0]);
            return null;
        }
    }

    private static class deleteAsyncTask extends AsyncTask<DatabaseContacts, Void, Void> {

        private LiveDatabaseContactsDao mAsyncTaskDao;

        deleteAsyncTask(LiveDatabaseContactsDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final DatabaseContacts... params) {
            mAsyncTaskDao.deleteUser(params[0]);
            return null;
        }
    }

    private static class searchContactByName extends AsyncTask<String, Void, LiveData<List<DatabaseContacts>>> {

        private LiveDatabaseContactsDao mDao;

        searchContactByName(LiveDatabaseContactsDao dao) {
            this.mDao = dao;
        }

        @Override
        protected LiveData<List<DatabaseContacts>> doInBackground(String... params) {
            return mDao.findByName("%" + params[0] + "%");
        }
    }

    private static class returnContactById extends AsyncTask<String, Void, DatabaseContacts> {

        private LiveDatabaseContactsDao mDao;

        returnContactById(LiveDatabaseContactsDao dao) {
            this.mDao = dao;
        }

        @Override
        protected DatabaseContacts doInBackground(String... params) {
            return mDao.returnById(params[0]);
        }
    }

    private static class returnUnreadContact extends AsyncTask<Void, Void, List<DatabaseContacts>> {

        private LiveDatabaseContactsDao mDao;

        returnUnreadContact(LiveDatabaseContactsDao dao) {
            this.mDao = dao;
        }

        @Override
        protected List<DatabaseContacts> doInBackground(Void... params) {
            return mDao.getAllWithUnread();
        }
    }

    public void removeListener() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

}
