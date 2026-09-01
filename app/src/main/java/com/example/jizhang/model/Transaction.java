package com.example.jizhang.model;

/**
 * 收支记录实体
 */
public class Transaction {
    public static final int TYPE_EXPENSE = 0;
    public static final int TYPE_INCOME = 1;

    public long id;
    public int type;        // 0=支出 1=收入
    public double amount;   // 金额（正数）
    public long channelId;  // 渠道 id
    public long categoryId; // 分类 id
    public long dateTime;   // 时间戳（毫秒）
    public String note;

    public boolean isExpense() {
        return type == TYPE_EXPENSE;
    }

    public boolean isIncome() {
        return type == TYPE_INCOME;
    }
}
