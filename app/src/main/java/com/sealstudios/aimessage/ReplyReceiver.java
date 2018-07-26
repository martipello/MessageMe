package com.sealstudios.aimessage;

import android.app.NotificationManager;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sealstudios.aimessage.Database.DatabaseMessage;
import com.sealstudios.aimessage.Database.LiveDatabaseBuilder;
import com.sealstudios.aimessage.Database.LiveDatabaseMessagesDao;
import com.sealstudios.aimessage.Database.LiveDbOpenHelper;
import com.sealstudios.aimessage.Database.MessageRepository;
import com.sealstudios.aimessage.Utils.Constants;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ReplyReceiver extends BroadcastReceiver {
    Context mContext;
    private LiveDbOpenHelper db;
    private LiveDatabaseMessagesDao databaseMessagesDao;
    private String TAG = "DrctRplyRcvr";
    String senderId;
    String senderName;
    String recipientId;
    String recipientName;
    private MessageRepository messageRepository;
    private NotificationManager mManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        DatabaseMessage message;
        Bundle remoteInput = null;
        Bundle intentExtras = intent.getExtras();
        if (intentExtras !=  null){
            recipientId = intentExtras.getString(Constants.MSG_SENDER);
            recipientName = intentExtras.getString(Constants.MSG_SENDER_NAME);
            senderId = intentExtras.getString(Constants.MSG_RECIPIENT);
            senderName = intentExtras.getString(Constants.MSG_RECIPIENT_NAME);
        }
        messageRepository = new MessageRepository(context,senderId);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            remoteInput = RemoteInput.getResultsFromIntent(intent);
        }
        Log.d(TAG, TAG);

        if (remoteInput != null) {
            CharSequence replyText = remoteInput.getCharSequence(MyFirebaseMessagingService.NOTIFICATION_REPLY);
            //Bundle b = remoteInput.getBundle(MyFirebaseMessagingService.NOTIFICATION_REPLY);
            Log.d(TAG, remoteInput.toString());
            if (replyText != null){
                validateMessage(createMessage(replyText.toString(),senderId,recipientId,senderName,recipientName));
                int notificationId = createIdFromId(recipientId);
                getManager(context).cancel(notificationId);
                messageRepository.markAllAsRead(senderId,recipientId);
                //message = createMessage(replyText.toString(),senderId,recipientId,senderName,recipientName);
                //MyFirebaseMessagingService.messages.add(message);
                //MyFirebaseMessagingService.sendNotification(context,message,"");
            }
        }
    }

    public static int createIdFromId(String id) {
        String digits = id.replaceAll("[^0-9]", "");
        if (digits.length() > 8) {
            String digitsInsideThreshold = digits.substring(0, 9);
            return Integer.parseInt(digitsInsideThreshold);
        }
        return Integer.parseInt(digits);
    }

    private NotificationManager getManager(Context context) {
        if (mManager == null) {
            mManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mManager;
    }

    private DatabaseMessage createMessage(String message_Text, String senderId, String recipientId, String senderName, String recipientName) {
        Calendar cal = Calendar.getInstance();
        DatabaseMessage databaseMessage = new DatabaseMessage();
        databaseMessage.setMessageId(cal.getTime().toString());
        databaseMessage.setSenderId(senderId);
        databaseMessage.setMessage(message_Text);
        databaseMessage.setTime_stamp(cal.getTime());
        databaseMessage.setData_type(Constants.DATA_TYPE_TEXT);
        databaseMessage.setData_url("");
        databaseMessage.setRecipientId(recipientId);
        databaseMessage.setSenderName(senderName);
        databaseMessage.setSent_received(0);
        databaseMessage.setRecipientName(recipientName);
        return databaseMessage;
    }

    private void validateMessage(DatabaseMessage userMessage) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference messageSenderRef = db.collection(Constants.USERS)
                .document(userMessage.getRecipientId()).collection(Constants.CONTACTS).document(userMessage.getSenderId());
        messageSenderRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot document) {
                if (document != null) {
                    Boolean blocked = (Boolean) document.get(Constants.FS_BLOCKED);
                    if (blocked == null)
                        blocked = false;
                    if (blocked) {
                        Toast.makeText(mContext, R.string.blocked_user, Toast.LENGTH_SHORT).show();
                    } else {
                        messageRepository.insertMessageOnline(userMessage);
                    }
                }
            }
        });
    }
}
