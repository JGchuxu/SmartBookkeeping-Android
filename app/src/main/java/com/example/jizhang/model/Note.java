package com.example.jizhang.model;

/**
 * 笔记 / 备忘录
 */
public class Note {
    public long id;
    public String title;
    public String content;
    public int colorIndex;
    public int todo; // 0=普通笔记, 1=待办未完成, 2=待办已完成
    public long createTime;
    public long updateTime;
}