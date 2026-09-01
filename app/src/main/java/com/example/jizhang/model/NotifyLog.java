package com.example.jizhang.model;

/**
 * 通知识别日志（诊断用）
 */
public class NotifyLog {
    public long id;
    public String source;     // 微信 / 支付宝
    public String title;
    public String text;
    public boolean parsed;    // 是否解析成功
    public int resultType;    // -1=未识别 0=支出 1=收入
    public double resultAmount;
    public long dateTime;

    public String typeLabel() {
        if (!parsed) return "未识别";
        return resultType == Transaction.TYPE_INCOME ? "收入" : "支出";
    }
}
