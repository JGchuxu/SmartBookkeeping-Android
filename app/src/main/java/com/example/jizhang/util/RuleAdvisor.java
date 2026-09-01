package com.example.jizhang.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 财务建议引擎（纯规则、预设建议词，非 AI 模型）
 */
public class RuleAdvisor {

    /**
     * 根据初始金额与收支情况生成建议
     */
    public static List<String> advise(double initial, double totalIncome, double totalExpense,
                                      double monthIncome, double monthExpense,
                                      double topCategoryExpense, String topCategoryName) {
        List<String> tips = new ArrayList<>();
        double balance = initial + totalIncome - totalExpense;

        if (totalIncome + totalExpense < 0.01) {
            tips.add("还没有记账数据，从记下第一笔收支开始吧");
            return tips;
        }

        if (initial > 0) {
            if (balance >= initial * 1.5) {
                tips.add("资金稳步增长，可考虑将结余用于储蓄或低风险理财");
            } else if (balance < initial * 0.2 && balance >= 0) {
                tips.add("余额已不足初始资金的 20%，建议暂停非必要的大额支出");
            }
        }

        if (monthExpense > monthIncome && monthExpense > 0) {
            tips.add("本月支出已超过收入，注意控制消费节奏");
        } else if (monthIncome > monthExpense && monthIncome > 0) {
            tips.add("本月收大于支，结余良好，保持这个节奏");
        }

        if (totalIncome > 0 && totalExpense / totalIncome > 0.9) {
            tips.add("总支出已接近总收入，建议预留一笔应急资金");
        }

        if (monthExpense > 0 && topCategoryExpense / monthExpense > 0.4) {
            tips.add("本月「" + topCategoryName + "」支出占比偏高，可关注是否需要压缩");
        }

        if (tips.isEmpty()) {
            tips.add("收支平衡，按当前节奏记账即可");
        }
        return tips;
    }
}
