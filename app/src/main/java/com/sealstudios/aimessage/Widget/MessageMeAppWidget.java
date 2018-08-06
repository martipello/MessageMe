package com.sealstudios.aimessage.Widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.sealstudios.aimessage.MessageListActivity;
import com.sealstudios.aimessage.R;
import com.sealstudios.aimessage.Utils.Constants;

/**
 * Implementation of App Widget functionality.
 */
public class MessageMeAppWidget extends AppWidgetProvider {
    private static final String TAG = "MsgMeWdgt";

    @Override
    public void onReceive(Context context, Intent intent) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (intent.getAction().equals(Constants.LAUNCH_ACTIVITY)) {
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);

            String recipientId =  intent.getStringExtra(Constants.FS_ID);
            String recipientName =  intent.getStringExtra(Constants.FS_NAME);

            Intent i = new Intent(context, MessageListActivity.class);
            Bundle b = new Bundle();
            b.putString(Constants.FS_ID, recipientId);
            b.putString(Constants.FS_NAME, recipientName);
            i.putExtras(b);
            Bundle bundle = intent.getExtras();
            String toastText = "position is " + bundle.getInt(Constants.LAUNCH_ACTIVITY);
            //Toast.makeText(context,toastText,Toast.LENGTH_SHORT).show();
            context.startActivity(i);
        }else if(intent.getAction().equals(Constants.DONT_LAUNCH_ACTIVITY)){
            //Toast.makeText(context,"",Toast.LENGTH_SHORT).show();
        }
        super.onReceive(context, intent);
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                       int appWidgetId) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFS,Context.MODE_PRIVATE);
        boolean userSignedIn = sharedPreferences.getBoolean(Constants.SIGNED_IN, false);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_list_view);
        if (userSignedIn){
            //move everything in here after i know its working
            Intent intent = new Intent(context, MessagingService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            //populates data
            remoteViews.setRemoteAdapter(appWidgetId,R.id.widget_list,intent);
            remoteViews.setEmptyView(R.id.widget_list,R.id.empty);

            Intent launchIntent = new Intent(context, MessageMeAppWidget.class);
            launchIntent.setAction(Constants.LAUNCH_ACTIVITY);
            launchIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent launchPendingIntent = PendingIntent.getBroadcast(context, 0, launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            remoteViews.setPendingIntentTemplate(R.id.widget_list, launchPendingIntent);
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }else{
            Toast.makeText(context,"Please sign in",Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
               Log.d(TAG,"on update");
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

