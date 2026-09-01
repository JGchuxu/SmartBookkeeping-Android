package com.example.jizhang.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 颜色与格式化工具
 */
public class Palette {

    /** 支出红、收入绿（记账惯例，红涨绿跌） */
    public static final int COLOR_EXPENSE = 0xFFFF5A5A;
    public static final int COLOR_INCOME = 0xFF43C478;

    /** 分类 / 笔记配色盘（54 色，按色系分组：红/橙/黄/绿/青/蓝/紫/粉/灰） */
    private static final int[] COLORS = {
            // 红
            0xFFE53935, 0xFFEF5350, 0xFFE57373, 0xFFEF9A9A, 0xFFFFCDD2, 0xFFFF5252,
            // 橙
            0xFFFF9800, 0xFFFFAB40, 0xFFFFB74D, 0xFFFFCC80, 0xFFFFE0B2, 0xFFFF6E40,
            // 黄
            0xFFFFC107, 0xFFFFEB3B, 0xFFFFCA28, 0xFFFFD54F, 0xFFFFE082, 0xFFFFF59D,
            // 绿
            0xFF4CAF50, 0xFF00C853, 0xFF66BB6A, 0xFF81C784, 0xFFA5D6A7, 0xFFC8E6C9,
            // 青
            0xFF009688, 0xFF1DE9B6, 0xFF26A69A, 0xFF4DB6AC, 0xFF80CBC4, 0xFFB2DFDB,
            // 蓝
            0xFF2196F3, 0xFF448AFF, 0xFF42A5F5, 0xFF64B5F6, 0xFF90CAF9, 0xFFBBDEFB,
            // 紫
            0xFF673AB7, 0xFF8C9EFF, 0xFF7E57C2, 0xFF9575CD, 0xFFB39DDB, 0xFFD1C4E9,
            // 粉
            0xFFEC407A, 0xFFFF4081, 0xFFF06292, 0xFFF48FB1, 0xFFF8BBD0, 0xFFFF80AB,
            // 灰
            0xFF455A64, 0xFF607D8B, 0xFF78909C, 0xFF90A4AE, 0xFFB0BEC5, 0xFFCFD8DC
    };

    public static int colorOf(int index) {
        if (index < 0 || index >= COLORS.length) index = 0;
        return COLORS[index];
    }

    public static int colorCount() {
        return COLORS.length;
    }

    /** 金额：千分位 + 2 位小数 */
    public static String formatAmount(double v) {
        return String.format(Locale.CHINA, "%,.2f", v);
    }

    /** 金额简写（用于坐标轴） */
    public static String formatAmountShort(double v) {
        if (v >= 100000000) return String.format(Locale.CHINA, "%.1f亿", v / 100000000);
        if (v >= 10000) return String.format(Locale.CHINA, "%.1f万", v / 10000);
        if (v >= 1000) return String.format(Locale.CHINA, "%.1f千", v / 1000);
        return String.format(Locale.CHINA, "%.0f", v);
    }

    public static String formatDate(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date(millis));
    }

    public static String formatDateTime(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date(millis));
    }

    public static String formatMonthDay(long millis) {
        return new SimpleDateFormat("MM-dd", Locale.CHINA).format(new Date(millis));
    }

    public static String formatTime(long millis) {
        return new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(millis));
    }
}
