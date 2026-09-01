package com.example.jizhang;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.jizhang.BaseActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jizhang.adapter.ChannelAdapter;
import com.example.jizhang.db.DatabaseHelper;
import com.example.jizhang.model.Channel;
import com.example.jizhang.util.ThemeManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * 支付渠道管理
 */
public class ChannelManageActivity extends BaseActivity {

    private DatabaseHelper db;
    private ChannelAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.get(this).getThemeResId());
        super.onCreate(savedInstanceState);
        ThemeManager.get(this).apply(this);
        setContentView(R.layout.activity_channel_manage);
        db = new DatabaseHelper(this);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChannelAdapter();
        adapter.setListener(new ChannelAdapter.Listener() {
            @Override
            public void onEdit(Channel c) {
                showEditDialog(c);
            }

            @Override
            public void onDelete(Channel c) {
                confirmDelete(c);
            }
        });
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> showEditDialog(null));

        reload();
    }

    private void reload() {
        adapter.setItems(db.queryChannels());
    }

    private void showEditDialog(Channel channel) {
        EditText input = new EditText(this);
        input.setHint("渠道名称，如：微信、支付宝、银行卡");
        input.setSingleLine(true);
        if (channel != null) {
            input.setText(channel.name);
            input.setSelection(channel.name.length());
        }
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle(channel == null ? "添加渠道" : "编辑渠道")
                .setView(input)
                .setPositiveButton("确定", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "名称不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (channel == null) {
                        db.insertChannel(name);
                    } else {
                        db.updateChannel(channel.id, name);
                    }
                    reload();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmDelete(Channel c) {
        new AlertDialog.Builder(this)
                .setTitle("删除渠道")
                .setMessage("确定删除「" + c.name + "」吗？该渠道下的账单将显示为未知。")
                .setPositiveButton("删除", (dialog, which) -> {
                    db.deleteChannel(c.id);
                    reload();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
