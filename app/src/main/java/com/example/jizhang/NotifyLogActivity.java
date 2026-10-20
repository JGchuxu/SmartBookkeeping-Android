package com.example.jizhang;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.jizhang.BaseActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jizhang.adapter.NotifyLogAdapter;
import com.example.jizhang.db.DatabaseHelper;
import com.example.jizhang.model.NotifyLog;
import com.example.jizhang.util.ThemeManager;

import java.util.List;

/**
 * 通知识别日志（诊断用）
 */
public class NotifyLogActivity extends BaseActivity {

    private DatabaseHelper db;
    private NotifyLogAdapter adapter;
    private TextView tvEmpty;
    private TextView tvListenerStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.get(this).getThemeResId());
        super.onCreate(savedInstanceState);
        ThemeManager.get(this).apply(this);
        setContentView(R.layout.activity_notify_log);
        db = new DatabaseHelper(this);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        tvListenerStatus = findViewById(R.id.tv_listener_status);
        tvListenerStatus.setOnClickListener(v -> startActivity(new android.content.Intent(
                android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        tvEmpty = findViewById(R.id.tv_empty);
        findViewById(R.id.btn_clear).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("清空日志")
                    .setMessage("确定清空全部识别日志吗？")
                    .setPositiveButton("清空", (d, w) -> {
                        db.clearNotifyLog();
                        reload();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        RecyclerView recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotifyLogAdapter();
        recyclerView.setAdapter(adapter);

        reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        refreshListenerStatus();
        List<NotifyLog> list = db.queryNotifyLog(100);
        adapter.setItems(list);
        tvEmpty.setVisibility(list.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    /** 主动检测通知使用权：未开启显示红色引导，已开启则强制系统重连监听服务 */
    private void refreshListenerStatus() {
        String pkg = getPackageName();
        String listeners = android.provider.Settings.Secure.getString(
                getContentResolver(), "enabled_notification_listeners");
        boolean enabled = listeners != null && listeners.contains(pkg);
        if (enabled) {
            tvListenerStatus.setText("✅ 通知使用权已开启（若支付后仍无记录，请把本 App 锁定后台并允许自启动）");
            tvListenerStatus.setBackgroundColor(0xFFE8F5E9);
            tvListenerStatus.setTextColor(0xFF2E7D32);
            // MIUI 常在覆盖安装/杀后台后断开绑定：强制系统重连监听服务
            try {
                // requestRebind 为系统隐藏 API，反射调用；失败则静默（开关权限时系统会自动重绑）
                android.app.NotificationManager nm =
                        (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                java.lang.reflect.Method m = nm.getClass().getMethod("requestRebind",
                        android.content.ComponentName.class);
                m.invoke(nm, new android.content.ComponentName(
                        this, com.example.jizhang.service.BillNotificationListener.class));
            } catch (Exception ignored) {
            }
        } else {
            tvListenerStatus.setText("❌ 通知使用权未开启 —— 点此一键开启（自动记账的必要前提）");
            tvListenerStatus.setBackgroundColor(0xFFFFEBEE);
            tvListenerStatus.setTextColor(0xFFC62828);
        }
    }
}
