package com.example.jizhang.model;

/**
 * 统计条目（饼图/柱状图数据）
 */
public class StatItem {
    public String name;
    public double value;
    public int colorIndex;

    public StatItem(String name, double value, int colorIndex) {
        this.name = name;
        this.value = value;
        this.colorIndex = colorIndex;
    }
}
