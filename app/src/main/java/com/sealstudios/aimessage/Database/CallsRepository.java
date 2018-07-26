package com.sealstudios.aimessage.Database;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.sealstudios.aimessage.Utils.Constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CallsRepository {

    private final LiveDatabaseCallsDao databaseCallsDao;
    private LiveData<List<DatabaseCalls>> callsList;
    private String userId;
    private LiveData<List<DatabaseCalls>> data;
    private ListenerRegistration listenerRegistration;
    CollectionReference callReference;


    public CallsRepository(Application application) {
        SharedPreferences pref = application.getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE);
        userId = pref.getString(Constants.FS_ID,"id");
        LiveDbOpenHelper db = LiveDatabaseBuilder.getUserDatabase(application);
        databaseCallsDao = db.callsDaoLive();
        callReference = FirebaseFirestore.getInstance().collection(Constants.USERS).document(userId).collection(Constants.CALLS);
    }

    public LiveData<List<DatabaseCalls>> getAllCalls() {
        Log.d("CallsRepo" , "getAllCalls");
        if(data == null){
            data = databaseCallsDao.getAllCalls();
        }
        return data;
    }

    private void insertCallOnline(DatabaseCalls databaseCall) {
        new insertCallOnlineAsyncTask(userId).execute(databaseCall);
    }

    private void updateCallOnline(DatabaseCalls databaseCall) {
        new updateCallOnlineAsyncTask(userId).execute(databaseCall);
    }

    public LiveData<DatabaseCalls> getCallById(String id) {
        return databaseCallsDao.findCallsById(id);
    }

    public LiveData<List<DatabaseCalls>> getCallsByName(String name) {
        return databaseCallsDao.findCallsByName(name);
    }

    public LiveData<List<CallObject>> getCallsWithImage() {
        return databaseCallsDao.loadCallsWithImages();
    }

    public void deleteCall(DatabaseCalls databaseCalls) {
        new deleteAsyncTask(databaseCallsDao).execute(databaseCalls);
    }

    public void updateCall(DatabaseCalls databaseCalls) {
        new updateAsyncTask(databaseCallsDao).execute(databaseCalls);
    }

    public void insertCall(DatabaseCalls databaseCalls) {
        new insertAsyncTask(databaseCallsDao).execute(databaseCalls);
    }

    private static class updateCallOnlineAsyncTask extends AsyncTask<DatabaseCalls, Void, Void> {
        private String userId;

        updateCallOnlineAsyncTask(String userId) {
            this.userId = userId;
        }

        @Override
        protected Void doInBackground(final DatabaseCalls... params) {
            Map<String, Object> callMap = new HashMap<>();
            DatabaseCalls call = params[0];
            callMap.put(Constants.CALL_ID, call.getCall_id());
            callMap.put(Constants.CALL_CALLER_ID, call.getCall_caller_id());
            callMap.put(Constants.CALL_CALLER_NAME, call.getCall_caller_name());
            callMap.put(Constants.CALL_CALLED_ID, call.getCall_called_id());
            callMap.put(Constants.CALL_CALLED_NAME, call.getCall_called_name());
            callMap.put(Constants.CALL_STATUS, call.getCall_status());
            callMap.put(Constants.CALL_TIME_STAMP, call.getCall_time_stamp());

            FirebaseFirestore.getInstance().collection(Constants.USERS)
                    .document(userId)
                    .collection(Constants.CALLS)
                    .document(call.getCall_id())
                    .update(callMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                }
            });
            return null;
        }
    }

    private static class insertCallOnlineAsyncTask extends AsyncTask<DatabaseCalls, Void, Void> {
        private String userId;

        insertCallOnlineAsyncTask(String userId) {
            this.userId = userId;
        }

        @Override
        protected Void doInBackground(final DatabaseCalls... params) {
            Map<String, Object> callMap = new HashMap<>();
            DatabaseCalls call = params[0];
            callMap.put(Constants.CALL_ID, call.getCall_id());
            callMap.put(Constants.CALL_CALLER_ID, call.getCall_caller_id());
            callMap.put(Constants.CALL_CALLER_NAME, call.getCall_caller_name());
            callMap.put(Constants.CALL_CALLED_ID, call.getCall_called_id());
            callMap.put(Constants.CALL_CALLED_NAME, call.getCall_called_name());
            callMap.put(Constants.CALL_STATUS, call.getCall_status());
            callMap.put(Constants.CALL_TIME_STAMP, call.getCall_time_stamp());
            FirebaseFirestore.getInstance().collection(Constants.USERS)
                    .document(userId)
                    .collection(Constants.CALLS)
                    .document(call.getCall_id())
                    .set(callMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                }
            });
            return null;
        }
    }

    private static class insertAsyncTask extends AsyncTask<DatabaseCalls, Void, Void> {

        private LiveDatabaseCallsDao mAsyncTaskDao;

        insertAsyncTask(LiveDatabaseCallsDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final DatabaseCalls... params) {
            mAsyncTaskDao.insertCall(params[0]);
            return null;
        }
    }

    private static class updateAsyncTask extends AsyncTask<DatabaseCalls, Void, Void> {

        private LiveDatabaseCallsDao mAsyncTaskDao;

        updateAsyncTask(LiveDatabaseCallsDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final DatabaseCalls... params) {
            mAsyncTaskDao.updateCall(params[0]);
            return null;
        }
    }

    private static class deleteAsyncTask extends AsyncTask<DatabaseCalls, Void, Void> {

        private LiveDatabaseCallsDao mAsyncTaskDao;

        deleteAsyncTask(LiveDatabaseCallsDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final DatabaseCalls... params) {
            mAsyncTaskDao.deleteCall(params[0]);
            return null;
        }
    }

    private static class searchCallByName extends AsyncTask<String, Void, LiveData<List<DatabaseCalls>>> {

        private LiveDatabaseCallsDao mDao;

        searchCallByName(LiveDatabaseCallsDao dao){
            this.mDao = dao;
        }

        @Override
        protected LiveData<List<DatabaseCalls>> doInBackground(String... params) {
            return mDao.findCallsByName("%" + params[0] + "%");
        }
    }

    public void removeListener(){
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

}
