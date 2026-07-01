package com.campusmate.pro.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.campusmate.pro.notification.NotificationHelper;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        NotificationHelper.createChannel(context);
        NotificationHelper.show(context, title == null ? "CampusMate Pro" : title, message == null ? "Ada aktivitas yang perlu dicek." : message, (int) System.currentTimeMillis());
    }
}
