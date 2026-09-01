package com.example.jizhang.util;

import android.content.Context;

import com.example.jizhang.db.DatabaseHelper;
import com.example.jizhang.model.Category;
import com.example.jizhang.model.Channel;
import com.example.jizhang.model.Transaction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 数据导出与备份工具
 */
public class Exporter {

    /** 导出全部账单为 CSV（带 BOM，Excel 可直接打开） */
    public static String exportCsv(Context context, DatabaseHelper db) {
        File dir = backupDir(context);
        String name = "账单导出_" + timestamp() + ".csv";
        File file = new File(dir, name);
        try {
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            writer.write('\ufeff');
            writer.write("类型,金额,渠道,分类,日期时间,备注\n");

            Map<Long, String> channelNames = new HashMap<>();
            for (Channel c : db.queryChannels()) channelNames.put(c.id, c.name);
            Map<Long, String> categoryNames = new HashMap<>();
            for (Category c : db.queryCategories(Category.TYPE_EXPENSE)) categoryNames.put(c.id, c.name);
            for (Category c : db.queryCategories(Category.TYPE_INCOME)) categoryNames.put(c.id, c.name);

            List<Transaction> list = db.queryTransactions(0L, Long.MAX_VALUE);
            for (Transaction t : list) {
                String type = t.isExpense() ? "支出" : "收入";
                String channel = channelNames.containsKey(t.channelId) ? channelNames.get(t.channelId) : "未知";
                String category = categoryNames.containsKey(t.categoryId) ? categoryNames.get(t.categoryId) : "未知";
                String dateTime = Palette.formatDateTime(t.dateTime);
                String note = t.note == null ? "" : t.note.replace(",", "，").replace("\n", " ");
                writer.write(String.format(Locale.CHINA, "%s,%.2f,%s,%s,%s,%s\n",
                        type, t.amount, channel, category, dateTime, note));
            }
            writer.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    /** 备份完整数据库文件 */
    public static String backupDatabase(Context context, DatabaseHelper db) {
        File dir = backupDir(context);
        String name = "数据备份_" + timestamp() + ".db";
        File dest = new File(dir, name);
        try {
            db.close(); // 关闭连接，确保数据已落盘
            File src = context.getDatabasePath("jizhang.db");
            copyFile(src, dest);
            return dest.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    private static File backupDir(Context context) {
        File dir = new File(context.getExternalFilesDir(null), "backup");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private static String timestamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date());
    }

    private static void copyFile(File src, File dest) throws Exception {
        java.io.InputStream in = new java.io.FileInputStream(src);
        java.io.OutputStream out = new FileOutputStream(dest);
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        in.close();
        out.close();
    }
}
