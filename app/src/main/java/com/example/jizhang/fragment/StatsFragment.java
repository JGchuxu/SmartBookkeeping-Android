package com.example.jizhang.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.jizhang.R;
import com.example.jizhang.db.DatabaseHelper;
import com.example.jizhang.model.Transaction;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.example.jizhang.util.Palette;
import com.example.jizhang.view.BarChartView;
import com.example.jizhang.view.LineChartView;
import com.example.jizhang.view.PieChartView;

import java.util.Calendar;

/**
 * 统计页：时间筛选 + 收支汇总 + 饼图/柱状图/折线图
 */
public class StatsFragment extends Fragment {

    private static final int MODE_DAY = 0;
    private static final int MODE_WEEK = 1;
    private static final int MODE_MONTH = 2;
    private static final int MODE_YEAR = 3;
    private static final int MODE_CUSTOM = 4;

    private DatabaseHelper db;

    private TextView tvIncome, tvExpense, tvBalance, tvRange;
    private TextView btnDay, btnWeek, btnMonth, btnYear, btnCustom;
    private PieChartView pieExpense, pieIncome;
    private BarChartView barChart;
    private LineChartView lineChart;

    private int mode = MODE_MONTH;
    private long customStart, customEnd;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new DatabaseHelper(requireContext());

        tvIncome = view.findViewById(R.id.tv_income);
        tvExpense = view.findViewById(R.id.tv_expense);
        tvBalance = view.findViewById(R.id.tv_balance);
        tvRange = view.findViewById(R.id.tv_range);

        btnDay = view.findViewById(R.id.btn_day);
        btnWeek = view.findViewById(R.id.btn_week);
        btnMonth = view.findViewById(R.id.btn_month);
        btnYear = view.findViewById(R.id.btn_year);
        btnCustom = view.findViewById(R.id.btn_custom);

        btnDay.setOnClickListener(v -> setMode(MODE_DAY));
        btnWeek.setOnClickListener(v -> setMode(MODE_WEEK));
        btnMonth.setOnClickListener(v -> setMode(MODE_MONTH));
        btnYear.setOnClickListener(v -> setMode(MODE_YEAR));
        btnCustom.setOnClickListener(v -> pickCustomRange());

        pieExpense = view.findViewById(R.id.pie_expense);
        pieIncome = view.findViewById(R.id.pie_income);
        barChart = view.findViewById(R.id.bar_chart);
        lineChart = view.findViewById(R.id.line_chart);
    }

    @Override
    public void onResume() {
        super.onResume();
        setMode(mode);
    }

    private void setMode(int m) {
        mode = m;
        updateButtons();
        loadStats();
    }

    private void updateButtons() {
        btnDay.setSelected(mode == MODE_DAY);
        btnWeek.setSelected(mode == MODE_WEEK);
        btnMonth.setSelected(mode == MODE_MONTH);
        btnYear.setSelected(mode == MODE_YEAR);
        btnCustom.setSelected(mode == MODE_CUSTOM);
    }

    private long[] computeRange() {
        Calendar c = Calendar.getInstance();
        long end = c.getTimeInMillis();
        long start;
        switch (mode) {
            case MODE_DAY:
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                start = c.getTimeInMillis();
                break;
            case MODE_WEEK:
                c.add(Calendar.DAY_OF_YEAR, -6);
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                start = c.getTimeInMillis();
                break;
            case MODE_YEAR:
                c.set(Calendar.DAY_OF_YEAR, 1);
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                start = c.getTimeInMillis();
                break;
            case MODE_CUSTOM:
                return new long[]{customStart, customEnd};
            case MODE_MONTH:
            default:
                c.set(Calendar.DAY_OF_MONTH, 1);
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                start = c.getTimeInMillis();
                break;
        }
        return new long[]{start, end};
    }

    private void pickCustomRange() {
        MaterialDatePicker<Long> startPicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("选择开始日期")
                .build();
        startPicker.addOnPositiveButtonClickListener(startSel -> {
            MaterialDatePicker<Long> endPicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("选择结束日期")
                    .setSelection(startSel)
                    .build();
            endPicker.addOnPositiveButtonClickListener(endSel -> {
                Calendar s = Calendar.getInstance();
                s.setTimeInMillis(startSel);
                s.set(Calendar.HOUR_OF_DAY, 0);
                s.set(Calendar.MINUTE, 0);
                s.set(Calendar.SECOND, 0);
                s.set(Calendar.MILLISECOND, 0);
                Calendar e = Calendar.getInstance();
                e.setTimeInMillis(endSel);
                e.set(Calendar.HOUR_OF_DAY, 23);
                e.set(Calendar.MINUTE, 59);
                e.set(Calendar.SECOND, 59);
                customStart = s.getTimeInMillis();
                customEnd = e.getTimeInMillis();
                setMode(MODE_CUSTOM);
            });
            endPicker.show(getParentFragmentManager(), "end_picker");
        });
        startPicker.show(getParentFragmentManager(), "start_picker");
    }

    private void loadStats() {
        long[] range = computeRange();
        long start = range[0];
        long end = range[1];

        tvRange.setText(Palette.formatDate(start) + " ~ " + Palette.formatDate(end));

        double income = db.sumByType(Transaction.TYPE_INCOME, start, end);
        double expense = db.sumByType(Transaction.TYPE_EXPENSE, start, end);
        tvIncome.setText("¥" + Palette.formatAmount(income));
        tvExpense.setText("¥" + Palette.formatAmount(expense));
        tvBalance.setText("¥" + Palette.formatAmount(income - expense));

        pieExpense.setData(db.sumByCategory(Transaction.TYPE_EXPENSE, start, end), "总支出");
        pieIncome.setData(db.sumByCategory(Transaction.TYPE_INCOME, start, end), "总收入");
        barChart.setData(db.channelExpense(start, end), db.channelIncome(start, end));
        lineChart.setData(db.trend(start, end));
    }
}
