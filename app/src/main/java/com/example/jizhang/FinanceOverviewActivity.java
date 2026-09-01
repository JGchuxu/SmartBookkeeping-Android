package com.example.jizhang;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.jizhang.BaseActivity;

import com.example.jizhang.db.DatabaseHelper;
import com.example.jizhang.model.Channel;
import com.example.jizhang.model.StatItem;
import com.example.jizhang.model.Transaction;
import com.example.jizhang.util.Palette;
import com.example.jizhang.util.RuleAdvisor;
import com.example.jizhang.util.ThemeManager;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.Calendar;
import java.util.List;

/**
 * 财务总览（多账户）：每个账户独立初始金额、余额、当天/当月/当年收支，可自定义添加账户
 */
public class FinanceOverviewActivity extends BaseActivity {

    private static final String PREFS = "finance";
    private static final String KEY_INITIAL_PREFIX = "initial_";

    private DatabaseHelper db;
    private List<Channel> channels;
    private long selectedChannelId = -1;
    private long selectedHistoryDate = System.currentTimeMillis();

    private LinearLayout llTabs;
    private LinearLayout llDetails;
    private TextView tvBalance, tvInitial, tvDayNet, tvMonthNet, tvYearNet;
    private TextView tvHistoryDate, tvHistoryDayNet, tvHistoryBalance, tvAdvice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.get(this).getThemeResId());
        super.onCreate(savedInstanceState);
        ThemeManager.get(this).apply(this);
        setContentView(R.layout.activity_finance_overview);
        db = new DatabaseHelper(this);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        llTabs = findViewById(R.id.ll_tabs);
        llDetails = findViewById(R.id.ll_details);
        tvBalance = findViewById(R.id.tv_balance);
        tvInitial = findViewById(R.id.tv_initial);
        tvDayNet = findViewById(R.id.tv_day_net);
        tvMonthNet = findViewById(R.id.tv_month_net);
        tvYearNet = findViewById(R.id.tv_year_net);
        tvHistoryDate = findViewById(R.id.tv_history_date);
        tvHistoryDayNet = findViewById(R.id.tv_history_day_net);
        tvHistoryBalance = findViewById(R.id.tv_history_balance);
        tvAdvice = findViewById(R.id.tv_advice);

        findViewById(R.id.btn_set_initial).setOnClickListener(v -> showInitialDialog());
        findViewById(R.id.btn_history).setOnClickListener(v -> showDatePicker());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChannels();
        buildTabs();
        refresh();
    }

    private void loadChannels() {
        channels = db.queryChannels();
        if (selectedChannelId < 0 && !channels.isEmpty()) {
            selectedChannelId = channels.get(0).id;
        }
    }

    private Channel currentChannel() {
        if (channels == null) return null;
        for (Channel c : channels) {
            if (c.id == selectedChannelId) return c;
        }
        return channels.isEmpty() ? null : channels.get(0);
    }

    private void buildTabs() {
        llTabs.removeAllViews();
        for (Channel c : channels) {
            llTabs.addView(buildTab(c.name, c.id));
        }
        TextView add = new TextView(this);
        add.setText("+ 添加");
        add.setTextSize(13);
        add.setTextColor(0xFF4DB6AC);
        add.setPadding(dp(16), dp(8), dp(16), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor(0xFFE0F2F1);
        bg.setStroke(1, 0xFF4DB6AC);
        add.setBackground(bg);
        add.setOnClickListener(v -> showAddDialog());
        llTabs.addView(add);
    }

    private TextView buildTab(String name, long id) {
        TextView tab = new TextView(this);
        tab.setText(name);
        tab.setTextSize(13);
        tab.setPadding(dp(16), dp(8), dp(16), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        boolean selected = id == selectedChannelId;
        bg.setColor(selected ? 0xFF4DB6AC : 0xFFEEEEEE);
        tab.setBackground(bg);
        tab.setTextColor(selected ? 0xFF333333 : 0xFF555555);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(8);
        tab.setLayoutParams(lp);
        tab.setOnClickListener(v -> {
            selectedChannelId = id;
            buildTabs();
            refresh();
        });
        tab.setOnLongClickListener(v -> {
            confirmDelete(id, name);
            return true;
        });
        return tab;
    }

    private void showAddDialog() {
        EditText input = new EditText(this);
        input.setHint("账户名称，如：建行卡");
        new AlertDialog.Builder(this)
                .setTitle("添加账户")
                .setView(input)
                .setPositiveButton("添加", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "请输入账户名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    long id = db.insertChannel(name);
                    selectedChannelId = id;
                    loadChannels();
                    buildTabs();
                    refresh();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmDelete(long id, String name) {
        double income = db.sumByChannel(Transaction.TYPE_INCOME, id, 0, Long.MAX_VALUE);
        double expense = db.sumByChannel(Transaction.TYPE_EXPENSE, id, 0, Long.MAX_VALUE);
        String msg;
        if (income + expense > 0) {
            msg = "「" + name + "」下还有账单记录（收 ¥" + Palette.formatAmount(income)
                    + "，支 ¥" + Palette.formatAmount(expense) + "），删除后这些账单将失去归属。确定删除？";
        } else {
            msg = "确定删除账户「" + name + "」吗？";
        }
        new AlertDialog.Builder(this)
                .setTitle("删除账户")
                .setMessage(msg)
                .setPositiveButton("删除", (d, w) -> {
                    db.deleteChannel(id);
                    if (selectedChannelId == id) selectedChannelId = -1;
                    loadChannels();
                    buildTabs();
                    refresh();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showInitialDialog() {
        Channel ch = currentChannel();
        if (ch == null) return;
        double current = getInitial(ch.id);
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("例如 10000");
        input.setText(current > 0 ? formatAmountForInput(current) : "");
        new AlertDialog.Builder(this)
                .setTitle("设置「" + ch.name + "」初始金额")
                .setView(input)
                .setPositiveButton("确定", (d, w) -> {
                    try {
                        double v = Double.parseDouble(input.getText().toString().trim());
                        // 用 String 存储，避免 float 精度丢失（1.95 → 1.9500000476837158）
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                                .putString(KEY_INITIAL_PREFIX + ch.id, formatAmountForInput(v)).apply();
                        refresh();
                    } catch (Exception e) {
                        Toast.makeText(this, "请输入有效金额", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 保留 2 位小数的格式化（避免 1.95 被显示成 1.9500000476837158） */
    private String formatAmountForInput(double v) {
        if (v == (long) v) return String.valueOf((long) v);
        return String.format(java.util.Locale.US, "%.2f", v);
    }

    private double getInitial(long channelId) {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String key = KEY_INITIAL_PREFIX + channelId;
        // 优先读 String（新数据），兼容旧 float 数据
        // 注意：同一 key 旧数据是 Float，getString 会抛 ClassCastException（而非返回默认值）
        try {
            String s = sp.getString(key, null);
            if (s != null && !s.isEmpty()) {
                try {
                    return Double.parseDouble(s);
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (ClassCastException ignored) {
            // 旧数据是 Float 类型，走下面 getFloat 分支
        }
        return sp.getFloat(key, 0f);
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("选择日期")
                .setSelection(selectedHistoryDate)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            selectedHistoryDate = startOfDay(selection);
            refresh();
        });
        picker.show(getSupportFragmentManager(), "date_picker");
    }

    private void refresh() {
        Channel ch = currentChannel();
        if (ch == null) {
            tvBalance.setText("¥0.00");
            tvInitial.setText("请先添加账户");
            return;
        }
        long now = System.currentTimeMillis();
        long cid = ch.id;

        double initial = getInitial(cid);
        double totalIncome = db.sumByChannel(Transaction.TYPE_INCOME, cid, 0, now);
        double totalExpense = db.sumByChannel(Transaction.TYPE_EXPENSE, cid, 0, now);
        double balance = initial + totalIncome - totalExpense;

        long dayStart = startOfDay(now);
        long monthStart = startOfMonth(now);
        long yearStart = startOfYear(now);

        double dayIncome = db.sumByChannel(Transaction.TYPE_INCOME, cid, dayStart, now);
        double dayExpense = db.sumByChannel(Transaction.TYPE_EXPENSE, cid, dayStart, now);
        double monthIncome = db.sumByChannel(Transaction.TYPE_INCOME, cid, monthStart, now);
        double monthExpense = db.sumByChannel(Transaction.TYPE_EXPENSE, cid, monthStart, now);
        double yearIncome = db.sumByChannel(Transaction.TYPE_INCOME, cid, yearStart, now);
        double yearExpense = db.sumByChannel(Transaction.TYPE_EXPENSE, cid, yearStart, now);

        tvInitial.setText("「" + ch.name + "」初始金额 ¥" + Palette.formatAmount(initial));
        tvBalance.setText("¥" + Palette.formatAmount(balance));
        tvBalance.setTextColor(balance >= 0 ? Palette.COLOR_INCOME : Palette.COLOR_EXPENSE);

        setNet(tvDayNet, dayIncome - dayExpense);
        setNet(tvMonthNet, monthIncome - monthExpense);
        setNet(tvYearNet, yearIncome - yearExpense);

        refreshHistory(cid);
        refreshDetails(cid);

        List<StatItem> catStats = db.sumByCategory(Transaction.TYPE_EXPENSE, monthStart, now);
        double topExpense = catStats.isEmpty() ? 0 : catStats.get(0).value;
        String topName = catStats.isEmpty() ? "" : catStats.get(0).name;
        List<String> tips = RuleAdvisor.advise(initial, totalIncome, totalExpense,
                monthIncome, monthExpense, topExpense, topName);
        StringBuilder sb = new StringBuilder();
        for (String t : tips) sb.append("· ").append(t).append("\n");
        tvAdvice.setText(sb.toString().trim());
    }

    private void refreshHistory(long channelId) {
        long dayStart = startOfDay(selectedHistoryDate);
        long dayEnd = dayStart + 24L * 3600 * 1000 - 1;

        double dayIncome = db.sumByChannel(Transaction.TYPE_INCOME, channelId, dayStart, dayEnd);
        double dayExpense = db.sumByChannel(Transaction.TYPE_EXPENSE, channelId, dayStart, dayEnd);
        double totalIncome = db.sumByChannel(Transaction.TYPE_INCOME, channelId, 0, dayEnd);
        double totalExpense = db.sumByChannel(Transaction.TYPE_EXPENSE, channelId, 0, dayEnd);
        double balance = getInitial(channelId) + totalIncome - totalExpense;

        tvHistoryDate.setText(Palette.formatDate(selectedHistoryDate));
        setNet(tvHistoryDayNet, dayIncome - dayExpense);
        tvHistoryBalance.setText("¥" + Palette.formatAmount(balance));
    }

    /** 展示该账户最近的账单明细（金额 + 具体时间） */
    private void refreshDetails(long channelId) {
        llDetails.removeAllViews();
        List<Transaction> list = db.queryTransactionsByChannel(channelId, 0, Long.MAX_VALUE);
        if (list.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("该账户暂无账单记录");
            empty.setTextColor(0xFF999999);
            empty.setTextSize(13);
            llDetails.addView(empty);
            return;
        }
        int max = Math.min(list.size(), 20);
        for (int i = 0; i < max; i++) {
            llDetails.addView(buildDetailRow(list.get(i)));
        }
    }

    private LinearLayout buildDetailRow(Transaction t) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        row.setPadding(0, pad, 0, pad);

        TextView tvTime = new TextView(this);
        tvTime.setText(Palette.formatDateTime(t.dateTime));
        tvTime.setTextSize(12);
        tvTime.setTextColor(0xFF888888);
        row.addView(tvTime);

        TextView tvName = new TextView(this);
        String catName = "未知";
        if (t.categoryId > 0) {
            com.example.jizhang.model.Category cat = db.getCategory(t.categoryId);
            if (cat != null) catName = cat.name;
        }
        tvName.setText(catName);
        tvName.setTextSize(13);
        tvName.setTextColor(0xFF333333);
        LinearLayout.LayoutParams nameLp =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        nameLp.leftMargin = dp(12);
        row.addView(tvName, nameLp);

        TextView tvAmount = new TextView(this);
        String sign = t.type == Transaction.TYPE_INCOME ? "+" : "-";
        tvAmount.setText(sign + "¥" + Palette.formatAmount(t.amount));
        tvAmount.setTextSize(14);
        tvAmount.setTextColor(t.type == Transaction.TYPE_INCOME ? Palette.COLOR_INCOME : Palette.COLOR_EXPENSE);
        row.addView(tvAmount);

        return row;
    }

    private void setNet(TextView tv, double net) {
        tv.setText((net >= 0 ? "+" : "") + "¥" + Palette.formatAmount(net));
        tv.setTextColor(net >= 0 ? Palette.COLOR_INCOME : Palette.COLOR_EXPENSE);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private long startOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long startOfMonth(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long startOfYear(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.MONTH, Calendar.JANUARY);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
