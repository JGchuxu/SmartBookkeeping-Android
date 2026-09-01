package com.example.jizhang.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Window;

import com.example.jizhang.R;

/**
 * 主题管理器：管理 3 套主题（蓝紫星空/粉紫梦幻/薄荷青绿）× 深浅双模式 × 字体，
 * 提供全屏渐变背景、主题色值，并用 SharedPreferences 持久化。
 */
public class ThemeManager {

    // 主题 ID
    public static final int THEME_BLUE_PURPLE = 0; // 蓝紫星空（默认主打）
    public static final int THEME_PINK_PURPLE = 1; // 粉紫梦幻
    public static final int THEME_MINT = 2;        // 薄荷青绿
    public static final int THEME_CUSTOM = 3;      // 自定义（外部导入）
    // 菲比主题（4-12 对应 9 张表情包）
    public static final int THEME_PHEBE_SLEEP = 4;
    public static final int THEME_PHEBE_GLOW = 5;
    public static final int THEME_PHEBE_SHY = 6;
    public static final int THEME_PHEBE_SPEECHLESS = 7;
    public static final int THEME_PHEBE_SOFT = 8;
    public static final int THEME_PHEBE_MILD = 9;
    public static final int THEME_PHEBE_PROUD = 10;
    public static final int THEME_PHEBE_CONFUSED = 11;
    public static final int THEME_PHEBE_SILLY = 12;

    public static final String[] THEME_NAMES = {
            "薄荷青绿", "粉紫梦幻", "蓝紫星空", "自定义",
            "菲比·躲被窝", "菲比·发光", "菲比·害羞", "菲比·无语",
            "菲比·温柔", "菲比·海风温和", "菲比·厉害吧", "菲比·疑惑", "菲比·哎呀"
    };

    /** 菲比主题对应的 drawable 资源名（用于顶部装饰插图） */
    public static final String[] PHEBE_IMAGE_NAMES = {
            "phebe_sleep", "phebe_glow", "phebe_shy", "phebe_speechless",
            "phebe_soft", "phebe_mild", "phebe_proud", "phebe_confused", "phebe_silly"
    };

    // 字体 ID（res/font 下的自定义字体 + 默认系统字体，全部 TTF 保证兼容）
    public static final int FONT_DEFAULT = 0;
    public static final int FONT_NAILAO = 1;
    public static final int FONT_HUANGYOU = 2;
    public static final int FONT_GAODUANHEI = 3;
    public static final int FONT_XIAOKAI = 4;
    public static final int FONT_XINGSHU = 5;
    public static final int FONT_XIAHANGKAI = 6;
    public static final int FONT_WENYITI = 7;
    public static final int FONT_KUAILETI = 8;
    public static final int FONT_SHUYUANTI = 9;
    public static final int FONT_DAOLITI = 10;

    public static final String[] FONT_NAMES = {
            "默认", "小可奶酪体", "黄油体", "高端黑", "悠然小楷", "静龙行书",
            "夏行楷", "文艺体", "快乐体", "舒圆体", "刀隶体"
    };

    // 红涨绿跌（中国习惯）
    public static final int COLOR_EXPENSE = 0xFFFF5A5A; // 支出红
    public static final int COLOR_INCOME = 0xFF43C478;  // 收入绿

    private static final String PREFS = "theme_prefs";
    private static final String KEY_THEME = "theme_id";
    private static final String KEY_DARK = "dark_mode";
    private static final String KEY_FONT = "font_id";
    private static final String KEY_CUSTOM_PRIMARY = "custom_primary";
    private static final String KEY_CUSTOM_START = "custom_start";
    private static final String KEY_CUSTOM_END = "custom_end";

    private static ThemeManager instance;
    private final SharedPreferences prefs;

    /** 一套主题的色值集合 */
    public static class Profile {
        public int primary;       // 主色（按钮/强调）
        public int gradientStart; // 渐变起点（左上）
        public int gradientEnd;   // 渐变终点（右下）
        public int card;          // 卡片背景
        public int textPrimary;   // 主文字
        public int textSecondary; // 次文字
    }

    private ThemeManager(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static ThemeManager get(Context ctx) {
        if (instance == null) instance = new ThemeManager(ctx);
        return instance;
    }

    public int getThemeId() {
        int id = prefs.getInt(KEY_THEME, THEME_BLUE_PURPLE);
        // 越界保护：旧数据可能超出当前主题数组长度
        if (id < 0 || id >= THEME_NAMES.length) return THEME_BLUE_PURPLE;
        return id;
    }

    public boolean isDark() {
        return prefs.getBoolean(KEY_DARK, false);
    }

    public void setTheme(int id) {
        prefs.edit().putInt(KEY_THEME, id).apply();
    }

    public void setDark(boolean dark) {
        prefs.edit().putBoolean(KEY_DARK, dark).apply();
    }

    public int getFontId() {
        int id = prefs.getInt(KEY_FONT, FONT_DEFAULT);
        // 越界保护：旧数据（如之前选的字体 ID=8）可能超出当前字体数组长度
        if (id < 0 || id >= FONT_NAMES.length) return FONT_DEFAULT;
        return id;
    }

    public void setFont(int id) {
        prefs.edit().putInt(KEY_FONT, id).apply();
    }

    /** 保存自定义主题（外部导入）：primary/gradientStart/gradientEnd 三个色值 */
    public void setCustomTheme(int primary, int gradientStart, int gradientEnd) {
        prefs.edit()
                .putInt(KEY_CUSTOM_PRIMARY, primary)
                .putInt(KEY_CUSTOM_START, gradientStart)
                .putInt(KEY_CUSTOM_END, gradientEnd)
                .putInt(KEY_THEME, THEME_CUSTOM)
                .apply();
    }

    /** 按字体 ID 返回对应的主题资源（在 super.onCreate 前调用 setTheme） */
    public int getThemeResId() {
        switch (getFontId()) {
            case FONT_NAILAO: return R.style.Theme_Jizhang_Nailao;
            case FONT_HUANGYOU: return R.style.Theme_Jizhang_Huangyou;
            case FONT_GAODUANHEI: return R.style.Theme_Jizhang_Gaoduanhei;
            case FONT_XIAOKAI: return R.style.Theme_Jizhang_Xiaokai;
            case FONT_XINGSHU: return R.style.Theme_Jizhang_Xingshu;
            case FONT_XIAHANGKAI: return R.style.Theme_Jizhang_Xiahangkai;
            case FONT_WENYITI: return R.style.Theme_Jizhang_Wenyiti;
            case FONT_KUAILETI: return R.style.Theme_Jizhang_Kuaileti;
            case FONT_SHUYUANTI: return R.style.Theme_Jizhang_Shuyuanti;
            case FONT_DAOLITI: return R.style.Theme_Jizhang_Daoliti;
            default: return R.style.Theme_Jizhang;
        }
    }

    /** 当前主题对应的顶部装饰插图（菲比主题返回对应 drawable；其他主题返回 0） */
    public int getThemeImageResId(Context ctx) {
        int id = getThemeId();
        if (id < THEME_PHEBE_SLEEP || id > THEME_PHEBE_SILLY) return 0;
        String name = PHEBE_IMAGE_NAMES[id - THEME_PHEBE_SLEEP];
        return ctx.getResources().getIdentifier(name, "drawable", ctx.getPackageName());
    }

    /** 当前主题的色值 */
    public Profile current() {
        return profile(getThemeId(), isDark());
    }

    /** 按主题 ID + 深浅模式取色值 */
    public Profile profile(int themeId, boolean dark) {
        Profile p = new Profile();
        switch (themeId) {
            case THEME_PINK_PURPLE:
                if (dark) {
                    p.primary = 0xFFF48FB1; p.gradientStart = 0xFF301A3E; p.gradientEnd = 0xFF6E2E5A;
                    p.card = 0xFF3A2448; p.textPrimary = 0xFFFFFFFF; p.textSecondary = 0xFFC8A0B8;
                } else {
                    p.primary = 0xFFEC6CA8; p.gradientStart = 0xFFFFE8F0; p.gradientEnd = 0xFFF3E0FF;
                    p.card = 0xFFFFFFFF; p.textPrimary = 0xFF3A2A33; p.textSecondary = 0xFFA08890;
                }
                break;
            case THEME_MINT:
                if (dark) {
                    p.primary = 0xFF4DB6AC; p.gradientStart = 0xFF12302E; p.gradientEnd = 0xFF1E4A44;
                    p.card = 0xFF1E3A38; p.textPrimary = 0xFFFFFFFF; p.textSecondary = 0xFFA0C0BC;
                } else {
                    p.primary = 0xFF26A69A; p.gradientStart = 0xFFE0F7F4; p.gradientEnd = 0xFFE8F5E9;
                    p.card = 0xFFFFFFFF; p.textPrimary = 0xFF2E3A38; p.textSecondary = 0xFF88A09C;
                }
                break;
            case THEME_CUSTOM: {
                int primary = prefs.getInt(KEY_CUSTOM_PRIMARY, 0xFF4DB6AC);
                int start = prefs.getInt(KEY_CUSTOM_START, 0xFFE0F7F4);
                int end = prefs.getInt(KEY_CUSTOM_END, 0xFFC8E6C9);
                p.primary = primary;
                p.gradientStart = start;
                p.gradientEnd = end;
                if (dark) {
                    p.card = 0xFF2A2B4E; p.textPrimary = 0xFFFFFFFF; p.textSecondary = 0xFFB0B0D0;
                } else {
                    p.card = 0xFFFFFFFF; p.textPrimary = 0xFF333344; p.textSecondary = 0xFF8888A0;
                }
                break;
            }
            case THEME_PHEBE_SLEEP:
            case THEME_PHEBE_GLOW:
            case THEME_PHEBE_SHY:
            case THEME_PHEBE_SPEECHLESS:
            case THEME_PHEBE_SOFT:
            case THEME_PHEBE_MILD:
            case THEME_PHEBE_PROUD:
            case THEME_PHEBE_CONFUSED:
            case THEME_PHEBE_SILLY:
                // 菲比主题：统一浅色清新（呱呱记账风格），顶部加菲比插图
                p.primary = 0xFF4DB6AC; p.gradientStart = 0xFFE0F7F4; p.gradientEnd = 0xFFC8E6C9;
                p.card = 0xFFFFFFFF; p.textPrimary = 0xFF2E3A38; p.textSecondary = 0xFF88A09C;
                break;
            case THEME_BLUE_PURPLE:
            default:
                if (dark) {
                    p.primary = 0xFF26A69A; p.gradientStart = 0xFFB2DFDB; p.gradientEnd = 0xFF80CBC4;
                    p.card = 0xFFE0F2EF; p.textPrimary = 0xFFFFFFFF; p.textSecondary = 0xFFCAEEE6;
                } else {
                    // 默认：薄荷青绿浅色（呱呱记账风格）
                    p.primary = 0xFF4DB6AC; p.gradientStart = 0xFFE0F7F4; p.gradientEnd = 0xFFC8E6C9;
                    p.card = 0xFFFFFFFF; p.textPrimary = 0xFF2E3A38; p.textSecondary = 0xFF88A09C;
                }
                break;
        }
        return p;
    }

    /** 创建全屏渐变背景（左上→右下） */
    public GradientDrawable createBackgroundGradient() {
        Profile p = current();
        return new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{p.gradientStart, p.gradientEnd});
    }

    /** 应用主题到 Activity：状态栏色 + 窗口背景渐变 */
    public void apply(Activity activity) {
        Profile p = current();
        Window w = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            w.setStatusBarColor(p.gradientStart);
        }
        w.setBackgroundDrawable(createBackgroundGradient());
    }
}
