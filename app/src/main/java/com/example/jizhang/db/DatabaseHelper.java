package com.example.jizhang.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.jizhang.model.Category;
import com.example.jizhang.model.Channel;
import com.example.jizhang.model.Note;
import com.example.jizhang.model.NotifyLog;
import com.example.jizhang.model.PendingBill;
import com.example.jizhang.model.StatItem;
import com.example.jizhang.model.Transaction;
import com.example.jizhang.model.TrendPoint;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 记账数据库助手：统一管理账单、渠道、分类三张表及统计查询
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "jizhang.db";
    private static final int DB_VERSION = 5;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_channel (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL)");

        db.execSQL("CREATE TABLE tbl_category (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "type INTEGER NOT NULL, " +
                "color_index INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE tbl_transaction (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "type INTEGER NOT NULL, " +
                "amount REAL NOT NULL, " +
                "channel_id INTEGER NOT NULL, " +
                "category_id INTEGER NOT NULL, " +
                "date_time INTEGER NOT NULL, " +
                "note TEXT)");

        db.execSQL("CREATE TABLE tbl_pending (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "type INTEGER NOT NULL, " +
                "amount REAL NOT NULL, " +
                "source TEXT NOT NULL, " +
                "content TEXT, " +
                "date_time INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE tbl_notify_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "source TEXT NOT NULL, " +
                "title TEXT, " +
                "text TEXT, " +
                "parsed INTEGER NOT NULL, " +
                "result_type INTEGER NOT NULL, " +
                "result_amount REAL NOT NULL, " +
                "date_time INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE tbl_note (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "content TEXT, " +
                "color_index INTEGER NOT NULL, " +
                "todo INTEGER NOT NULL DEFAULT 0, " +
                "create_time INTEGER NOT NULL, " +
                "update_time INTEGER NOT NULL)");

        seedDefaultData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE tbl_pending (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "type INTEGER NOT NULL, " +
                    "amount REAL NOT NULL, " +
                    "source TEXT NOT NULL, " +
                    "content TEXT, " +
                    "date_time INTEGER NOT NULL)");
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE tbl_notify_log (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "source TEXT NOT NULL, " +
                    "title TEXT, " +
                    "text TEXT, " +
                    "parsed INTEGER NOT NULL, " +
                    "result_type INTEGER NOT NULL, " +
                    "result_amount REAL NOT NULL, " +
                    "date_time INTEGER NOT NULL)");
        }
        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE tbl_note (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT NOT NULL, " +
                    "content TEXT, " +
                    "color_index INTEGER NOT NULL, " +
                    "create_time INTEGER NOT NULL, " +
                    "update_time INTEGER NOT NULL)");
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE tbl_note ADD COLUMN todo INTEGER NOT NULL DEFAULT 0");
        }
    }

    /** 初始化预设渠道与分类 */
    private void seedDefaultData(SQLiteDatabase db) {
        String[] channels = {"微信", "支付宝", "银行卡", "现金", "其他"};
        for (String c : channels) {
            ContentValues cv = new ContentValues();
            cv.put("name", c);
            db.insert("tbl_channel", null, cv);
        }

        // 预设支出分类
        String[] expenseCats = {"餐饮", "交通", "购物", "住房", "娱乐", "医疗", "教育", "通讯", "日用", "其他"};
        for (int i = 0; i < expenseCats.length; i++) {
            ContentValues cv = new ContentValues();
            cv.put("name", expenseCats[i]);
            cv.put("type", Category.TYPE_EXPENSE);
            cv.put("color_index", i);
            db.insert("tbl_category", null, cv);
        }

        // 预设收入分类
        String[] incomeCats = {"工资", "奖金", "理财", "兼职", "红包", "其他"};
        for (int i = 0; i < incomeCats.length; i++) {
            ContentValues cv = new ContentValues();
            cv.put("name", incomeCats[i]);
            cv.put("type", Category.TYPE_INCOME);
            cv.put("color_index", i);
            db.insert("tbl_category", null, cv);
        }
    }

    // ==================== 账单 ====================

    public long insertTransaction(Transaction t) {
        ContentValues cv = new ContentValues();
        cv.put("type", t.type);
        cv.put("amount", t.amount);
        cv.put("channel_id", t.channelId);
        cv.put("category_id", t.categoryId);
        cv.put("date_time", t.dateTime);
        cv.put("note", t.note);
        return getWritableDatabase().insert("tbl_transaction", null, cv);
    }

    public void updateTransaction(Transaction t) {
        ContentValues cv = new ContentValues();
        cv.put("type", t.type);
        cv.put("amount", t.amount);
        cv.put("channel_id", t.channelId);
        cv.put("category_id", t.categoryId);
        cv.put("date_time", t.dateTime);
        cv.put("note", t.note);
        getWritableDatabase().update("tbl_transaction", cv, "id=?", new String[]{String.valueOf(t.id)});
    }

    public void deleteTransaction(long id) {
        getWritableDatabase().delete("tbl_transaction", "id=?", new String[]{String.valueOf(id)});
    }

    /** 按时间范围查询账单（含端点），按时间倒序 */
    public List<Transaction> queryTransactions(long start, long end) {
        List<Transaction> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM tbl_transaction WHERE date_time >= ? AND date_time <= ? ORDER BY date_time DESC",
                new String[]{String.valueOf(start), String.valueOf(end)});
        while (c.moveToNext()) {
            list.add(readTransaction(c));
        }
        c.close();
        return list;
    }

    /** 统计时间范围内的账单数量（只 COUNT 不加载数据，用于"隐藏账单"提示） */
    public int countTransactions(long start, long end) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM tbl_transaction WHERE date_time >= ? AND date_time <= ?",
                new String[]{String.valueOf(start), String.valueOf(end)});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    /** 按渠道查询账单（倒序，用于账户明细） */
    public List<Transaction> queryTransactionsByChannel(long channelId, long start, long end) {
        List<Transaction> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM tbl_transaction WHERE channel_id=? AND date_time >= ? AND date_time <= ? ORDER BY date_time DESC",
                new String[]{String.valueOf(channelId), String.valueOf(start), String.valueOf(end)});
        while (c.moveToNext()) {
            list.add(readTransaction(c));
        }
        c.close();
        return list;
    }

    public Transaction getTransaction(long id) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM tbl_transaction WHERE id=?", new String[]{String.valueOf(id)});
        Transaction t = null;
        if (c.moveToFirst()) {
            t = readTransaction(c);
        }
        c.close();
        return t;
    }

    private Transaction readTransaction(Cursor c) {
        Transaction t = new Transaction();
        t.id = c.getLong(c.getColumnIndexOrThrow("id"));
        t.type = c.getInt(c.getColumnIndexOrThrow("type"));
        t.amount = c.getDouble(c.getColumnIndexOrThrow("amount"));
        t.channelId = c.getLong(c.getColumnIndexOrThrow("channel_id"));
        t.categoryId = c.getLong(c.getColumnIndexOrThrow("category_id"));
        t.dateTime = c.getLong(c.getColumnIndexOrThrow("date_time"));
        t.note = c.getString(c.getColumnIndexOrThrow("note"));
        return t;
    }

    // ==================== 渠道 ====================

    public long insertChannel(String name) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        return getWritableDatabase().insert("tbl_channel", null, cv);
    }

    public void updateChannel(long id, String name) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        getWritableDatabase().update("tbl_channel", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteChannel(long id) {
        SQLiteDatabase db = getWritableDatabase();
        // 把该渠道的账单转移到第一个其他渠道，避免孤儿数据
        Cursor c = db.rawQuery("SELECT id FROM tbl_channel WHERE id != ? ORDER BY id ASC LIMIT 1",
                new String[]{String.valueOf(id)});
        if (c.moveToFirst()) {
            long target = c.getLong(0);
            c.close();
            ContentValues cv = new ContentValues();
            cv.put("channel_id", target);
            db.update("tbl_transaction", cv, "channel_id=?", new String[]{String.valueOf(id)});
        } else {
            c.close();
        }
        db.delete("tbl_channel", "id=?", new String[]{String.valueOf(id)});
    }

    public List<Channel> queryChannels() {
        List<Channel> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM tbl_channel ORDER BY id ASC", null);
        while (c.moveToNext()) {
            Channel ch = new Channel();
            ch.id = c.getLong(c.getColumnIndexOrThrow("id"));
            ch.name = c.getString(c.getColumnIndexOrThrow("name"));
            list.add(ch);
        }
        c.close();
        return list;
    }

    public Channel getChannel(long id) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM tbl_channel WHERE id=?", new String[]{String.valueOf(id)});
        Channel ch = null;
        if (c.moveToFirst()) {
            ch = new Channel();
            ch.id = c.getLong(c.getColumnIndexOrThrow("id"));
            ch.name = c.getString(c.getColumnIndexOrThrow("name"));
        }
        c.close();
        return ch;
    }

    // ==================== 分类 ====================

    public long insertCategory(String name, int type, int colorIndex) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("type", type);
        cv.put("color_index", colorIndex);
        return getWritableDatabase().insert("tbl_category", null, cv);
    }

    public void updateCategory(long id, String name, int colorIndex) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("color_index", colorIndex);
        getWritableDatabase().update("tbl_category", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteCategory(long id) {
        getWritableDatabase().delete("tbl_category", "id=?", new String[]{String.valueOf(id)});
    }

    public List<Category> queryCategories(int type) {
        List<Category> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM tbl_category WHERE type=? ORDER BY id ASC",
                new String[]{String.valueOf(type)});
        while (c.moveToNext()) {
            Category cat = new Category();
            cat.id = c.getLong(c.getColumnIndexOrThrow("id"));
            cat.name = c.getString(c.getColumnIndexOrThrow("name"));
            cat.type = c.getInt(c.getColumnIndexOrThrow("type"));
            cat.colorIndex = c.getInt(c.getColumnIndexOrThrow("color_index"));
            list.add(cat);
        }
        c.close();
        return list;
    }

    public Category getCategory(long id) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM tbl_category WHERE id=?", new String[]{String.valueOf(id)});
        Category cat = null;
        if (c.moveToFirst()) {
            cat = new Category();
            cat.id = c.getLong(c.getColumnIndexOrThrow("id"));
            cat.name = c.getString(c.getColumnIndexOrThrow("name"));
            cat.type = c.getInt(c.getColumnIndexOrThrow("type"));
            cat.colorIndex = c.getInt(c.getColumnIndexOrThrow("color_index"));
        }
        c.close();
        return cat;
    }

    // ==================== 统计 ====================

    /** 时间范围内某类型的总金额 */
    public double sumByType(int type, long start, long end) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT SUM(amount) FROM tbl_transaction WHERE type=? AND date_time>=? AND date_time<=?",
                new String[]{String.valueOf(type), String.valueOf(start), String.valueOf(end)});
        double sum = 0;
        if (c.moveToFirst() && !c.isNull(0)) {
            sum = c.getDouble(0);
        }
        c.close();
        return sum;
    }

    /** 按渠道过滤的收支求和（用于多账户独立统计） */
    public double sumByChannel(int type, long channelId, long start, long end) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT SUM(amount) FROM tbl_transaction WHERE type=? AND channel_id=? AND date_time>=? AND date_time<=?",
                new String[]{String.valueOf(type), String.valueOf(channelId),
                        String.valueOf(start), String.valueOf(end)});
        double sum = 0;
        if (c.moveToFirst() && !c.isNull(0)) {
            sum = c.getDouble(0);
        }
        c.close();
        return sum;
    }

    /** 按分类汇总（饼图数据） */
    public List<StatItem> sumByCategory(int type, long start, long end) {
        List<StatItem> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT category_id, SUM(amount) AS s FROM tbl_transaction " +
                        "WHERE type=? AND date_time>=? AND date_time<=? GROUP BY category_id ORDER BY s DESC",
                new String[]{String.valueOf(type), String.valueOf(start), String.valueOf(end)});
        while (c.moveToNext()) {
            long catId = c.getLong(0);
            double sum = c.getDouble(1);
            Category cat = getCategory(catId);
            String name = cat != null ? cat.name : "未知";
            int colorIndex = cat != null ? cat.colorIndex : 0;
            list.add(new StatItem(name, sum, colorIndex));
        }
        c.close();
        return list;
    }

    /** 按渠道分别返回支出与收入（用于柱状图双柱） */
    public List<StatItem> channelExpense(long start, long end) {
        List<StatItem> list = new ArrayList<>();
        List<Channel> channels = queryChannels();
        for (Channel ch : channels) {
            double v = channelSum(ch.id, 0, start, end);
            list.add(new StatItem(ch.name, v, 0));
        }
        return list;
    }

    public List<StatItem> channelIncome(long start, long end) {
        List<StatItem> list = new ArrayList<>();
        List<Channel> channels = queryChannels();
        for (Channel ch : channels) {
            double v = channelSum(ch.id, 1, start, end);
            list.add(new StatItem(ch.name, v, 1));
        }
        return list;
    }

    private double channelSum(long channelId, int type, long start, long end) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT SUM(amount) FROM tbl_transaction WHERE channel_id=? AND type=? AND date_time>=? AND date_time<=?",
                new String[]{String.valueOf(channelId), String.valueOf(type), String.valueOf(start), String.valueOf(end)});
        double sum = 0;
        if (c.moveToFirst() && !c.isNull(0)) {
            sum = c.getDouble(0);
        }
        c.close();
        return sum;
    }

    /** 按日聚合的趋势数据（折线图），自动按范围选择粒度 */
    public List<TrendPoint> trend(long start, long end) {
        List<TrendPoint> list = new ArrayList<>();
        long span = end - start;
        // 超过 90 天按月聚合，否则按日聚合
        boolean byMonth = span > 90L * 24 * 3600 * 1000;

        Cursor c = getReadableDatabase().rawQuery(
                "SELECT date_time, type, amount FROM tbl_transaction WHERE date_time>=? AND date_time<=? ORDER BY date_time ASC",
                new String[]{String.valueOf(start), String.valueOf(end)});
        Calendar cal = Calendar.getInstance();
        while (c.moveToNext()) {
            long dt = c.getLong(0);
            int type = c.getInt(1);
            double amount = c.getDouble(2);
            cal.setTimeInMillis(dt);
            if (byMonth) {
                cal.set(Calendar.DAY_OF_MONTH, 1);
            }
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long key = cal.getTimeInMillis();

            TrendPoint tp = findPoint(list, key);
            if (tp == null) {
                tp = new TrendPoint(key, 0, 0);
                list.add(tp);
            }
            if (type == Transaction.TYPE_EXPENSE) {
                tp.expense += amount;
            } else {
                tp.income += amount;
            }
        }
        c.close();
        return list;
    }

    private TrendPoint findPoint(List<TrendPoint> list, long key) {
        for (TrendPoint p : list) {
            if (p.dateMillis == key) return p;
        }
        return null;
    }

    // ==================== 待确认账单 ====================

    public long insertPending(int type, double amount, String source, String content, long dateTime) {
        ContentValues cv = new ContentValues();
        cv.put("type", type);
        cv.put("amount", amount);
        cv.put("source", source);
        cv.put("content", content);
        cv.put("date_time", dateTime);
        return getWritableDatabase().insert("tbl_pending", null, cv);
    }

    /** 是否已存在重复的待确认账单（同来源+同金额+5分钟窗口内） */
    public boolean hasDuplicatePending(int type, double amount, String source, long dateTime) {
        long window = 5 * 60 * 1000L;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM tbl_pending WHERE type=? AND amount=? AND source=? " +
                        "AND date_time>=? AND date_time<=?",
                new String[]{String.valueOf(type), String.valueOf(amount), source,
                        String.valueOf(dateTime - window), String.valueOf(dateTime + window)});
        boolean dup = false;
        if (c.moveToFirst()) dup = c.getInt(0) > 0;
        c.close();
        return dup;
    }

    public List<PendingBill> queryPending() {
        List<PendingBill> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM tbl_pending ORDER BY date_time DESC", null);
        while (c.moveToNext()) {
            PendingBill p = new PendingBill();
            p.id = c.getLong(c.getColumnIndexOrThrow("id"));
            p.type = c.getInt(c.getColumnIndexOrThrow("type"));
            p.amount = c.getDouble(c.getColumnIndexOrThrow("amount"));
            p.source = c.getString(c.getColumnIndexOrThrow("source"));
            p.content = c.getString(c.getColumnIndexOrThrow("content"));
            p.dateTime = c.getLong(c.getColumnIndexOrThrow("date_time"));
            list.add(p);
        }
        c.close();
        return list;
    }

    public int pendingCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM tbl_pending", null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    public void deletePending(long id) {
        getWritableDatabase().delete("tbl_pending", "id=?", new String[]{String.valueOf(id)});
    }

    /** 确认入账：匹配/创建渠道，写入正式账单，删除待确认记录 */
    public boolean confirmPending(long pendingId, long categoryId) {
        PendingBill p = null;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM tbl_pending WHERE id=?", new String[]{String.valueOf(pendingId)});
        if (c.moveToFirst()) {
            p = new PendingBill();
            p.id = c.getLong(c.getColumnIndexOrThrow("id"));
            p.type = c.getInt(c.getColumnIndexOrThrow("type"));
            p.amount = c.getDouble(c.getColumnIndexOrThrow("amount"));
            p.source = c.getString(c.getColumnIndexOrThrow("source"));
            p.content = c.getString(c.getColumnIndexOrThrow("content"));
            p.dateTime = c.getLong(c.getColumnIndexOrThrow("date_time"));
        }
        c.close();
        if (p == null) return false;

        long channelId = findOrCreateChannel(p.source);
        Transaction t = new Transaction();
        t.type = p.type;
        t.amount = p.amount;
        t.channelId = channelId;
        t.categoryId = categoryId;
        t.dateTime = p.dateTime;
        t.note = p.content;
        insertTransaction(t);
        deletePending(pendingId);
        return true;
    }

    public long findOrCreateChannel(String name) {
        for (Channel ch : queryChannels()) {
            if (name.equals(ch.name)) return ch.id;
        }
        return insertChannel(name);
    }

    /** 找某类型的默认分类：优先"其他"，否则第一个，否则自动创建一个 */
    public long findOrCreateDefaultCategory(int type) {
        List<Category> cats = queryCategories(type);
        for (Category cat : cats) {
            if ("其他".equals(cat.name)) return cat.id;
        }
        if (!cats.isEmpty()) return cats.get(0).id;
        return insertCategory("其他", type, 0);
    }

    /** 是否已存在重复账单（同来源+同金额+5分钟窗口内），用于通知识别直接入账去重 */
    public boolean hasDuplicateTransaction(int type, double amount, String source, long dateTime) {
        long window = 5 * 60 * 1000L;
        long channelId = findOrCreateChannel(source);
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM tbl_transaction WHERE type=? AND amount=? AND channel_id=? " +
                        "AND date_time>=? AND date_time<=?",
                new String[]{String.valueOf(type), String.valueOf(amount), String.valueOf(channelId),
                        String.valueOf(dateTime - window), String.valueOf(dateTime + window)});
        boolean dup = false;
        if (c.moveToFirst()) dup = c.getInt(0) > 0;
        c.close();
        return dup;
    }

    // ==================== 通知识别日志 ====================

    public long insertNotifyLog(String source, String title, String text,
                                boolean parsed, int resultType, double resultAmount, long dateTime) {
        ContentValues cv = new ContentValues();
        cv.put("source", source);
        cv.put("title", title);
        cv.put("text", text);
        cv.put("parsed", parsed ? 1 : 0);
        cv.put("result_type", resultType);
        cv.put("result_amount", resultAmount);
        cv.put("date_time", dateTime);
        long id = getWritableDatabase().insert("tbl_notify_log", null, cv);
        // 仅保留最近 200 条，避免日志无限膨胀
        getWritableDatabase().delete("tbl_notify_log",
                "id NOT IN (SELECT id FROM tbl_notify_log ORDER BY id DESC LIMIT 200)", null);
        return id;
    }

    public List<NotifyLog> queryNotifyLog(int limit) {
        List<NotifyLog> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM tbl_notify_log ORDER BY date_time DESC LIMIT " + Math.max(1, limit), null);
        while (c.moveToNext()) {
            NotifyLog l = new NotifyLog();
            l.id = c.getLong(c.getColumnIndexOrThrow("id"));
            l.source = c.getString(c.getColumnIndexOrThrow("source"));
            l.title = c.getString(c.getColumnIndexOrThrow("title"));
            l.text = c.getString(c.getColumnIndexOrThrow("text"));
            l.parsed = c.getInt(c.getColumnIndexOrThrow("parsed")) == 1;
            l.resultType = c.getInt(c.getColumnIndexOrThrow("result_type"));
            l.resultAmount = c.getDouble(c.getColumnIndexOrThrow("result_amount"));
            l.dateTime = c.getLong(c.getColumnIndexOrThrow("date_time"));
            list.add(l);
        }
        c.close();
        return list;
    }

    public void clearNotifyLog() {
        getWritableDatabase().delete("tbl_notify_log", null, null);
    }

    // ==================== 笔记 ====================

    public long insertNote(Note n) {
        ContentValues cv = new ContentValues();
        cv.put("title", n.title);
        cv.put("content", n.content);
        cv.put("color_index", n.colorIndex);
        cv.put("todo", n.todo);
        cv.put("create_time", n.createTime);
        cv.put("update_time", n.updateTime);
        return getWritableDatabase().insert("tbl_note", null, cv);
    }

    /** 是否已存在重复待办（同内容 + 5分钟窗口内），用于通知无法识别时加入待办去重 */
    public boolean hasDuplicateTodo(String content, long time) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM tbl_note WHERE todo=1 AND content=? AND create_time>=?",
                new String[]{content, String.valueOf(time - 5L * 60 * 1000)});
        boolean dup = false;
        if (c.moveToFirst()) dup = c.getInt(0) > 0;
        c.close();
        return dup;
    }

    public void updateNote(Note n) {
        ContentValues cv = new ContentValues();
        cv.put("title", n.title);
        cv.put("content", n.content);
        cv.put("color_index", n.colorIndex);
        cv.put("todo", n.todo);
        cv.put("update_time", n.updateTime);
        getWritableDatabase().update("tbl_note", cv, "id=?", new String[]{String.valueOf(n.id)});
    }

    public void deleteNote(long id) {
        getWritableDatabase().delete("tbl_note", "id=?", new String[]{String.valueOf(id)});
    }

    public List<Note> queryNotes() {
        List<Note> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM tbl_note ORDER BY update_time DESC", null);
        while (c.moveToNext()) {
            Note n = new Note();
            n.id = c.getLong(c.getColumnIndexOrThrow("id"));
            n.title = c.getString(c.getColumnIndexOrThrow("title"));
            n.content = c.getString(c.getColumnIndexOrThrow("content"));
            n.colorIndex = c.getInt(c.getColumnIndexOrThrow("color_index"));
            n.todo = c.getInt(c.getColumnIndexOrThrow("todo"));
            n.createTime = c.getLong(c.getColumnIndexOrThrow("create_time"));
            n.updateTime = c.getLong(c.getColumnIndexOrThrow("update_time"));
            list.add(n);
        }
        c.close();
        return list;
    }

    /** 按类型查询笔记：todoMode=0 普通笔记，1 待办（未完成在前） */
    public List<Note> queryNotesByTodo(int todoMode) {
        List<Note> list = new ArrayList<>();
        String where = todoMode == 0 ? "todo=0" : "todo>0";
        String order = todoMode == 0 ? "update_time DESC" : "todo ASC, update_time DESC";
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM tbl_note WHERE " + where + " ORDER BY " + order, null);
        while (c.moveToNext()) {
            Note n = new Note();
            n.id = c.getLong(c.getColumnIndexOrThrow("id"));
            n.title = c.getString(c.getColumnIndexOrThrow("title"));
            n.content = c.getString(c.getColumnIndexOrThrow("content"));
            n.colorIndex = c.getInt(c.getColumnIndexOrThrow("color_index"));
            n.todo = c.getInt(c.getColumnIndexOrThrow("todo"));
            n.createTime = c.getLong(c.getColumnIndexOrThrow("create_time"));
            n.updateTime = c.getLong(c.getColumnIndexOrThrow("update_time"));
            list.add(n);
        }
        c.close();
        return list;
    }

    public Note getNote(long id) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM tbl_note WHERE id=?", new String[]{String.valueOf(id)});
        Note n = null;
        if (c.moveToFirst()) {
            n = new Note();
            n.id = c.getLong(c.getColumnIndexOrThrow("id"));
            n.title = c.getString(c.getColumnIndexOrThrow("title"));
            n.content = c.getString(c.getColumnIndexOrThrow("content"));
            n.colorIndex = c.getInt(c.getColumnIndexOrThrow("color_index"));
            n.todo = c.getInt(c.getColumnIndexOrThrow("todo"));
            n.createTime = c.getLong(c.getColumnIndexOrThrow("create_time"));
            n.updateTime = c.getLong(c.getColumnIndexOrThrow("update_time"));
        }
        c.close();
        return n;
    }
}
