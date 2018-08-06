package com.sealstudios.aimessage;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.sealstudios.aimessage.Database.DatabaseMessage;
import com.sealstudios.aimessage.Database.MessageRepository;
import com.sealstudios.aimessage.Utils.Constants;
import com.sealstudios.aimessage.Widget.MessageMeAppWidget;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final int NOTIFICATION_MAX_CHARACTERS = 30;
    private NotificationManager mManager;
    private static final String SINGLE_CHANNEL_ID = "com.sealstudios.aimessage.single";
    private static final String GROUP_CHANNEL_ID = "com.sealstudios.aimessage.group";
    public static final String NOTIFICATION_REPLY = "NotificationReply";
    public static final String NOTIFICATION_DISMISS = "NotificationDismiss";
    public static final String KEY_INTENT_REPLY = "keyintentreply";
    public static final int REQUEST_CODE_REPLY = 100;
    public static final String KEY_INTENT_DISMISS = "keyintentdismiss";
    public static final int REQUEST_CODE_DISMISS = 300;
    private static final int NOTIFICATION_ID = 200;

    private static final String SINGLE_CHANNEL_NAME = "SINGLE CHANNEL";
    private static final String GROUP_CHANNEL_NAME = "GROUP CHANNEL";
    private static final String SINGLE_CHANNEL_DESC = "Single channel notification";
    private static final String GROUP_CHANNEL_DESC = "Group channel notification";
    private boolean isActive = false;
    private String CURRENT_CONVO;
    //public static List<DatabaseMessage> messages;
    private List<DatabaseMessage> messages;
    private String MYTAG = "MsgSrvc";
    private MessageRepository messageRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);
        isActive = MessageListActivity.isActive;
        CURRENT_CONVO = sp.getString(Constants.ACTIVE_USER, "");
        Log.d(MYTAG, "current convo " + CURRENT_CONVO + " isActive : " + isActive);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if message contains a data payload.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannels();
        }
        Log.d(MYTAG, remoteMessage.getData().toString());
        Date date = new Date();
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            String dateString = data.get(Constants.MSG_TIME_STAMP);
            messageRepository = new MessageRepository(this,data.get(Constants.MSG_RECIPIENT));
            try {
                DateFormat df = new SimpleDateFormat("EEE MMM dd yyyy kk:mm:ss zzz", Locale.US);
                date = df.parse(dateString);
            } catch (ParseException pe) {
                pe.printStackTrace();
            }
            if (data.size() > 0) {
                if (data.get(Constants.MSG_DATA_TYPE).equals(Constants.DATA_TYPE_CALL) && !data.get(Constants.MSG_TEXT).equals(Constants.CALL_MISSED)){
                    //don't notify for any calls other than missed calls and only if we aren't already talking to the caller
                    Log.d(MYTAG, "Message is a call don't notify");
                }else{
                    if (isActive && CURRENT_CONVO.equals(data.get(Constants.MSG_SENDER))) {
                        Log.d(MYTAG, "Dont notify");
                        //if message list activity is open and we are talking to the sender of the message don't notify
                    } else {
                        //notify for everything else
                        Log.d(MYTAG, "notify");
                        DatabaseMessage message = new DatabaseMessage();
                        message.setMessage(data.get(Constants.MSG_TEXT));
                        message.setSenderName(data.get(Constants.MSG_SENDER_NAME));
                        message.setSenderId(data.get(Constants.MSG_SENDER));
                        message.setData_url(data.get(Constants.MSG_DATA_URL));
                        message.setData_type(data.get(Constants.MSG_DATA_TYPE));
                        message.setMessageId(data.get(Constants.MSG_ID));
                        message.setTime_stamp(date);
                        message.setRecipientName(data.get(Constants.MSG_RECIPIENT_NAME));
                        message.setRecipientId(data.get(Constants.MSG_RECIPIENT));
                        message.setSent_received(1);
                        messageRepository.insertMessage(message);
                        sendNotification(data, message);
                        Log.d(MYTAG, "message " + message.toString());
                    }
                }
            }
        }
        // Check if message contains a notification payload.
        //and do nothing if it does
        if (remoteMessage.getNotification() != null) {

        }
    }

    private NotificationManager getManager() {
        if (mManager == null) {
            mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mManager;
    }

    private void sendNotification(Map<String, String> data, DatabaseMessage message) {

        String profileImage = data.get(Constants.FS_IMAGE);
        String clickAction = data.get(Constants.CLICK_ACTION);
        String shortBody;
        Intent intent;
        PendingIntent pendingIntent;

        if (isActive) {
            Log.d(MYTAG, "intent mainactivity");
            intent = new Intent(this, MainActivity.class);
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            Log.d(MYTAG, "intent messageactivity with stack");
            intent = new Intent(this, MessageListActivity.class);
            Bundle b = new Bundle();
            b.putString(Constants.FS_NAME, message.getSenderName());
            b.putString(Constants.FS_ID, message.getSenderId());
            intent.putExtras(b);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addParentStack(MainActivity.class);
            stackBuilder.addNextIntent(intent);
            pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        if (message.getMessage().length() > NOTIFICATION_MAX_CHARACTERS) {
            shortBody = message.getMessage().substring(0, NOTIFICATION_MAX_CHARACTERS) + "\u2026";
        } else {
            shortBody = message.getMessage();
        }

        //int notificationId = createId(message.getTime_stamp().getTime());
        int notificationId = createIdFromId(message.getSenderId());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            NotificationCompat.Builder notificationBuilder = getNotificationDefault(message, shortBody, pendingIntent, profileImage);
            getManager().notify(notificationId, notificationBuilder.build());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            NotificationCompat.Builder notificationBuilder = getNotificationNougat(message, shortBody, pendingIntent, profileImage);
            getManager().notify(notificationId, notificationBuilder.build());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder notificationBuilder = getNotificationOreo(message, shortBody, SINGLE_CHANNEL_ID, pendingIntent, profileImage);
            getManager().notify(notificationId, notificationBuilder.build());
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.getApplicationContext());
        Intent widgetIntent = new Intent(this.getApplicationContext(), MessageMeAppWidget.class);
        widgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(this.getApplication(),MessageMeAppWidget.class));
        appWidgetManager.notifyAppWidgetViewDataChanged(ids,R.id.widget_list);
        //intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        //sendBroadcast(intent);
        for (int id : ids){
            MessageMeAppWidget.updateAppWidget(this.getApplicationContext(),appWidgetManager,id);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private NotificationCompat.Builder getNotificationOreo(DatabaseMessage message, String shortBody, String channelId, PendingIntent pendingIntent, String profileImage) {

        Intent intent;
        PendingIntent replyPendingIntent;
        Bitmap profileBitmap;
        messages = returnUnreadMessages(message.getRecipientId(), message.getSenderId());

        Bundle bundle = new Bundle();
        bundle.putString(Constants.MSG_SENDER_NAME, message.getSenderName());
        bundle.putString(Constants.MSG_SENDER, message.getSenderId());
        bundle.putString(Constants.MSG_RECIPIENT_NAME, message.getRecipientName());
        bundle.putString(Constants.MSG_RECIPIENT, message.getRecipientId());

        String replyLabel = getString(R.string.notif_action_reply);
        intent = new Intent(this, ReplyReceiver.class);
        intent.putExtras(bundle);
        replyPendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        RemoteInput remoteInput = new RemoteInput.Builder(NOTIFICATION_REPLY).setLabel(replyLabel).build();
        NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_send_white_24dp,
                this.getString(R.string.reply), replyPendingIntent)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build();

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        if (!profileImage.isEmpty()) {
            profileBitmap = getBitmapfromUrl(profileImage);
        } else {
            profileBitmap = BitmapFactory.decodeResource(this.getResources(),
                    R.drawable.message_me_notification_blue);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder
                .setContentTitle(String.format(getString(R.string.notification_message), message.getSenderName()))
                .setSmallIcon(R.drawable.message_me_notification_blue)
                .setLargeIcon(profileBitmap)
                .setContentText(shortBody)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setAutoCancel(true)
                .setChannelId(channelId)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .addAction(action);

        if (message.getData_type().equals(Constants.DATA_TYPE_IMAGE)) {
            Bitmap sentBitmap = getBitmapfromUrl(message.getData_url());
            notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle()
                    .setSummaryText(this.getString(R.string.new_picture))
                    .bigPicture(sentBitmap));
            return notificationBuilder;

        } else if (message.getData_type().equals(Constants.DATA_TYPE_TEXT)) {

            NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(message.getRecipientName());
            messagingStyle.setConversationTitle(getDateAndTime(message.getTime_stamp().getTime()));
            if (messages != null) {
                Log.d(MYTAG, "list size " + messages.size());
                for (DatabaseMessage databaseMessage : messages) {
                    NotificationCompat.MessagingStyle.Message notificationMessage = new NotificationCompat.MessagingStyle.Message(
                            databaseMessage.getMessage(), databaseMessage.getTime_stamp().getTime(), databaseMessage.getRecipientName()
                    );
                    messagingStyle.addMessage(notificationMessage);
                }
            } else {
                Log.d(MYTAG, "list is null");
                NotificationCompat.MessagingStyle.Message notificationMessage = new NotificationCompat.MessagingStyle.Message(
                        message.getMessage(), message.getTime_stamp().getTime(), message.getRecipientName()
                );
                messagingStyle.addMessage(notificationMessage);
            }
            notificationBuilder.setStyle(messagingStyle);
            return notificationBuilder;
        }
        return notificationBuilder;

    }

    private NotificationCompat.Builder getNotificationNougat(DatabaseMessage message, String shortBody, PendingIntent pendingIntent, String profileImage) {

        Intent intent;
        PendingIntent replyPendingIntent;
        Bitmap profileBitmap;
        messages = returnUnreadMessages(message.getRecipientId(), message.getSenderId());

        Bundle bundle = new Bundle();
        bundle.putString(Constants.MSG_SENDER_NAME, message.getSenderName());
        bundle.putString(Constants.MSG_SENDER, message.getSenderId());
        bundle.putString(Constants.MSG_RECIPIENT_NAME, message.getRecipientName());
        bundle.putString(Constants.MSG_RECIPIENT, message.getRecipientId());

        String replyLabel = getString(R.string.notif_action_reply);
        intent = new Intent(this, ReplyReceiver.class);
        intent.putExtras(bundle);
        replyPendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        RemoteInput remoteInput = new RemoteInput.Builder(NOTIFICATION_REPLY).setLabel(replyLabel).build();
        NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_send_white_24dp,
                this.getString(R.string.reply), replyPendingIntent)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build();

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        if (!profileImage.isEmpty()) {
            profileBitmap = getBitmapfromUrl(profileImage);
        } else {
            profileBitmap = BitmapFactory.decodeResource(this.getResources(),
                    R.drawable.message_me_notification_blue);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder
                .setContentTitle(String.format(getString(R.string.notification_message), message.getSenderName()))
                .setSmallIcon(R.drawable.message_me_notification_blue)
                .setLargeIcon(profileBitmap)
                .setContentText(shortBody)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .addAction(action);

        if (message.getData_type().equals(Constants.DATA_TYPE_IMAGE)) {
            Bitmap sentBitmap = getBitmapfromUrl(message.getData_url());
            notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle()
                    .setSummaryText(this.getString(R.string.new_picture))
                    .bigPicture(sentBitmap));
            return notificationBuilder;

        } else if (message.getData_type().equals(Constants.DATA_TYPE_TEXT)) {

            NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(message.getRecipientName());
            messagingStyle.setConversationTitle(getDateAndTime(message.getTime_stamp().getTime()));
            if (messages != null) {
                Log.d(MYTAG, "list size " + messages.size());
                for (DatabaseMessage databaseMessage : messages) {
                    NotificationCompat.MessagingStyle.Message notificationMessage = new NotificationCompat.MessagingStyle.Message(
                            databaseMessage.getMessage(), databaseMessage.getTime_stamp().getTime(), databaseMessage.getRecipientName()
                    );
                    messagingStyle.addMessage(notificationMessage);
                }
            } else {
                Log.d(MYTAG, "list is null");
                NotificationCompat.MessagingStyle.Message notificationMessage = new NotificationCompat.MessagingStyle.Message(
                        message.getMessage(), message.getTime_stamp().getTime(), message.getRecipientName()
                );
                messagingStyle.addMessage(notificationMessage);
            }
            notificationBuilder.setStyle(messagingStyle);
            return notificationBuilder;
        }
        return notificationBuilder;

    }

    private NotificationCompat.Builder getNotificationDefault(DatabaseMessage message, String shortBody, PendingIntent pendingIntent, String profileImage) {

        Intent intent;
        PendingIntent replyPendingIntent;
        Bitmap profileBitmap;

        if (isActive) {
            Log.d("FrbsMsgSrvc", "getNotificationDefault isActive = " + isActive);
            intent = new Intent(this, MainActivity.class);
            replyPendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            intent = new Intent(this, MessageListActivity.class);
            Bundle b = new Bundle();
            b.putString(Constants.FS_NAME, message.getSenderName());
            b.putString(Constants.FS_ID, message.getSenderId());
            intent.putExtras(b);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addParentStack(MainActivity.class);
            stackBuilder.addNextIntent(intent);
            replyPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            //replyPendingIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        }

        String replyLabel = getString(R.string.notif_action_reply);
        RemoteInput remoteInput = new RemoteInput.Builder(NOTIFICATION_REPLY).setLabel(replyLabel).build();
        NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_send_white_24dp,
                this.getString(R.string.reply), replyPendingIntent)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build();

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (!profileImage.isEmpty()) {
            profileBitmap = getBitmapfromUrl(profileImage);
        } else {
            profileBitmap = BitmapFactory.decodeResource(this.getResources(),
                    R.drawable.message_me_notification_wb);
        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder
                .setContentTitle(String.format(getString(R.string.notification_message), message.getSenderName()))
                .setSmallIcon(R.drawable.message_me_notification)
                .setLargeIcon(profileBitmap)
                .setContentText(shortBody)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .addAction(action);

        if (message.getData_type().equals(Constants.DATA_TYPE_IMAGE)) {
            Bitmap sentBitmap = getBitmapfromUrl(message.getData_url());
            notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle()
                    .setSummaryText(this.getString(R.string.new_picture) + " from " + message.getSenderName())
                    .bigPicture(sentBitmap));
            return notificationBuilder;

        } else if (message.getData_type().equals(Constants.DATA_TYPE_TEXT)) {
            notificationBuilder
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(message.getMessage())
                            .setSummaryText(this.getString(R.string.new_message)));
            return notificationBuilder;
        }
        return notificationBuilder;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannels() {
        // create single channel
        NotificationChannel singleChannel = new NotificationChannel(SINGLE_CHANNEL_ID,
                SINGLE_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        singleChannel.enableLights(true);
        singleChannel.enableVibration(true);
        singleChannel.setDescription(SINGLE_CHANNEL_DESC);
        singleChannel.setLightColor(R.color.colorPrimary);
        singleChannel.enableVibration(true);
        singleChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        getManager().createNotificationChannel(singleChannel);

        // create group channel
        NotificationChannel groupChannel = new NotificationChannel(GROUP_CHANNEL_ID,
                GROUP_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        groupChannel.enableLights(true);
        groupChannel.enableVibration(true);
        groupChannel.setDescription(GROUP_CHANNEL_DESC);
        groupChannel.setLightColor(R.color.colorPrimary);
        singleChannel.enableVibration(true);
        groupChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        getManager().createNotificationChannel(groupChannel);
    }

    private Bitmap getBitmapfromUrl(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getDateAndTime(long msgTimeMillis) {

        Calendar messageTime = Calendar.getInstance();
        messageTime.setTimeInMillis(msgTimeMillis);
        // get Current time
        Calendar now = Calendar.getInstance();

        final String strTimeFormate = "h:mm aa";
        final String strDateFormate = "dd/MM/yyyy";

        return android.text.format.DateFormat.format(strDateFormate, messageTime).toString();

    }

    public int createId(long time) {
        Calendar messageTime = Calendar.getInstance();
        messageTime.setTimeInMillis(time);
        int day = messageTime.get(Calendar.DATE);
        int month = messageTime.get(Calendar.MONTH);
        int year = messageTime.get(Calendar.YEAR);
        int hour = messageTime.get(Calendar.HOUR);
        int minutes = messageTime.get(Calendar.MINUTE);
        int seconds = messageTime.get(Calendar.SECOND);
        int milis = messageTime.get(Calendar.MILLISECOND);
        String notificationId = "" + minutes + seconds + milis;
        //String notificationId = "" + day + month + year;
        return Integer.parseInt(notificationId);
    }

    public static int createIdFromId(String id) {
        String digits = id.replaceAll("[^0-9]", "");
        if (digits.length() > 8) {
            String digitsInsideThreshold = digits.substring(0, 9);
            return Integer.parseInt(digitsInsideThreshold);
        }
        return Integer.parseInt(digits);
    }

    public List<DatabaseMessage> returnUnreadMessages(String recipientId, String userId) {
        return messageRepository.returnUnreadMessages(recipientId,userId);
    }


}
