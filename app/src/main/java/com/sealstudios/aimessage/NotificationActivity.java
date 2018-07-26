package com.sealstudios.aimessage;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.RemoteInput;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sealstudios.aimessage.Database.DatabaseContacts;
import com.sealstudios.aimessage.Database.DatabaseMessage;
import com.sealstudios.aimessage.Database.DatabaseUser;
import com.sealstudios.aimessage.Database.LiveDatabaseBuilder;
import com.sealstudios.aimessage.Database.LiveDbOpenHelper;
import com.sealstudios.aimessage.Utils.Constants;
import com.sealstudios.aimessage.ViewModels.MessagesViewModel;

import java.util.Calendar;


public class NotificationActivity extends AppCompatActivity {

    private static final String SINGLE_CHANNEL_ID = "com.sealstudios.aimessage.single";
    public static final String GROUP_CHANNEL_ID = "com.sealstudios.aimessage.group";
    // Key for the string that's delivered in the action's intent.
    private static final String NOTIFICATION_REPLY = "NotificationReply";
    // mRequestCode allows you to update the notification.
    private static final int NOTIFICATION_ID = 200;
    private FirebaseFirestore db;
    private String stringRef = Constants.STORAGE_REF;
    private FirebaseStorage storage;
    private DatabaseContacts recipientUser;
    private DatabaseUser user;
    private StorageReference storageReference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReferenceFromUrl(stringRef);
        recipientUser = getContact(getIntent());
        user = getUser(getIntent());
        validateMessage(createMessage(getMessageText(getIntent()), Constants.DATA_TYPE_TEXT, user.getUser_id(), recipientUser.getUser_id(), recipientUser.getUser_name()));

    }

    private String getMessageText(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getString(NOTIFICATION_REPLY);
        }
        return null;
    }

    private DatabaseUser getUser(Intent intent) {
        if (intent != null) {
            String id = intent.getStringExtra(Constants.RECIPIENT_ID);
            Log.d("NtfctnActvty", id);
            LiveDbOpenHelper dbOpenHelper = LiveDatabaseBuilder.getUserDatabase(this);
            return dbOpenHelper.userDaoLive().findById(id).getValue();
        }
        return null;
    }

    private DatabaseContacts getContact(Intent intent) {
        if (intent != null) {
            String senderId = intent.getStringExtra(Constants.SENDER_ID);
            Log.d("NtfctnActvty", senderId);
            LiveDbOpenHelper dbOpenHelper = LiveDatabaseBuilder.getUserDatabase(this);
            return dbOpenHelper.contactDaoLive().findById(senderId).getValue();
        }
        return null;
    }

    private DatabaseMessage createMessage(String message_Text, String dataType, String senderId, String recipientId, String senderName) {
        Calendar cal = Calendar.getInstance();
        DatabaseMessage databaseMessage = new DatabaseMessage();
        databaseMessage.setMessageId(cal.getTime().toString());
        databaseMessage.setSenderId(senderId);
        databaseMessage.setMessage(message_Text);
        databaseMessage.setTime_stamp(cal.getTime());
        databaseMessage.setData_type(dataType);
        databaseMessage.setData_url("");
        databaseMessage.setRecipientId(recipientId);
        databaseMessage.setSenderName(senderName);
        databaseMessage.setSent_received(0);
        databaseMessage.setRecipientName(recipientUser.getUser_name());
        return databaseMessage;
    }

    private void validateMessage(DatabaseMessage userMessage) {
        //check if text is 0
        //check if there is an image or text etc
        //check if user is allowed to send this message to the recipient
        //check if user is in the recipient contact and if not add them
        MessagesViewModel messagesViewModel = ViewModelProviders.of(this).get(MessagesViewModel.class);
        DocumentReference messageSenderRef = db.collection(Constants.USERS)
                .document(userMessage.getRecipientId()).collection(Constants.CONTACTS).document(userMessage.getSenderId());
        messageSenderRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot document) {
                if (document != null) {
                    Boolean blocked = (Boolean) document.get(Constants.FS_BLOCKED);
                    System.out.println(document.getData());
                    if (blocked == null)
                        blocked = false;
                    if (blocked) {
                        Toast.makeText(NotificationActivity.this, R.string.message_not_sent, Toast.LENGTH_SHORT).show();
                    } else {
                        switch (userMessage.getData_type()) {
                            case Constants.DATA_TYPE_TEXT:
                                if (userMessage.getMessage().length() < 1) {
                                    Toast.makeText(NotificationActivity.this, R.string.message_not_sent, Toast.LENGTH_SHORT).show();
                                } else {
                                    messagesViewModel.insertMessage(userMessage);
                                }
                                break;
                            case Constants.DATA_TYPE_IMAGE:
                                //userMessage.setData_type(imageUri.toString());
                                break;
                        }
                    }
                }
            }
        });
    }

}
