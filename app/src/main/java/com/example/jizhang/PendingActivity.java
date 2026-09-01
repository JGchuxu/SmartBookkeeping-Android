package com.example.jizhang;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.jizhang.BaseActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jizhang.adapter.PendingAdapter;
import com.example.jizhang.db.DatabaseHelper;
import com.example.jizhang.model.Category;
import com.example.jizhang.model.PendingBill;
import com.example.jizhang.util.ThemeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 待确认账单列表（通知识别结果，需用户确认后入账）
 */
public class PendingActivity extends BaseActivity {

    private DatabaseHelper db;
    private PendingAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.get(this).getThemeResId());
        super.onCreate(savedInstanceState);
        ThemeManager.get(this).apply(this);
        setContentView(R.layout.activity_pending);
        db = new DatabaseHelper(this);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        tvEmpty = findViewById(R.id.tv_empty);

        RecyclerView recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PendingAdapter();
        adapter.setListener(new PendingAdapter.Listener() {
            @Override
            public void onConfirm(PendingBill p) {
                chooseCategory(p);
            }

            @Override
            public void onIgnore(PendingBill p) {
                db.deletePending(p.id);
                reload();
            }

            @Override
            public void onSelectionChanged(int count) {
            }
        });
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btn_batch).setOnClickListener(v -> toggleBatchMode());

        reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        List<PendingBill> list = db.queryPending();
        adapter.setItems(list);
        tvEmpty.setVisibility(list.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void toggleBatchMode() {
        if (adapter.isSelectionMode()) {
            int count = adapter.getSelectedIds().size();
            if (count > 0) {
                new AlertDialog.Builder(this)
                        .setTitle("批量删除")
                        .setMessage("确定删除选中的 " + count + " 条待确认账单吗？")
                        .setPositiveButton("删除", (dialog, which) -> {
                            for (Long id : adapter.getSelectedIds()) {
                                db.deletePending(id);
                            }
                            adapter.setSelectionMode(false);
                            reload();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            } else {
                adapter.setSelectionMode(false);
            }
        } else {
            adapter.setSelectionMode(true);
            Toast.makeText(this, "批量选择模式：点击账单勾选，再点右上角批量管理删除", Toast.LENGTH_SHORT).show();
        }
    }

    /** 确认入账前选择分类 */
    private void chooseCategory(PendingBill p) {
        List<Category> cats = db.queryCategories(p.type);
        if (cats.isEmpty()) {
            Toast.makeText(this, "请先在「设置-分类管理」添加分类", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> names = new ArrayList<>();
        for (Category c : cats) names.add(c.name);
        new AlertDialog.Builder(this)
                .setTitle("选择分类")
                .setItems(names.toArray(new String[0]), (dialog, which) -> {
                    long categoryId = cats.get(which).id;
                    db.confirmPending(p.id, categoryId);
                    Toast.makeText(this, "已入账", Toast.LENGTH_SHORT).show();
                    reload();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
