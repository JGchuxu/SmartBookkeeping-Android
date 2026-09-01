package com.example.jizhang.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.NotificationListenerService;

/**
 * 保活广播接收器：定时唤醒，请求系统重新绑定通知监听服务（防被系统杀掉后失效）
 */
public class KeepAliveReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            // Android 11+ 支持主动请求重新绑定通知监听服务
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                NotificationListenerService.requestRebind(
                        new ComponentName(context, BillNotificationListener.class));
            }
        } catch (Exception ignored) {
        }
    }
}
