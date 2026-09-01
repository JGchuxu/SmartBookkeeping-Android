package com.example.jizhang;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.jizhang.BaseActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jizhang.adapter.CategoryAdapter;
import com.example.jizhang.db.DatabaseHelper;
import com.example.jizhang.model.Category;
import com.example.jizhang.util.Palette;
import com.example.jizhang.util.ThemeManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * 收支分类管理
 */
public class CategoryManageActivity extends BaseActivity {

    private DatabaseHelper db;
    private CategoryAdapter adapter;
    private int type = Category.TYPE_EXPENSE;

    private TextView tvExpense, tvIncome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.get(this).getThemeResId());
        super.onCreate(savedInstanceState);
        ThemeManager.get(this).apply(this);
        setContentView(R.layout.activity_category_manage);
        db = new DatabaseHelper(this);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        tvExpense = findViewById(R.id.tv_type_expense);
        tvIncome = findViewById(R.id.tv_type_income);
        tvExpense.setOnClickListener(v -> switchType(Category.TYPE_EXPENSE));
        tvIncome.setOnClickListener(v -> switchType(Category.TYPE_INCOME));

        RecyclerView recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryAdapter();
        adapter.setListener(new CategoryAdapter.Listener() {
            @Override
            public void onEdit(Category c) {
                showEditDialog(c);
            }

            @Override
            public void onDelete(Category c) {
                confirmDelete(c);
            }
        });
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> showEditDialog(null));

        switchType(type);
    }

    private void switchType(int t) {
        type = t;
        tvExpense.setSelected(type == Category.TYPE_EXPENSE);
        tvIncome.setSelected(type == Category.TYPE_INCOME);
        tvExpense.setTextColor(type == Category.TYPE_EXPENSE ? 0xFFFF5A5A : 0xFF999999);
        tvIncome.setTextColor(type == Category.TYPE_INCOME ? 0xFF43C478 : 0xFF999999);
        adapter.setItems(db.queryCategories(type));
    }

    private void showEditDialog(Category category) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText input = new EditText(this);
        input.setHint("分类名称");
        input.setSingleLine(true);
        if (category != null) {
            input.setText(category.name);
            input.setSelection(category.name.length());
        }
        container.addView(input);

        // 颜色选择行
        TextView colorLabel = new TextView(this);
        colorLabel.setText("选择颜色");
        colorLabel.setTextColor(0xFF888888);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = pad;
        container.addView(colorLabel, lp);

        final int[] selectedColor = {category != null ? category.colorIndex : 0};

        LinearLayout colorRow = new LinearLayout(this);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        int dotSize = (int) (40 * getResources().getDisplayMetrics().density);
        int dotMargin = (int) (6 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < Palette.colorCount(); i++) {
            final int idx = i;
            View dot = new View(this);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(Palette.colorOf(i));
            if (i == selectedColor[0]) {
                bg.setStroke((int) (3 * getResources().getDisplayMetrics().density), 0xFF333333);
            }
            dot.setBackground(bg);
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dotSize, dotSize);
            dotLp.setMargins(dotMargin, dotMargin, dotMargin, dotMargin);
            dot.setOnClickListener(v -> {
                selectedColor[0] = idx;
                for (int j = 0; j < colorRow.getChildCount(); j++) {
                    View child = colorRow.getChildAt(j);
                    GradientDrawable d = new GradientDrawable();
                    d.setShape(GradientDrawable.OVAL);
                    d.setColor(Palette.colorOf(j));
                    if (j == selectedColor[0]) {
                        d.setStroke((int) (3 * getResources().getDisplayMetrics().density), 0xFF333333);
                    }
                    child.setBackground(d);
                }
            });
            colorRow.addView(dot, dotLp);
        }
        container.addView(colorRow);

        new AlertDialog.Builder(this)
                .setTitle(category == null ? "添加分类" : "编辑分类")
                .setView(container)
                .setPositiveButton("确定", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "名称不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (category == null) {
                        db.insertCategory(name, type, selectedColor[0]);
                    } else {
                        db.updateCategory(category.id, name, selectedColor[0]);
                    }
                    switchType(type);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmDelete(Category c) {
        new AlertDialog.Builder(this)
                .setTitle("删除分类")
                .setMessage("确定删除「" + c.name + "」吗？该分类下的账单将显示为未知。")
                .setPositiveButton("删除", (dialog, which) -> {
                    db.deleteCategory(c.id);
                    switchType(type);
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
