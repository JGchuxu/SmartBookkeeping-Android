package com.example.jizhang.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.jizhang.R;

/**
 * 待办闹钟接收器：到点发通知提醒
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "todo_reminder";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        if (title == null || title.isEmpty()) title = "待办提醒";

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "待办提醒", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("待办提醒")
                    .setContentText(title)
                    .setAutoCancel(true)
                    .build();
        } else {
            notification = new Notification.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("待办提醒")
                    .setContentText(title)
                    .setAutoCancel(true)
                    .build();
        }
        nm.notify((int) System.currentTimeMillis(), notification);
    }
}
