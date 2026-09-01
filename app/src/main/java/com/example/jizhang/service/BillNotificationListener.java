package com.example.jizhang.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.example.jizhang.R;
import com.example.jizhang.db.DatabaseHelper;
import com.example.jizhang.model.Note;
import com.example.jizhang.util.BillParser;

/**
 * 通知监听服务：监听微信/支付宝的消费通知，解析后直接入账，并记录识别日志。
 * 通过前台服务（startForeground）+ 保活闹钟，尽量在后台常驻（类似微信/QQ）。
 */
public class BillNotificationListener extends NotificationListenerService {

    private static final String CHANNEL_ID = "bill_listener";
    private static final int FOREGROUND_ID = 1001;
    private DatabaseHelper db;

    @Override
    public void onCreate() {
        super.onCreate();
        db = new DatabaseHelper(this);
        startForegroundService();
        scheduleKeepAlive();
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        // 系统重新绑定服务时，再次确保前台服务已启动
        startForegroundService();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            stopForeground(true);
        } catch (Exception ignored) {
        }
    }

    /** 升级为前台服务，显示常驻通知，提高进程优先级（防被杀） */
    private void startForegroundService() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "记账识别", NotificationManager.IMPORTANCE_MIN);
                channel.setShowBadge(false);
                nm.createNotificationChannel(channel);
                Notification n = new Notification.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("记账识别已开启")
                        .setContentText("正在后台监听微信/支付宝消费通知，自动记账")
                        .setOngoing(true)
                        .build();
                startForeground(FOREGROUND_ID, n);
            }
        } catch (Exception ignored) {
        }
    }

    /** 每 30 分钟唤醒一次，请求系统重新绑定监听服务（保活） */
    private void scheduleKeepAlive() {
        try {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent intent = new Intent(this, KeepAliveReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(this, 2001, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 30L * 60 * 1000,
                    30L * 60 * 1000, pi);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;

        String pkg = sbn.getPackageName();
        if (!BillParser.isSupported(pkg)) return;

        Notification n = sbn.getNotification();
        String title = extractTitle(n);
        String text = extractText(n);

        String content = (title == null ? "" : title) + " " + (text == null ? "" : text);

        // 广告过滤：广告/营销通知直接忽略，不入账也不入待办
        if (BillParser.isAdvertisement(content)) {
            return;
        }

        BillParser.Result r = BillParser.parse(pkg, title, text);

        // 只记录真正解析成功（消费/退款）的通知，过滤营销、无关等非必要信息
        if (r != null) {
            long now = System.currentTimeMillis();
            // 去重：同一笔账单（同来源+同金额+5分钟内）只记录一次
            if (db.hasDuplicateTransaction(r.type, r.amount, r.source, now)) {
                return;
            }
            String note = (title == null ? "" : title) + " " + (text == null ? "" : text);
            if (r.refund) {
                note = "[退款] " + note;
            }
            db.insertNotifyLog(r.source, title, text, true, r.type, r.amount, now);
            // 直接入账，显示在账单列表（自动分配默认分类 + 渠道）
            com.example.jizhang.model.Transaction t = new com.example.jizhang.model.Transaction();
            t.type = r.type;
            t.amount = r.amount;
            t.channelId = db.findOrCreateChannel(r.source);
            t.categoryId = db.findOrCreateDefaultCategory(r.type);
            t.dateTime = now;
            t.note = note.trim();
            db.insertTransaction(t);
        } else {
            // 无法分辨收支金额 / 是否为收支信息，但疑似收支 → 加入待办，供用户手动处理
            if (BillParser.isSuspectedBill(content)) {
                addToTodo(pkg, content.trim());
            }
        }
    }

    /** 疑似收支但无法精确识别的通知加入待办（todo=1 待办未完成），5 分钟窗口去重 */
    private void addToTodo(String pkg, String content) {
        try {
            if (content.isEmpty()) return;

            long now = System.currentTimeMillis();
            // 去重：5 分钟内相同内容不重复加入
            if (db.hasDuplicateTodo(content, now)) {
                return;
            }

            String source = BillParser.getSource(pkg);
            Note note = new Note();
            note.title = (source == null ? "通知" : source) + " · 待识别";
            note.content = content;
            note.colorIndex = 0;
            note.todo = 1; // 待办未完成
            note.createTime = now;
            note.updateTime = now;
            db.insertNote(note);
        } catch (Exception ignored) {
        }
    }

    private String extractTitle(Notification n) {
        Bundle extras = n.extras;
        if (extras == null) return null;
        CharSequence c = extras.getCharSequence(Notification.EXTRA_TITLE);
        return c == null ? null : c.toString();
    }

    /** 提取通知文本：合并 TEXT / BIG_TEXT / SUB_TEXT / TEXT_LINES 等字段 */
    private String extractText(Notification n) {
        Bundle extras = n.extras;
        if (extras == null) return null;
        StringBuilder sb = new StringBuilder();
        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
        if (text != null) sb.append(text).append(' ');
        CharSequence big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (big != null) sb.append(big).append(' ');
        CharSequence sub = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
        if (sub != null) sb.append(sub).append(' ');
        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines != null) {
            for (CharSequence l : lines) {
                if (l != null) sb.append(l).append(' ');
            }
        }
        return sb.toString().trim();
    }
}
