package com.example.jizhang.util;

import com.example.jizhang.model.Transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通知内容解析器：从各 App 通知中提取金额、收支方向、退款
 */
public class BillParser {

    public static final String SOURCE_WECHAT = "微信";
    public static final String SOURCE_ALIPAY = "支付宝";

    /** 包名 → 来源名 映射 */
    private static final Map<String, String> SOURCE_MAP = new HashMap<>();

    static {
        // 仅识别微信、支付宝的收支通知，避免其他软件对同一笔账单重复发通知
        SOURCE_MAP.put("com.tencent.mm", "微信");
        SOURCE_MAP.put("com.eg.android.AlipayGphone", "支付宝");
    }

    public static class Result {
        public int type;      // Transaction.TYPE_EXPENSE / TYPE_INCOME
        public double amount;
        public String source; // 微信 / 支付宝 / 美团 ...
        public boolean refund; // 是否为退款
    }

    /** 判断是否支持识别该软件的消费通知 */
    public static boolean isSupported(String packageName) {
        return packageName != null && SOURCE_MAP.containsKey(packageName);
    }

    /** 根据包名获取来源名（微信/支付宝），不支持返回 null */
    public static String getSource(String packageName) {
        return SOURCE_MAP.get(packageName);
    }

    /**
     * 判断内容是否疑似收支（含收支关键词），用于无法精确识别金额/收支方向时加入待办。
     * 这样既能兜住"分不清金额或收支"的通知，又不会把纯营销/无关通知塞进待办。
     */
    public static boolean isSuspectedBill(String content) {
        if (content == null || content.trim().isEmpty()) return false;
        String[] keys = {
                "支付", "消费", "付款", "扣款", "支出", "购买", "已支付", "交易成功",
                "买单", "付款成功", "扣费", "消费支出", "消费成功", "下单", "订单", "成交",
                "到账", "收款", "收到", "收入", "存入", "转入", "入账", "进账", "收钱",
                "来账", "收款成功", "收益", "返现", "退款", "退款成功", "已退款", "原路退回",
                "退回", "退费", "返还", "转账", "红包", "账单", "余额", "金额", "元", "¥", "￥"
        };
        for (String k : keys) {
            if (content.contains(k)) return true;
        }
        return false;
    }

    /**
     * 判断内容是否为广告/营销通知（用于过滤，避免广告被误识别为账单或加入待办）。
     * 只匹配明确的广告/促销词，避免误伤真实收款（如"红包"、"转账"单独出现不算广告）。
     */
    public static boolean isAdvertisement(String content) {
        if (content == null || content.trim().isEmpty()) return false;
        String[] adKeys = {
                // 促销/优惠
                "优惠", "促销", "折扣", "满减", "秒杀", "抢购", "特价", "限时", "大促", "狂欢",
                "清仓", "打折", "包邮", "券后", "领券", "优惠券", "券包", "省钱", "钜惠", "惠购",
                "史低", "低价", "爆款", "热卖", "热销", "囤货", "补货",
                // 活动/福利
                "活动", "抽奖", "福利", "礼包", "红包雨", "免费领", "立减", "返利", "补贴",
                "邀请", "砍价", "拼团", "团购", "新品", "上新", "预售", "首发", "尝鲜",
                // 广告/推荐
                "广告", "推广", "推荐", "精选", "专享", "尊享", "会员日", "新人礼", "首单",
                // 营销引导
                "限时抢", "手慢无", "错过", "最后一波", "福利来", "点此", "点击领取",
                "立即抢", "马上抢", "领红包", "开红包", "拆红包", "抢红包", "瓜分",
                // 电商大促节日
                "双十一", "双11", "618", "双12", "年中大促", "年货节", "女王节", "女神节",
                "超级品牌日", "聚划算", "百亿补贴", "秒杀日", "狂欢节", "购物节",
                // 直播/种草
                "直播间", "主播", "带货", "种草", "安利", "爆款",
                // 低价诱导
                "0元", "1元", "9.9", "免费送", "领奖", "中奖", "签到领", "新人专享",
                "限时秒杀", "限时免费", "限时立减", "限量", "名额有限", "先到先得",
                "全场满", "券后价", "到手价", "仅需", "只要", "超值", "划算", "捡漏"
        };
        for (String k : adKeys) {
            if (content.contains(k)) return true;
        }
        return false;
    }

    /** 解析通知，无法识别返回 null */
    public static Result parse(String packageName, String title, String text) {
        String source = SOURCE_MAP.get(packageName);
        if (source == null) return null;

        String content = (title == null ? "" : title) + " " + (text == null ? "" : text);

        boolean refund = isRefund(content);

        // 退款：归为收入（钱回到账户）
        if (refund) {
            Double amount = extractAmount(content);
            if (amount == null || amount <= 0) return null;
            Result r = new Result();
            r.source = source;
            r.amount = amount;
            r.type = Transaction.TYPE_INCOME;
            r.refund = true;
            return r;
        }

        // 正常收支
        Integer type = extractType(content);
        if (type == null) return null;
        Double amount = extractAmount(content);
        if (amount == null || amount <= 0) return null;

        Result r = new Result();
        r.source = source;
        r.amount = amount;
        r.type = type;
        r.refund = false;
        return r;
    }

    /** 是否为退款通知 */
    private static boolean isRefund(String s) {
        String[] refund = {"退款", "退款成功", "已退款", "原路退回", "退回", "退费", "返还", "退款到账", "退款已完成"};
        for (String k : refund) {
            if (s.contains(k)) return true;
        }
        return false;
    }

    /** 提取金额：¥/￥ 符号 → 元后缀 → 纯数字（带小数优先） */
    private static Double extractAmount(String s) {
        Matcher m1 = Pattern.compile("[¥￥]\\s*(\\d[\\d,]*(?:\\.\\d{1,2})?)").matcher(s);
        if (m1.find()) return parseNum(m1.group(1));

        Matcher m2 = Pattern.compile("(\\d[\\d,]*(?:\\.\\d{1,2})?)\\s*元").matcher(s);
        if (m2.find()) return parseNum(m2.group(1));

        Matcher m3 = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)").matcher(s);
        while (m3.find()) {
            String g = m3.group(1);
            if (g.contains(".")) return parseNum(g);
        }
        Matcher m4 = Pattern.compile("(\\d+)").matcher(s);
        if (m4.find()) return parseNum(m4.group(1));
        return null;
    }

    private static Double parseNum(String s) {
        try {
            return Double.parseDouble(s.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 判断收支方向：先查收入关键词，再查支出关键词 */
    private static Integer extractType(String s) {
        String[] income = {"到账", "收款", "收到", "收入", "存入", "转入", "入账", "进账", "收钱", "来账", "收款成功", "收益", "返现"};
        for (String k : income) {
            if (s.contains(k)) return Transaction.TYPE_INCOME;
        }
        String[] expense = {"支付", "消费", "付款", "扣款", "支出", "购买", "已支付", "交易成功",
                "买单", "付款成功", "扣费", "转出", "消费支出", "消费成功", "下单", "订单", "成交"};
        for (String k : expense) {
            if (s.contains(k)) return Transaction.TYPE_EXPENSE;
        }
        return null;
    }
}
