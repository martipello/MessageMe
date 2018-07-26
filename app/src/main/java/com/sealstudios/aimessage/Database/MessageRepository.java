package com.sealstudios.aimessage.Database;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.sealstudios.aimessage.Utils.Constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import static android.support.constraint.Constraints.TAG;

/**
 * Created by marti on 29/06/2018.
 */

public class MessageRepository {

    private final LiveDatabaseMessagesDao databaseMessagesDao;
    private final LiveDatabaseContactsDao databaseContactsDao;
    private final LiveDatabaseUserDao databaseUserDao;
    private LiveData<DatabaseMessage> contact;
    private LiveData<List<DatabaseMessage>> messageList;
    private static FirebaseFirestore firedb;
    private static FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private StorageReference messageStorageRef;
    private static final Object LOCK = new Object();
    private MaterialDialog materialDialog;
    private MaterialDialog.Builder builder;
    private String materialDialogBuilderContent;
    private String userId;
    private String recipientId;
    private static String userNumber;
    private LiveData<List<DatabaseMessage>> data;
    private LiveData<DatabaseMessage> userData;
    private ListenerRegistration listenerRegistration;
    private CollectionReference messageReference;

    public MessageRepository(Application application,String id) {
        SharedPreferences pref = application.getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE);
        userId = pref.getString(Constants.FS_ID, "id");
        userNumber = pref.getString(Constants.FS_NUMBER, "number");
        recipientId = id;
        LiveDbOpenHelper db = LiveDatabaseBuilder.getUserDatabase(application);
        databaseMessagesDao = db.messagesDaoLive();
        databaseContactsDao = db.contactDaoLive();
        databaseUserDao = db.userDaoLive();
        messageList = databaseMessagesDao.getAll(userId,recipientId);
        firedb = getFirebaseInstance();
        firebaseStorage = getFirebaseStorageInstance();
        storageReference = firebaseStorage.getReferenceFromUrl(Constants.STORAGE_REF);
        builder = new MaterialDialog.Builder(application);
        messageReference = firedb.collection(Constants.USERS).document(userId)
                .collection(Constants.CONTACTS);

    }

    public MessageRepository(Context context, String rId) {
        SharedPreferences pref = context.getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE);
        userId = pref.getString(Constants.FS_ID, "id");
        userNumber = pref.getString(Constants.FS_NUMBER, "number");
        LiveDbOpenHelper db = LiveDatabaseBuilder.getUserDatabase(context);
        databaseMessagesDao = db.messagesDaoLive();
        databaseContactsDao = db.contactDaoLive();
        databaseUserDao = db.userDaoLive();
        //messageList = databaseMessagesDao.getAll(userId,recipientId);
        firedb = getFirebaseInstance();
        firebaseStorage = getFirebaseStorageInstance();
        storageReference = firebaseStorage.getReferenceFromUrl(Constants.STORAGE_REF);
        builder = new MaterialDialog.Builder(context);
        messageReference = firedb.collection(Constants.USERS).document(userId)
                .collection(Constants.CONTACTS);

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

    public LiveData<List<DatabaseMessage>> getAllMessages(String sId, String rId) {
        Log.d("MsgRpo", "sId " + sId + " rId " + rId );
        Query query = messageReference.document(rId).collection(Constants.MESSAGES).orderBy(Constants.MSG_TIME_STAMP);
        listenerRegistration = query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }
                for (QueryDocumentSnapshot doc : value) {
                    //get object from firestore
                    DatabaseMessage message = doc.toObject(DatabaseMessage.class);
                    //this is triggered by message list activity starting or
                    // by message list activity being open and firestore or room getting an update
                    //either by us sending a message or receiving a message
                    switch (message.getSent_received()){
                        case 0 :
                            if (message.getSenderId().equals(sId)){
                                //if we are the sender
                                //add message to room so user can see its being sent (room updates ui to white),
                                // firestore will update this to 1 almost immediately
                                Log.d("MsgRpo","case 0 inserting message from " + message.getSenderName());
                                insertMessage(message);
                            }
                            Log.d("MsgRpo", "case " + message.getSent_received() + " UN " + message.getSenderName() + " text " + message.getMessage());
                            break;
                        case 1 :
                            Log.d("MsgRpo","case 1 sender id " + message.getSenderId() + " sId is " + sId + " rId is " + rId);
                            if (message.getSenderId().equals(sId)){
                                //message is now successfully in firestore update room to update the ui to grey
                                Log.d("MsgRpo","case 1 inserting message from " + message.getSenderName());
                                insertMessage(message);
                            }else if (message.getSenderId().equals(rId)){

                                //if we are not the sender we are receiving a message
                                //update firestore to show the recipient it was received
                                //this will trigger this method to be called again putting us in case 2
                                Map<String, Object> messageMap = new HashMap<>();
                                messageMap.put(Constants.MSG_SENT_RECEIVED, 2);
                                messageReference.document(rId).collection(Constants.MESSAGES).document(message.getMessageId()).update(messageMap);
                                Log.d("MsgRpo", "set message sent_received 2 " + message.getMessageId());
                            }
                            Log.d("MsgRpo", "case " + message.getSent_received() + " UN " + message.getSenderName() + " text " + message.getMessage());
                            break;
                        case 2:
                            //this will trigger for every message that comes in
                            //this approach works for new messages and restoring but
                            //ideally this would only trigger if the message doesnt already exist
                            //maybe a Conflict strategy ignore would be better
                            Log.d("MsgRpo","case 2 inserting message from " + message.getSenderName());
                            insertMessage(message);
                            Log.d("MsgRpo", "case " + message.getSent_received() + " UN " + message.getSenderName() + " text " + message.getMessage());
                            break;
                    }
                    Log.d("MsgRpo","end of switch " + message.getSent_received());
                    //contact ref to update unread message count

                }
                CollectionReference contactRef = getFirebaseInstance().collection(Constants.USERS)
                        .document(sId).collection(Constants.CONTACTS);
                //contact map to change unread value
                Map<String, Object> contact = new HashMap<>();
                contact.put(Constants.FS_UNREAD, 0);
                contactRef.document(rId).update(contact);
            }
        });

        if (data == null) {
            data = databaseMessagesDao.getAll(sId, rId);
        }
        return data;
    }

    public void insertMessageOnline(DatabaseMessage databaseMessage) {
        new insertMessageOnlineAsyncTask(databaseMessagesDao).execute(databaseMessage);
    }

    private void updateMessageOnline(DatabaseMessage databaseUser, LiveDatabaseMessagesDao messagesDao) {
        new updateMessageOnlineAsyncTask().execute(databaseUser);
    }

    public void deleteMessage(DatabaseMessage databaseUser) {
        new deleteAsyncTask(databaseMessagesDao).execute(databaseUser);
    }

    public void updateMessage(DatabaseMessage databaseMessage) {
        new updateAsyncTask(databaseMessagesDao).execute(databaseMessage);
    }

    public void insertMessage(DatabaseMessage databaseMessage) {
        new insertAsyncTask(databaseMessagesDao).execute(databaseMessage);
    }

    public void insertDataMessageOnline(DatabaseMessage databaseMessage) {
        new insertDataMessageOnlineAsyncTask(databaseMessagesDao).execute(databaseMessage);
    }

    public LiveData<List<DatabaseMessage>> getAllMessagesById(String rId, String sId) {
        return databaseMessagesDao.getAll(rId, sId);
    }

    public LiveData<List<DatabaseMessage>> getAllMessagesByType(String id, String type) {
        return databaseMessagesDao.getAllByType(id, type);
    }

    public LiveData<List<DatabaseMessage>> getAllMessagesByName(String name) {
        return databaseMessagesDao.findByName(name);
    }

    public LiveData<List<DatabaseMessage>> getAllByDate(String recipientId,String date) {
        return databaseMessagesDao.findByDate(recipientId, date);
    }

    public LiveData<List<DatabaseMessage>> getAllByMessageText(String recipientId,String text) {
        return databaseMessagesDao.returnByText(recipientId, text);
    }

    public List<DatabaseMessage> getAllUnreadById(String senderId, String recipientId) {
        return databaseMessagesDao.returnUnread(recipientId, senderId);
    }

    public void markAllAsRead(String recipientId, String senderId) {
        List<DatabaseMessage> messages = returnUnreadMessages(recipientId, senderId);
        CollectionReference contactRef = getFirebaseInstance().collection(Constants.USERS)                      //contact ref to update unread message count
                .document(senderId).collection(Constants.CONTACTS);
        Map<String, Object> contact = new HashMap<>();                                  //contact map for unread value
        contact.put(Constants.FS_UNREAD, 0);
        contactRef.document(recipientId).update(contact);
        for (DatabaseMessage databaseMessage : messages) {
            new markAsReadAsyncTask(databaseMessagesDao).execute(databaseMessage);
        }
    }

    public List<DatabaseMessage> returnUnreadMessages(String recipientId, String userId) {
        String[] array = {recipientId, userId};
        try {
            return new returnUnreadMessages(databaseMessagesDao).execute(array).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class returnUnreadMessages extends AsyncTask<String[], Void, List<DatabaseMessage>> {

        private LiveDatabaseMessagesDao mDao;

        returnUnreadMessages(LiveDatabaseMessagesDao dao) {
            this.mDao = dao;
        }

        @Override
        protected List<DatabaseMessage> doInBackground(String[]... params) {
            return mDao.returnUnread(params[0][0], params[0][1]);
        }
    }

    private static class updateMessageOnlineAsyncTask extends AsyncTask<DatabaseMessage, Void, Void> {

        updateMessageOnlineAsyncTask() {
        }

        @Override
        protected Void doInBackground(final DatabaseMessage... params) {

            Map<String, Object> message = new HashMap<>();
            DatabaseMessage userMessage = params[0];

            CollectionReference messageSenderRef = getFirebaseInstance().collection(Constants.USERS)
                    .document(userMessage.getSenderId()).collection(Constants.CONTACTS)
                    .document(userMessage.getRecipientId()).collection(Constants.MESSAGES);

            CollectionReference messageRecipientRef = getFirebaseInstance().collection(Constants.USERS)
                    .document(userMessage.getRecipientId()).collection(Constants.CONTACTS)
                    .document(userMessage.getSenderId()).collection(Constants.MESSAGES);

            message.put(Constants.MSG_ID, userMessage.getMessageId());
            message.put(Constants.MSG_SENDER, userMessage.getSenderId());
            message.put(Constants.MSG_TEXT, userMessage.getMessage());
            message.put(Constants.MSG_TIME_STAMP, userMessage.getTime_stamp());
            message.put(Constants.MSG_DATA_TYPE, userMessage.getData_type());
            message.put(Constants.MSG_DATA_URL, userMessage.getData_url());
            message.put(Constants.MSG_SENDER_NAME, userMessage.getSenderName());
            message.put(Constants.MSG_RECIPIENT, userMessage.getRecipientId());
            message.put(Constants.MSG_SENT_RECEIVED, userMessage.getSent_received());
            messageSenderRef.document(userMessage.getMessageId()).update(message);
            messageRecipientRef.document(userMessage.getMessageId()).update(message);

            return null;
        }
    }

    public static class insertDataMessageOnlineAsyncTask extends AsyncTask<DatabaseMessage, Void, Void> {

        LiveDatabaseMessagesDao dao;

        public insertDataMessageOnlineAsyncTask(LiveDatabaseMessagesDao dao) {
            this.dao = dao;
        }

        @Override
        protected Void doInBackground(final DatabaseMessage... params) {
            DatabaseMessage userMessage = params[0];

            Map<String, Object> message = new HashMap<>();
            CollectionReference messageSenderRef = getFirebaseInstance().collection(Constants.USERS)
                    .document(userMessage.getSenderId()).collection(Constants.CONTACTS)
                    .document(userMessage.getRecipientId()).collection(Constants.MESSAGES);

            CollectionReference messageRecipientRef = getFirebaseInstance().collection(Constants.USERS)
                    .document(userMessage.getRecipientId()).collection(Constants.CONTACTS)
                    .document(userMessage.getSenderId()).collection(Constants.MESSAGES);

            message.put(Constants.MSG_ID, userMessage.getMessageId());
            message.put(Constants.MSG_SENDER, userMessage.getSenderId());
            message.put(Constants.MSG_TEXT, userMessage.getMessage());
            message.put(Constants.MSG_TIME_STAMP, userMessage.getTime_stamp());
            message.put(Constants.MSG_DATA_TYPE, userMessage.getData_type());
            message.put(Constants.MSG_DATA_URL, "");
            message.put(Constants.MSG_SENDER_NAME, userMessage.getSenderName());
            message.put(Constants.MSG_RECIPIENT, userMessage.getRecipientId());
            message.put(Constants.MSG_RECIPIENT_NAME, userMessage.getRecipientName());
            message.put(Constants.MSG_SENT_RECEIVED, userMessage.getSent_received());
            //set message to sender refs firestore database
            ///this will force an update tp room and show our message with no image
            messageSenderRef.document(userMessage.getMessageId()).set(message)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //when this is successful we upload our image

                            new updateSenderAsyncTask().execute(userMessage);
                            StorageReference storageReference = firebaseStorage.getReferenceFromUrl(Constants.STORAGE_REF);
                            StorageReference uploadRef = storageReference.child(userMessage.getSenderId())
                                    .child(userMessage.getRecipientId()).child(userMessage.getMessageId());
                            Uri imageUri = Uri.parse(userMessage.getData_url());
                            UploadTask uploadTask = uploadRef.putFile(imageUri);                                            //if message is a picture upload the image
                            uploadTask.addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                }
                            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    uploadRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            //message has been added on senders side and the image has been uploaded
                                            //update sent received to show grey
                                            //and add the image url
                                            if (userMessage.getSent_received() < 2){
                                                message.put(Constants.MSG_SENT_RECEIVED, 1);
                                                message.put(Constants.MSG_DATA_URL,uri.toString());
                                                messageSenderRef.document(userMessage.getMessageId()).update(message);
                                            }
                                            //we can now inform the recipient by sending the message with url to their
                                            //firestore database
                                            messageRecipientRef.document(userMessage.getMessageId()).set(message)
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {

                                                            if (userMessage.getSent_received() < 2){
                                                                message.put(Constants.MSG_SENT_RECEIVED, 1);
                                                                messageRecipientRef.document(userMessage.getMessageId()).update(message);
                                                            }
                                                            new updateRecipientAsyncTask().execute(userMessage);
                                                        }
                                                    });
                                        }
                                    });
                                }
                            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                    //TODO make the viewholder show a spinner while image uploading
                                    Log.d("MsgRepo", "progress " + taskSnapshot.getBytesTransferred());
                                }
                            });
                        }
                    });

            return null;
        }
    }

    public static class insertMessageOnlineAsyncTask extends AsyncTask<DatabaseMessage, Void, Void> {

        LiveDatabaseMessagesDao dao;

        public insertMessageOnlineAsyncTask(LiveDatabaseMessagesDao dao) {
            this.dao = dao;
        }

        @Override
        protected Void doInBackground(final DatabaseMessage... params) {

            Map<String, Object> message = new HashMap<>();
            DatabaseMessage userMessage = params[0];

            CollectionReference messageSenderRef = getFirebaseInstance().collection(Constants.USERS)
                    .document(userMessage.getSenderId()).collection(Constants.CONTACTS)
                    .document(userMessage.getRecipientId()).collection(Constants.MESSAGES);

            CollectionReference messageRecipientRef = getFirebaseInstance().collection(Constants.USERS)
                    .document(userMessage.getRecipientId()).collection(Constants.CONTACTS)
                    .document(userMessage.getSenderId()).collection(Constants.MESSAGES);

            message.put(Constants.MSG_ID, userMessage.getMessageId());
            message.put(Constants.MSG_SENDER, userMessage.getSenderId());
            message.put(Constants.MSG_TEXT, userMessage.getMessage());
            message.put(Constants.MSG_TIME_STAMP, userMessage.getTime_stamp());
            message.put(Constants.MSG_DATA_TYPE, userMessage.getData_type());
            message.put(Constants.MSG_DATA_URL, userMessage.getData_url());
            message.put(Constants.MSG_SENDER_NAME, userMessage.getSenderName());
            message.put(Constants.MSG_RECIPIENT, userMessage.getRecipientId());
            message.put(Constants.MSG_RECIPIENT_NAME, userMessage.getRecipientName());
            message.put(Constants.MSG_SENT_RECEIVED, userMessage.getSent_received());
            messageSenderRef.document(userMessage.getMessageId()).set(message)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            if (userMessage.getSent_received() < 2){
                                message.put(Constants.MSG_SENT_RECEIVED, 1);
                                messageSenderRef.document(userMessage.getMessageId()).update(message);
                            }
                            new updateSenderAsyncTask().execute(userMessage);
                        }
                    });

            messageRecipientRef.document(userMessage.getMessageId()).set(message)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            if (userMessage.getSent_received() < 2){
                                message.put(Constants.MSG_SENT_RECEIVED, 1);
                                messageRecipientRef.document(userMessage.getMessageId()).update(message);
                            }
                            new updateRecipientAsyncTask().execute(userMessage);
                        }
                    });

            if (userMessage.getData_type().equals(Constants.DATA_TYPE_IMAGE)) {
                new uploadMessageDataAsyncTask(dao, userMessage.getData_url()).execute(userMessage);
            }
            return null;
        }
    }

    private static class uploadMessageDataAsyncTask extends AsyncTask<DatabaseMessage, Void, Void> {
        private String uriString;
        private LiveDatabaseMessagesDao messagesDao;

        uploadMessageDataAsyncTask(LiveDatabaseMessagesDao messagesDao, String uriString) {
            this.uriString = uriString;
            this.messagesDao = messagesDao;
        }

        @Override
        protected Void doInBackground(final DatabaseMessage... params) {
            DatabaseMessage userMessage = params[0];
            DatabaseContacts databaseContact = new DatabaseContacts();
            databaseContact.setUser_id(userMessage.getRecipientId());

            StorageReference storageReference = firebaseStorage.getReferenceFromUrl(Constants.STORAGE_REF);
            StorageReference uploadRef = storageReference.child(userMessage.getSenderId())
                    .child(userMessage.getRecipientId()).child(userMessage.getMessageId());
            Uri imageUri = Uri.parse(uriString);
            UploadTask uploadTask = uploadRef.putFile(imageUri);                                            //if message is a picture upload the image
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    uploadRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            userMessage.setData_url(uri.toString());             //add image url to database message object
                            new updateAsyncTask(messagesDao).execute(userMessage);
                            new updateMessageOnlineAsyncTask().execute(userMessage);
                        }
                    });
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    //TODO make the viewholder show a spinner while image uploading
                    Log.d("MsgRepo", "progress " + taskSnapshot.getBytesTransferred());
                }
            });

            return null;
        }
    }

    private static class updateSenderAsyncTask extends AsyncTask<DatabaseMessage, Void, Void> {

        updateSenderAsyncTask() {
        }

        @Override
        protected Void doInBackground(final DatabaseMessage... params) {
            DatabaseMessage userMessage = params[0];

            Map<String, Object> user = new HashMap<>();
            user.put(Constants.FS_MSG_TIME_STAMP, userMessage.getTime_stamp());

            if (userMessage.getMessage().isEmpty()) {
                user.put(Constants.FS_RECENT_MSG, userMessage.getRecipientName() + " sent you a message");
            } else {
                user.put(Constants.FS_RECENT_MSG, userMessage.getMessage());
            }

            CollectionReference senderRef = getFirebaseInstance().collection(Constants.USERS)
                    .document(userMessage.getRecipientId()).collection(Constants.CONTACTS);
            senderRef.document(userMessage.getSenderId()).update(user);

            return null;
        }
    }

    private static class updateRecipientAsyncTask extends AsyncTask<DatabaseMessage, Void, Void> {

        updateRecipientAsyncTask() {
        }

        @Override
        protected Void doInBackground(final DatabaseMessage... params) {
            DatabaseMessage userMessage = params[0];

            Map<String, Object> user = new HashMap<>();
            user.put(Constants.FS_MSG_TIME_STAMP, userMessage.getTime_stamp());

            if (userMessage.getMessage().isEmpty()) {
                user.put(Constants.FS_RECENT_MSG, userMessage.getRecipientName() + " sent you a message");
            } else {
                user.put(Constants.FS_RECENT_MSG, userMessage.getMessage());
            }

            CollectionReference senderRef = getFirebaseInstance().collection(Constants.USERS)
                    .document(userMessage.getSenderId()).collection(Constants.CONTACTS);
            senderRef.document(userMessage.getRecipientId()).update(user);

            return null;
        }
    }

    private static class insertAsyncTask extends AsyncTask<DatabaseMessage, Void, Void> {

        private LiveDatabaseMessagesDao mAsyncTaskDao;

        insertAsyncTask(LiveDatabaseMessagesDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final DatabaseMessage... params) {
            mAsyncTaskDao.insertMessage(params[0]);
            Log.d("MsgRepo", "insertAsync " + params[0].getSent_received());
            return null;
        }
    }

    private static class updateAsyncTask extends AsyncTask<DatabaseMessage, Void, Void> {

        private LiveDatabaseMessagesDao mAsyncTaskDao;

        updateAsyncTask(LiveDatabaseMessagesDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final DatabaseMessage... params) {
            mAsyncTaskDao.updateMessage(params[0]);
            return null;
        }
    }

    private static class markAsReadAsyncTask extends AsyncTask<DatabaseMessage, Void, Void> {

        private LiveDatabaseMessagesDao mAsyncTaskDao;

        markAsReadAsyncTask(LiveDatabaseMessagesDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final DatabaseMessage... params) {
            params[0].setSent_received(2);
            mAsyncTaskDao.updateMessage(params[0]);
            return null;
        }
    }

    private static class deleteAsyncTask extends AsyncTask<DatabaseMessage, Void, Void> {

        private LiveDatabaseMessagesDao mAsyncTaskDao;

        deleteAsyncTask(LiveDatabaseMessagesDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final DatabaseMessage... params) {
            mAsyncTaskDao.deleteMessage(params[0]);
            return null;
        }
    }

    /*
    private static class searchMessageByText extends AsyncTask<String, Void, ArrayList<DatabaseMessage>> {

        private LiveDatabaseMessagesDao mDao;
        private String text;

        searchMessageByText(LiveDatabaseMessagesDao dao, String text) {
            this.mDao = dao;
            this.text = text;
        }

        @Override
        protected ArrayList<DatabaseMessage> doInBackground(String... params) {
            ArrayList<DatabaseMessage> msgArrayList = new ArrayList<>();
            mDao.getAllByType(params[0], text);
            return msgArrayList;
        }
    }

    private static class searchMessageByType extends AsyncTask<String, Void, ArrayList<DatabaseMessage>> {

        private LiveDatabaseMessagesDao mDao;
        private String type;

        searchMessageByType(LiveDatabaseMessagesDao dao, String type) {
            this.mDao = dao;
            this.type = type;
        }

        @Override
        protected ArrayList<DatabaseMessage> doInBackground(String... params) {
            ArrayList<DatabaseMessage> msgArrayList = new ArrayList<>();
            mDao.getAllByType(params[0], type);
            return msgArrayList;
        }
    }

    private static class searchMessageByName extends AsyncTask<String, Void, ArrayList<DatabaseMessage>> {

        private LiveDatabaseMessagesDao mDao;

        searchMessageByName(LiveDatabaseMessagesDao dao) {
            this.mDao = dao;
        }

        @Override
        protected ArrayList<DatabaseMessage> doInBackground(String... params) {
            ArrayList<DatabaseMessage> msgArrayList = new ArrayList<>();
            mDao.findByName("%" + params[0] + "%");
            return msgArrayList;
        }
    }

    private static class searchMessageById extends AsyncTask<String, Void, ArrayList<DatabaseMessage>> {

        private LiveDatabaseMessagesDao mDao;

        searchMessageById(LiveDatabaseMessagesDao dao) {
            this.mDao = dao;
        }

        @Override
        protected ArrayList<DatabaseMessage> doInBackground(String... params) {
            ArrayList<DatabaseMessage> msgArrayList = new ArrayList<>();
            mDao.findById("%" + params[0] + "%");
            return msgArrayList;
        }
    }

    private static class searchMessageByDate extends AsyncTask<String, Void, ArrayList<DatabaseMessage>> {

        private LiveDatabaseMessagesDao mDao;
        String date;

        searchMessageByDate(LiveDatabaseMessagesDao dao, String date) {
            this.mDao = dao;
            this.date = date;
        }

        @Override
        protected ArrayList<DatabaseMessage> doInBackground(String... params) {
            ArrayList<DatabaseMessage> msgArrayList = new ArrayList<>();
            mDao.findByDate(params[0],"%" + date + "%");
            return msgArrayList;
        }
    }
    */

    public DatabaseMessage returnMessageById(String recipientId, String userId, String messageId) {
        String[] array = {recipientId, userId, messageId};
        try {
            return new returnMessageById(databaseMessagesDao).execute(array).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class returnMessageById extends AsyncTask<String[], Void, DatabaseMessage> {

        private LiveDatabaseMessagesDao mDao;

        returnMessageById(LiveDatabaseMessagesDao dao) {
            this.mDao = dao;
        }

        @Override
        protected DatabaseMessage doInBackground(String[]... params) {
            return mDao.returnById(params[0][0], params[0][1], params[0][2]).getValue();
        }
    }

    public void removeListener() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
