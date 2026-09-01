package com.example.jizhang.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jizhang.AddTransactionActivity;
import com.example.jizhang.R;
import com.example.jizhang.adapter.BillAdapter;
import com.example.jizhang.db.DatabaseHelper;
import com.example.jizhang.model.Category;
import com.example.jizhang.model.Channel;
import com.example.jizhang.model.Transaction;
import com.example.jizhang.util.Exporter;
import com.example.jizhang.util.Palette;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 账单页：本月收支汇总 + 全部账单列表
 */
public class BillFragment extends Fragment {

    private DatabaseHelper db;
    private BillAdapter adapter;
    private TextView tvExpense, tvIncome, tvBalance, tvEmpty;
    private View bannerBackup, emptyState;
    private TextView btnShowHidden;
    private boolean showAll = false; // 是否显示全部账单（默认只显示最近 3 天）

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bill, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new DatabaseHelper(requireContext());

        tvExpense = view.findViewById(R.id.tv_month_expense);
        tvIncome = view.findViewById(R.id.tv_month_income);
        tvBalance = view.findViewById(R.id.tv_month_balance);
        tvEmpty = view.findViewById(R.id.tv_empty);
        emptyState = view.findViewById(R.id.empty_state);
        bannerBackup = view.findViewById(R.id.banner_backup);
        btnShowHidden = view.findViewById(R.id.btn_show_hidden);
        btnShowHidden.setOnClickListener(v -> {
            showAll = !showAll;
            loadData();
        });

        RecyclerView recyclerView = view.findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BillAdapter(new HashMap<>(), new HashMap<>());
        adapter.setListener(new BillAdapter.Listener() {
            @Override
            public void onClick(Transaction t) {
                if (adapter.isSelectionMode()) {
                    return;
                }
                Intent intent = new Intent(requireContext(), AddTransactionActivity.class);
                intent.putExtra(AddTransactionActivity.EXTRA_ID, t.id);
                startActivity(intent);
            }

            @Override
            public void onLongClick(Transaction t) {
                if (adapter.isSelectionMode()) return;
                confirmDelete(t);
            }

            @Override
            public void onSelectionChanged(int count) {
            }
        });
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = view.findViewById(R.id.fab_add);
        fab.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddTransactionActivity.class)));

        view.findViewById(R.id.fab_batch).setOnClickListener(v -> toggleBatchMode());

        // 大绿色按钮 → 快速记账
        view.findViewById(R.id.btn_add_new).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddTransactionActivity.class)));

        // 备份提示条 → 弹导出/备份
        bannerBackup.setOnClickListener(v -> showBackupDialog());
        view.findViewById(R.id.iv_banner_close).setOnClickListener(v -> bannerBackup.setVisibility(View.GONE));

        // 右上角搜索
        view.findViewById(R.id.iv_search).setOnClickListener(v -> showSearchDialog());

        // 右上角菜单
        view.findViewById(R.id.iv_more).setOnClickListener(v -> showMoreMenu(v));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        // 本月汇总
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long monthStart = c.getTimeInMillis();
        long now = System.currentTimeMillis();

        double expense = db.sumByType(Transaction.TYPE_EXPENSE, monthStart, now);
        double income = db.sumByType(Transaction.TYPE_INCOME, monthStart, now);
        tvExpense.setText("¥" + Palette.formatAmount(expense));
        tvIncome.setText("¥" + Palette.formatAmount(income));
        tvBalance.setText("¥" + Palette.formatAmount(income - expense));

        // 账单（默认只显示最近 3 天，避免数据量大时卡顿；showAll 时显示全部）
        long threeDaysAgo = now - 3L * 24 * 3600 * 1000;
        Map<Long, String> channelNames = new HashMap<>();
        for (Channel ch : db.queryChannels()) channelNames.put(ch.id, ch.name);
        Map<Long, Category> categoryMap = new HashMap<>();
        for (Category cat : db.queryCategories(Category.TYPE_EXPENSE)) categoryMap.put(cat.id, cat);
        for (Category cat : db.queryCategories(Category.TYPE_INCOME)) categoryMap.put(cat.id, cat);

        List<Transaction> list;
        if (showAll) {
            list = db.queryTransactions(0L, Long.MAX_VALUE);
        } else {
            list = db.queryTransactions(threeDaysAgo, now);
        }
        adapter.setMaps(channelNames, categoryMap);
        adapter.setItems(list);

        // 隐藏账单按钮
        if (!showAll) {
            int hiddenCount = db.countTransactions(0L, threeDaysAgo);
            if (hiddenCount > 0) {
                btnShowHidden.setText("📂 打开隐藏账单（" + hiddenCount + " 条）");
                btnShowHidden.setVisibility(View.VISIBLE);
            } else {
                btnShowHidden.setVisibility(View.GONE);
            }
        } else {
            btnShowHidden.setText("📁 收起隐藏账单");
            btnShowHidden.setVisibility(View.VISIBLE);
        }

        emptyState.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirmDelete(Transaction t) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除账单")
                .setMessage("确定删除这笔记录吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    db.deleteTransaction(t.id);
                    loadData();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void toggleBatchMode() {
        if (adapter.isSelectionMode()) {
            int count = adapter.getSelectedIds().size();
            if (count > 0) {
                confirmBatchDelete(count);
            } else {
                adapter.setSelectionMode(false);
            }
        } else {
            adapter.setSelectionMode(true);
            android.widget.Toast.makeText(requireContext(),
                    "批量选择模式：点击账单勾选，再点左下角按钮删除", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmBatchDelete(int count) {
        new AlertDialog.Builder(requireContext())
                .setTitle("批量删除")
                .setMessage("确定删除选中的 " + count + " 条账单吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    for (Long id : adapter.getSelectedIds()) {
                        db.deleteTransaction(id);
                    }
                    adapter.setSelectionMode(false);
                    loadData();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 备份提示条点击：导出 CSV 或备份数据库 */
    private void showBackupDialog() {
        String[] items = {"📤 导出账单 (CSV)", "💾 备份数据库", "❌ 取消"};
        new AlertDialog.Builder(requireContext())
                .setTitle("数据备份")
                .setItems(items, (d, w) -> {
                    if (w == 0) exportCsv();
                    else if (w == 1) backupDb();
                })
                .show();
    }

    private void exportCsv() {
        String path = Exporter.exportCsv(requireContext(), db);
        Toast.makeText(requireContext(), path == null ? "导出失败" : ("已导出：" + path), Toast.LENGTH_LONG).show();
    }

    private void backupDb() {
        String path = Exporter.backupDatabase(requireContext(), db);
        Toast.makeText(requireContext(), path == null ? "备份失败" : ("已备份：" + path), Toast.LENGTH_LONG).show();
    }

    /** 搜索账单：按备注关键字 / 金额匹配 */
    private void showSearchDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("输入备注关键字或金额，例如：咖啡 / 35");
        input.setMinLines(2);
        new AlertDialog.Builder(requireContext())
                .setTitle("🔍 搜索账单")
                .setView(input)
                .setPositiveButton("搜索", (d, w) -> doSearch(input.getText().toString().trim()))
                .setNegativeButton("取消", null)
                .show();
    }

    private void doSearch(String keyword) {
        if (keyword.isEmpty()) {
            loadData();
            return;
        }
        // 搜索时隐藏"打开隐藏账单"按钮（搜索结果已含全部匹配，按钮状态会误导）
        btnShowHidden.setVisibility(View.GONE);
        List<Transaction> all = db.queryTransactions(0L, Long.MAX_VALUE);
        List<Transaction> filtered = new ArrayList<>();
        for (Transaction t : all) {
            String note = t.note == null ? "" : t.note;
            String amount = String.valueOf(t.amount);
            if (note.contains(keyword) || amount.contains(keyword)) {
                filtered.add(t);
            }
        }
        adapter.setItems(filtered);
        tvEmpty.setText("未找到匹配的账单");
        emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        if (filtered.isEmpty()) {
            Toast.makeText(requireContext(), "没找到匹配的账单", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "找到 " + filtered.size() + " 条", Toast.LENGTH_SHORT).show();
        }
    }

    /** 右上角弹 PopupMenu：更多操作 */
    private void showMoreMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(0, 1, 0, "🔄 刷新数据");
        menu.getMenu().add(0, 2, 1, "📊 本月统计");
        menu.getMenu().add(0, 3, 2, "📤 导出账单 (CSV)");
        menu.getMenu().add(0, 4, 3, "💾 备份数据库");
        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) { loadData(); Toast.makeText(requireContext(), "已刷新", Toast.LENGTH_SHORT).show(); }
            else if (id == 2) startActivity(new Intent(requireContext(), com.example.jizhang.FinanceOverviewActivity.class));
            else if (id == 3) exportCsv();
            else if (id == 4) backupDb();
            return true;
        });
        menu.show();
    }
}
