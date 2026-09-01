package com.example.jizhang.model;

/**
 * 趋势数据点（用于折线图）
 */
public class TrendPoint {
    public long dateMillis;
    public double expense;
    public double income;

    public TrendPoint(long dateMillis, double expense, double income) {
        this.dateMillis = dateMillis;
        this.expense = expense;
        this.income = income;
    }
}
