package com.example.jizhang.util;

import com.example.jizhang.model.Transaction;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自然语言记账解析器（纯规则，不依赖 AI 模型）
 * 支持："昨天买咖啡35块" / "前天收到工资5000" / "上周五打车28元" 等
 */
public class NaturalLangParser {

    public static class Result {
        public boolean ok;
        public int type = Transaction.TYPE_EXPENSE;
        public double amount;
        public long dateTime = System.currentTimeMillis();
        public String categoryHint; // 分类提示（可能为 null）
        public String channelHint;  // 渠道提示（可能为 null）
        public String noteHint;     // 备注提示（买了什么，可能为 null）
    }

    public static Result parse(String input) {
        Result r = new Result();
        if (input == null || input.trim().isEmpty()) return r;

        Double amount = extractAmount(input);
        if (amount == null || amount <= 0) return r;
        r.amount = amount;

        r.type = extractType(input);
        r.dateTime = extractTime(input);
        r.categoryHint = extractCategory(input, r.type);
        r.channelHint = extractChannel(input);
        r.noteHint = extractNote(input);
        r.ok = true;
        return r;
    }

    /** 判断并提取待办/笔记中的具体时间，无时间返回 null */
    public static Long parseTimeHint(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        boolean hasTime = s.contains("今天") || s.contains("明天") || s.contains("后天")
                || s.contains("大后天") || s.contains("昨天") || s.contains("前天")
                || s.contains("大前天") || s.contains("天前") || s.contains("天后")
                || s.contains("月") || s.contains("点") || s.contains("时")
                || s.contains("号") || s.contains("日");
        if (!hasTime) return null;
        return extractTime(s);
    }

    /** 提取金额：元/块/¥￥ 或 纯数字 */
    private static Double extractAmount(String s) {
        Matcher m1 = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)\\s*[元块]").matcher(s);
        if (m1.find()) return parseNum(m1.group(1));
        Matcher m2 = Pattern.compile("[¥￥]\\s*(\\d+(?:\\.\\d{1,2})?)").matcher(s);
        if (m2.find()) return parseNum(m2.group(1));
        Matcher m3 = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)").matcher(s);
        if (m3.find()) return parseNum(m3.group(1));
        return null;
    }

    private static Double parseNum(String s) {
        try {
            return Double.parseDouble(s.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 判断收支方向：先收入，后支出 */
    private static int extractType(String s) {
        String[] income = {"收到", "收入", "到账", "赚", "工资", "退款", "入账", "红包", "报销", "转入"};
        for (String k : income) {
            if (s.contains(k)) return Transaction.TYPE_INCOME;
        }
        return Transaction.TYPE_EXPENSE;
    }

    /** 提取时间：昨天/前天/N天前/N天后/明天/后天/X月X日 + 上午/下午X点X分，默认今天当前时间 */
    private static long extractTime(String s) {
        Calendar c = Calendar.getInstance();

        // 1. 相对日期
        if (s.contains("大后天")) {
            c.add(Calendar.DAY_OF_MONTH, 3);
        } else if (s.contains("后天")) {
            c.add(Calendar.DAY_OF_MONTH, 2);
        } else if (s.contains("明天")) {
            c.add(Calendar.DAY_OF_MONTH, 1);
        } else if (s.contains("大前天")) {
            c.add(Calendar.DAY_OF_MONTH, -3);
        } else if (s.contains("前天")) {
            c.add(Calendar.DAY_OF_MONTH, -2);
        } else if (s.contains("昨天")) {
            c.add(Calendar.DAY_OF_MONTH, -1);
        }

        // 2. N天前 / N天后
        Matcher m = Pattern.compile("(\\d+)天(前|后)").matcher(s);
        if (m.find()) {
            int n = Integer.parseInt(m.group(1));
            if ("前".equals(m.group(2))) n = -n;
            c.add(Calendar.DAY_OF_MONTH, n);
            return c.getTimeInMillis();
        }

        // 3. X月X日
        Matcher md = Pattern.compile("(\\d{1,2})月(\\d{1,2})[日号]").matcher(s);
        boolean hasDate = md.find();
        if (hasDate) {
            c.set(Calendar.MONTH, Integer.parseInt(md.group(1)) - 1);
            c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(md.group(2)));
        }

        // 4. 时间点：上午/下午/晚上/中午/凌晨 X点X分 / X:XX
        Matcher mt = Pattern.compile("(上午|下午|晚上|中午|凌晨)?(\\d{1,2})[点:：](\\d{1,2})?分?").matcher(s);
        if (mt.find()) {
            int hour = Integer.parseInt(mt.group(2));
            String period = mt.group(1);
            int minute = mt.group(3) != null ? Integer.parseInt(mt.group(3)) : 0;
            if ("下午".equals(period) || "晚上".equals(period)) {
                if (hour < 12) hour += 12;
            } else if ("中午".equals(period)) {
                if (hour <= 2) hour += 12;
            }
            c.set(Calendar.HOUR_OF_DAY, hour);
            c.set(Calendar.MINUTE, minute);
        } else if (hasDate) {
            c.set(Calendar.HOUR_OF_DAY, 12);
            c.set(Calendar.MINUTE, 0);
        }
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** 提取分类提示（区分支出/收入） */
    private static String extractCategory(String s, int type) {
        if (type == Transaction.TYPE_INCOME) {
            String[][] map = {
                    {"工资", "薪水", "薪资", "月薪"}, // 工资
                    {"奖金", "年终奖", "绩效"}, // 奖金
                    {"理财", "利息", "收益", "基金", "股票", "分红"}, // 理财
                    {"兼职", "副业", "外快"}, // 兼职
                    {"红包"}, // 红包
            };
            String[] names = {"工资", "奖金", "理财", "兼职", "红包"};
            for (int i = 0; i < map.length; i++) {
                for (String k : map[i]) {
                    if (s.contains(k)) return names[i];
                }
            }
            return null;
        }

        String[][] map = {
                {"咖啡", "奶茶", "可乐", "果汁", "奶茶", "饮料", "水", "啤酒", "白酒", "红酒",
                 "沙拉", "汉堡", "薯条", "甜品", "蛋糕", "面包", "饼干", "巧克力",
                 "吃饭", "早餐", "午餐", "晚餐", "宵夜", "夜宵", "外卖", "快餐",
                 "火锅", "烧烤", "烤肉", "聚餐", "餐厅", "饭店", "食堂", "面馆"}, // 餐饮
                {"打车", "出租车", "滴滴", "网约车", "地铁", "公交", "公交车", "火车",
                 "高铁", "动车", "机票", "航班", "飞机", "加油", "油费", "停车", "停车费",
                 "过路费", "高速", "ETC", "摩拜", "单车", "哈啰", "青桔"}, // 交通
                {"衣服", "裤子", "裙子", "鞋子", "袜子", "包包", "包", "帽子",
                 "化妆品", "护肤品", "面膜", "口红", "香水",
                 "淘宝", "京东", "拼多多", "网购", "网上", "超市", "便利店",
                 "屈臣氏", "优衣库", "ZARA", "商场", "百货", "文具"}, // 购物
                {"电影", "影院", "KTV", "唱歌", "密室", "剧本杀", "桌游",
                 "演唱会", "漫展", "展览", "博物馆", "景点", "游乐场", "迪士尼",
                 "游戏", "手游", "端游", "Steam"}, // 娱乐
                {"房租", "水电", "水费", "电费", "燃气", "燃气费", "物业", "物业费",
                 "宽带", "网费", "暖气", "取暖费", "房贷"}, // 住房
                {"药", "买药", "医院", "看病", "门诊", "体检", "挂号", "手术", "诊所"}, // 医疗
                {"学费", "书", "书本", "培训", "课程", "辅导", "补习", "文具"}, // 教育
                {"话费", "流量", "宽带", "手机费"}, // 通讯
                {"买菜", "超市", "日用", "零食", "水果", "蔬菜", "牛奶", "饮料"} // 日用
        };
        String[] names = {"餐饮", "交通", "购物", "娱乐", "住房", "医疗", "教育", "通讯", "日用"};
        for (int i = 0; i < map.length; i++) {
            for (String k : map[i]) {
                if (s.contains(k)) return names[i];
            }
        }
        return null;
    }

    /** 提取渠道提示：微信/支付宝/银行卡/现金 */
    private static String extractChannel(String s) {
        if (s.contains("微信")) return "微信";
        if (s.contains("支付宝")) return "支付宝";
        if (s.contains("银行") || s.contains("刷卡") || s.contains("储蓄卡") || s.contains("信用卡")) return "银行卡";
        if (s.contains("现金") || s.contains("现钞")) return "现金";
        return null;
    }

    /** 提取备注：AI 文字优化（保留核心物品名，去除噪声） */
    private static String extractNote(String s) {
        String t = s;
        // 1. 金额
        t = t.replaceAll("[¥￥]\\s*\\d[\\d,]*(?:\\.\\d{1,2})?", " ");
        t = t.replaceAll("\\d[\\d,]*(?:\\.\\d{1,2})?\\s*[元块]", " ");
        // 2. 时间
        t = stripTime(t);
        // 3. 渠道
        t = t.replaceAll("微信|支付宝|银行卡|储蓄卡|信用卡|现金|现钞|刷卡|花呗|白条", " ");
        // 4. 收支方向动词
        t = t.replaceAll("收到|收入|到账|赚|退款|入账|报销|转入|转出|收钱|收益|返现", " ");
        t = t.replaceAll("支付|消费|付款|扣款|支出|购买|已支付|交易成功|买单|付款成功|扣费|下单|成交", " ");
        // 5. 范围动词 + 助词
        t = t.replaceAll("买了|卖了|花了|用了|卖|买|花的|用的|吃的|喝的|吃的|喝的", " ");
        // 6. 停用词/助词
        t = t.replaceAll("的|了|在|给|从|到|去|和|与|着|过|把|被|让|请", " ");
        // 7. 残留动词（避免"喝咖啡"→"咖啡"）
        t = t.replaceAll("喝|吃|打|坐|乘|去|来|弄|做|玩|看|逛|逛逛", " ");
        // 8. 残留量词（"一杯咖啡"→"咖啡"）
        t = t.replaceAll("一杯|一份|一个|一张|一件|一瓶|一包|一盒|一袋|一碗|一盘|一顿|一次|一趟|了杯|杯|份|个|张|件|瓶|包|盒|袋|碗|盘|顿|次|趟", " ");
        // 9. 清理空白
        String result = t.trim().replaceAll("\\s+", " ");
        if (result.isEmpty()) return null;
        // 长度限制（核心物品名）
        if (result.length() > 20) result = result.substring(0, 20).trim();
        return result;
    }

    /** 笔记/待办自然语言解析结果 */
    public static class NoteResult {
        public Long time;   // 提取到的时间（可能为 null）
        public String text; // 去掉时间词后的剩余文本
    }

    /** 解析笔记/待办自然语言：提取时间 + 剥离时间词后的文本 */
    public static NoteResult parseNote(String s) {
        NoteResult r = new NoteResult();
        if (s == null || s.trim().isEmpty()) {
            r.text = "";
            return r;
        }
        r.time = parseTimeHint(s);
        r.text = stripTime(s);
        return r;
    }

    /** 去掉文本中的时间描述，留下核心内容 */
    private static String stripTime(String s) {
        String t = s;
        t = t.replaceAll("大后天|大前天", " ");
        t = t.replaceAll("后天|前天|昨天|明天|今天", " ");
        t = t.replaceAll("\\d+天前|\\d+天后", " ");
        t = t.replaceAll("\\d{1,2}月\\d{1,2}[日号]", " ");
        t = t.replaceAll("上午|下午|晚上|中午|凌晨", " ");
        t = t.replaceAll("\\d{1,2}[点:：]\\d{0,2}分?", " ");
        return t.trim().replaceAll("\\s+", " ");
    }
}
