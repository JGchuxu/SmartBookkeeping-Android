package com.example.jizhang.util;

/**
 * 分类 emoji 图标映射（二次元萌系风格，菲比/鼠鼠等萌宠主题的 emoji 兜底）
 */
public class EmojiIcons {

    /** 按分类名返回萌系 emoji，未匹配返回萌宠爪印 */
    public static String category(String name) {
        if (name == null) return "🐾";
        switch (name) {
            // 支出分类
            case "餐饮": return "🍜";
            case "交通": return "🚗";
            case "购物": return "🛍️";
            case "住房": return "🏠";
            case "娱乐": return "🎮";
            case "医疗": return "💊";
            case "教育": return "📚";
            case "通讯": return "📱";
            case "日用": return "🛒";
            case "其他": return "📦";
            // 收入分类
            case "工资": return "💰";
            case "奖金": return "🏆";
            case "理财": return "📈";
            case "兼职": return "💼";
            case "红包": return "🧧";
            default: return "🐾";
        }
    }
}
