package com.sealstudios.aimessage.Widget;

import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViewsService;

import com.sealstudios.aimessage.Widget.WidgetAdapter;

public class MessagingService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WidgetAdapter(this.getApplicationContext(), intent);
    }
}
