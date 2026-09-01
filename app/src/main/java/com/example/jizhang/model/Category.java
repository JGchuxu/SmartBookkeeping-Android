package com.example.jizhang.model;

/**
 * 收支分类实体
 */
public class Category {
    public static final int TYPE_EXPENSE = 0;
    public static final int TYPE_INCOME = 1;

    public long id;
    public String name;
    public int type;       // 0=支出分类 1=收入分类
    public int colorIndex; // 颜色索引（用于图表与图标）

    public Category() {
    }

    public Category(String name, int type, int colorIndex) {
        this.name = name;
        this.type = type;
        this.colorIndex = colorIndex;
    }

    public Category(long id, String name, int type, int colorIndex) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.colorIndex = colorIndex;
    }
}
