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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.get(this).getThemeResId());
        super.onCreate(savedInstanceState);
        ThemeManager.get(this).apply(this);
        setContentView(R.layout.activity_notify_log);
        db = new DatabaseHelper(this);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
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
        List<NotifyLog> list = db.queryNotifyLog(100);
        adapter.setItems(list);
        tvEmpty.setVisibility(list.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }
}
