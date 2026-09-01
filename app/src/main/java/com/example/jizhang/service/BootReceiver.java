package com.example.jizhang.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * 开机广播接收器：重启后重新注册保活闹钟（因为 AlarmManager 闹钟在关机后会失效）
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            scheduleKeepAlive(context);
        }
    }

    private void scheduleKeepAlive(Context context) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(context, KeepAliveReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(context, 2001, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 30L * 60 * 1000,
                    30L * 60 * 1000, pi);
        } catch (Exception ignored) {
        }
    }
}
