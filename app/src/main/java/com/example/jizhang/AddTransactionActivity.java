package com.example.jizhang;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.jizhang.BaseActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jizhang.adapter.CategoryGridAdapter;
import com.example.jizhang.db.DatabaseHelper;
import com.example.jizhang.model.Category;
import com.example.jizhang.model.Channel;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.example.jizhang.model.Transaction;
import com.example.jizhang.util.NaturalLangParser;
import com.example.jizhang.util.Palette;
import com.example.jizhang.util.ThemeManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 记账录入 / 编辑界面
 */
public class AddTransactionActivity extends BaseActivity {

    public static final String EXTRA_ID = "extra_id";

    private DatabaseHelper db;
    private int type = Transaction.TYPE_EXPENSE;
    private long editingId = -1;

    private TextView tvExpense, tvIncome, tvDateTime;
    private EditText etAmount, etNote;
    private Spinner spinnerChannel;
    private RecyclerView gridCategory;
    private CategoryGridAdapter categoryAdapter;

    private final List<Channel> channels = new ArrayList<>();
    private final List<Category> categories = new ArrayList<>();
    private long selectedCategoryId = -1;
    private long selectedDateTime = System.currentTimeMillis();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.get(this).getThemeResId());
        super.onCreate(savedInstanceState);
        ThemeManager.get(this).apply(this);
        setContentView(R.layout.activity_add_transaction);
        db = new DatabaseHelper(this);

        editingId = getIntent().getLongExtra(EXTRA_ID, -1);

        tvExpense = findViewById(R.id.tv_type_expense);
        tvIncome = findViewById(R.id.tv_type_income);
        etAmount = findViewById(R.id.et_amount);
        etNote = findViewById(R.id.et_note);
        spinnerChannel = findViewById(R.id.spinner_channel);
        gridCategory = findViewById(R.id.grid_category);
        tvDateTime = findViewById(R.id.tv_date_time);

        categoryAdapter = new CategoryGridAdapter();
        categoryAdapter.setListener(new CategoryGridAdapter.OnCategoryClick() {
            @Override
            public void onClick(Category c) {
                selectedCategoryId = c.id;
            }

            @Override
            public void onAddClick() {
                showAddCategoryDialog();
            }
        });
        gridCategory.setLayoutManager(new GridLayoutManager(this, 4));
        gridCategory.setNestedScrollingEnabled(false);
        gridCategory.setAdapter(categoryAdapter);

        tvExpense.setOnClickListener(v -> switchType(Transaction.TYPE_EXPENSE));
        tvIncome.setOnClickListener(v -> switchType(Transaction.TYPE_INCOME));

        findViewById(R.id.btn_date_time).setOnClickListener(v -> pickDateTime());
        findViewById(R.id.btn_save).setOnClickListener(v -> save());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_nl).setOnClickListener(v -> showNaturalInput());

        loadChannels();
        switchType(type);

        if (editingId > 0) {
            fillForEdit();
        }
    }

    private void loadChannels() {
        channels.clear();
        channels.addAll(db.queryChannels());
        List<String> names = new ArrayList<>();
        for (Channel c : channels) names.add(c.name);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChannel.setAdapter(adapter);
    }

    private void switchType(int t) {
        type = t;
        tvExpense.setSelected(type == Transaction.TYPE_EXPENSE);
        tvIncome.setSelected(type == Transaction.TYPE_INCOME);
        tvExpense.setTextColor(type == Transaction.TYPE_EXPENSE ? 0xFFFF5A5A : 0xFF999999);
        tvIncome.setTextColor(type == Transaction.TYPE_INCOME ? 0xFF43C478 : 0xFF999999);

        categories.clear();
        categories.addAll(db.queryCategories(type));
        categoryAdapter.setItems(categories);
        selectedCategoryId = categories.isEmpty() ? -1 : categories.get(0).id;
        categoryAdapter.setSelectedId(selectedCategoryId);
    }

    private void pickDateTime() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(selectedDateTime);
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("选择日期")
                .setSelection(selectedDateTime)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar tmp = Calendar.getInstance();
            tmp.setTimeInMillis(selectedDateTime);
            tmp.setTimeInMillis(selection);
            TimePickerDialog tpd = new TimePickerDialog(this,
                    (v2, hour, minute) -> {
                        tmp.set(Calendar.HOUR_OF_DAY, hour);
                        tmp.set(Calendar.MINUTE, minute);
                        selectedDateTime = tmp.getTimeInMillis();
                        updateDateTimeLabel();
                    },
                    tmp.get(Calendar.HOUR_OF_DAY), tmp.get(Calendar.MINUTE), true);
            tpd.show();
        });
        picker.show(getSupportFragmentManager(), "date_picker");
    }

    private void updateDateTimeLabel() {
        tvDateTime.setText(Palette.formatDateTime(selectedDateTime));
    }

    private void showNaturalInput() {
        EditText input = new EditText(this);
        input.setHint("例如：昨天买咖啡35块，前天收到工资5000");
        input.setMinLines(2);
        new AlertDialog.Builder(this)
                .setTitle("快捷记账（自然语言）")
                .setView(input)
                .setPositiveButton("解析", (d, w) -> {
                    String text = input.getText().toString().trim();
                    NaturalLangParser.Result r = NaturalLangParser.parse(text);
                    if (!r.ok) {
                        Toast.makeText(this, "没识别出金额，请检查输入", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    etAmount.setText(Palette.formatAmount(r.amount));
                    switchType(r.type);
                    selectedDateTime = r.dateTime;
                    updateDateTimeLabel();
                    if (r.categoryHint != null) {
                        selectCategoryByName(r.categoryHint);
                    }
                    if (r.channelHint != null) {
                        selectChannelByName(r.channelHint);
                    }
                    // 备注：话里买了什么（不附加时间）
                    if (r.noteHint != null && !r.noteHint.isEmpty()) {
                        etNote.setText(r.noteHint);
                    }
                    Toast.makeText(this, "已解析：¥" + Palette.formatAmount(r.amount)
                            + "，" + (r.type == Transaction.TYPE_INCOME ? "收入" : "支出")
                            + "，" + Palette.formatDateTime(r.dateTime), Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void selectCategoryByName(String name) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).name.equals(name)) {
                selectedCategoryId = categories.get(i).id;
                categoryAdapter.setSelectedId(selectedCategoryId);
                return;
            }
        }
    }

    private void selectChannelByName(String name) {
        for (int i = 0; i < channels.size(); i++) {
            if (channels.get(i).name.equals(name)) {
                spinnerChannel.setSelection(i);
                return;
            }
        }
    }

    private void showAddCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint("分类名称，如：宠物");
        new AlertDialog.Builder(this)
                .setTitle(type == Transaction.TYPE_EXPENSE ? "添加支出分类" : "添加收入分类")
                .setView(input)
                .setPositiveButton("添加", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "请输入分类名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    long id = db.insertCategory(name, type, categories.size());
                    switchType(type);
                    selectedCategoryId = id;
                    categoryAdapter.setSelectedId(id);
                    Toast.makeText(this, "已添加分类「" + name + "」", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void fillForEdit() {
        Transaction t = db.getTransaction(editingId);
        if (t == null) return;
        type = t.type;
        etAmount.setText(Palette.formatAmount(t.amount));
        etNote.setText(t.note);
        selectedDateTime = t.dateTime;
        updateDateTimeLabel();

        // 选择渠道
        int channelPos = 0;
        for (int i = 0; i < channels.size(); i++) {
            if (channels.get(i).id == t.channelId) {
                channelPos = i;
                break;
            }
        }
        spinnerChannel.setSelection(channelPos);

        // 分类
        switchType(t.type);
        selectedCategoryId = t.categoryId;
        categoryAdapter.setSelectedId(selectedCategoryId);
    }

    private void save() {
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "请输入金额", Toast.LENGTH_SHORT).show();
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "金额格式不正确", Toast.LENGTH_SHORT).show();
            return;
        }
        if (amount <= 0) {
            Toast.makeText(this, "金额必须大于 0", Toast.LENGTH_SHORT).show();
            return;
        }
        if (spinnerChannel.getSelectedItemPosition() < 0 || channels.isEmpty()) {
            Toast.makeText(this, "请先添加支付渠道", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedCategoryId <= 0) {
            Toast.makeText(this, "请选择分类", Toast.LENGTH_SHORT).show();
            return;
        }

        Transaction t = new Transaction();
        t.id = editingId;
        t.type = type;
        t.amount = amount;
        t.channelId = channels.get(spinnerChannel.getSelectedItemPosition()).id;
        t.categoryId = selectedCategoryId;
        t.dateTime = selectedDateTime;
        t.note = etNote.getText().toString().trim();

        if (editingId > 0) {
            db.updateTransaction(t);
            Toast.makeText(this, "已更新", Toast.LENGTH_SHORT).show();
        } else {
            db.insertTransaction(t);
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
