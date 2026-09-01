package com.example.jizhang.model;

/**
 * 待确认账单（通知识别结果，需用户确认后入账）
 */
public class PendingBill {
    public long id;
    public int type;       // 0=支出 1=收入
    public double amount;
    public String source;  // 微信 / 支付宝
    public String content; // 原始通知文本
    public long dateTime;

    public boolean isExpense() {
        return type == Transaction.TYPE_EXPENSE;
    }
}
