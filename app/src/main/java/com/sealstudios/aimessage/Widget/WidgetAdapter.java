package com.sealstudios.aimessage.Widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.FutureTarget;
import com.sealstudios.aimessage.Database.ContactRepository;
import com.sealstudios.aimessage.Database.DatabaseContacts;
import com.sealstudios.aimessage.R;
import com.sealstudios.aimessage.Utils.Constants;
import com.sealstudios.aimessage.Utils.GlideApp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

public class WidgetAdapter implements RemoteViewsService.RemoteViewsFactory {


    Context context;
    ArrayList<DatabaseContacts>  databaseContactsArrayList;
    String TAG = "wdgtAdptr";
    private int widgetId;

    public WidgetAdapter(Context context,Intent intent) {
        this.context = context;
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {
        //make the list
        Log.d(TAG,"widget onCreate");
        ContactRepository contactRepository = new ContactRepository(context);
        databaseContactsArrayList = new ArrayList<>(contactRepository.getAllWithUnread());
    }

    @Override
    public void onDataSetChanged() {
        Log.d(TAG,"widget onDataSetChanged");
        databaseContactsArrayList.clear();
        ContactRepository contactRepository = new ContactRepository(context);
        databaseContactsArrayList.addAll(contactRepository.getAllWithUnread());
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context,MessageMeAppWidget.class));
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.layout.widget_list_view);

    }

    @Override
    public void onDestroy() {
        databaseContactsArrayList.clear();
    }

    @Override
    public int getCount() {
        if (databaseContactsArrayList == null) return 0;
        return databaseContactsArrayList.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        DatabaseContacts databaseContacts = databaseContactsArrayList.get(position);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_contact_holder);

        Bundle extras = new Bundle();
        extras.putInt(Constants.LAUNCH_ACTIVITY, position);
        extras.putString(Constants.FS_ID,databaseContacts.getUser_id());
        extras.putString(Constants.FS_NAME,databaseContacts.getUser_name());
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        remoteViews.setOnClickFillInIntent(R.id.holder, fillInIntent);

        remoteViews.setTextViewText(R.id.contact_name,databaseContacts.getUser_name());
        if (databaseContacts.getUser_recent_message() != null){
            remoteViews.setTextViewText(R.id.message_time,getTimeDate(databaseContacts.getMsg_time_stamp().getTime()));
            remoteViews.setTextViewText(R.id.contact_status,databaseContacts.getUser_recent_message());
        }else{
            remoteViews.setTextViewText(R.id.message_time,getTimeDate(databaseContacts.getUser_time_stamp().getTime()));
            remoteViews.setTextViewText(R.id.contact_status,databaseContacts.getUser_status());
        }
        if (databaseContacts.getUnread() > 0){
            remoteViews.setViewVisibility(R.id.unread_holder, View.VISIBLE);
            remoteViews.setTextViewText(R.id.unread_count_text,String.valueOf(databaseContacts.getUnread()));
        }else{
            remoteViews.setViewVisibility(R.id.unread_holder, View.GONE);
        }
        remoteViews.setImageViewResource(R.id.contact_image,R.drawable.contact_placeholder_small);
        int height = 50;
        int width = 50;

        RequestBuilder<Bitmap> builder =
                GlideApp.with(context)
                        .asBitmap()
                        .load(databaseContacts.getUser_image())
                        .override(width, height)
                        .centerCrop();
        FutureTarget futureTarget = builder.into(width, height);
        try {
            remoteViews.setImageViewBitmap(R.id.contact_image, (Bitmap) futureTarget.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return remoteViews;
    }

    private String getTimeDate(long msgTimeMillis) {

        Calendar messageTime = Calendar.getInstance();
        messageTime.setTimeInMillis(msgTimeMillis);
        // get Currunt time
        Calendar now = Calendar.getInstance();

        final String strTimeFormate = "h:mm aa";
        final String strDateFormate = "dd/MM/yyyy h:mm aa";

        if (now.get(Calendar.DATE) == messageTime.get(Calendar.DATE)
                &&
                ((now.get(Calendar.MONTH) == messageTime.get(Calendar.MONTH)))
                &&
                ((now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)))
                ) {

            return DateFormat.format(strTimeFormate, messageTime).toString();

        } else if (
                ((now.get(Calendar.DATE) - messageTime.get(Calendar.DATE)) == 1)
                        &&
                        ((now.get(Calendar.MONTH) == messageTime.get(Calendar.MONTH)))
                        &&
                        ((now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)))
                ) {
            return "yesterday at " + DateFormat.format(strTimeFormate, messageTime);
        } else {
            return DateFormat.format(strDateFormate, messageTime).toString();
        }
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }



}

