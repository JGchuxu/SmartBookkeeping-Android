package com.example.jizhang.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.jizhang.AIChatActivity;
import com.example.jizhang.CategoryManageActivity;
import com.example.jizhang.ChannelManageActivity;
import com.example.jizhang.NotifyLogActivity;
import com.example.jizhang.R;
import com.example.jizhang.db.DatabaseHelper;
import com.example.jizhang.util.Exporter;
import com.example.jizhang.util.ThemeManager;

import org.json.JSONObject;

/**
 * 设置页：渠道/分类管理 + 数据导出备份
 */
public class SettingsFragment extends Fragment {

    private DatabaseHelper db;
    private TextView tvNotifyStatus;
    private TextView tvThemeName, tvDarkStatus, tvFontName;
    private android.widget.ImageView ivThemePreview;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new DatabaseHelper(requireContext());

        view.findViewById(R.id.item_channel).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ChannelManageActivity.class)));
        view.findViewById(R.id.item_category).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CategoryManageActivity.class)));
        view.findViewById(R.id.item_export).setOnClickListener(v -> exportCsv());
        view.findViewById(R.id.item_backup).setOnClickListener(v -> backupDb());

        tvNotifyStatus = view.findViewById(R.id.tv_notify_status);
        view.findViewById(R.id.item_notify).setOnClickListener(v -> handleNotify());
        view.findViewById(R.id.item_notify_log).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), NotifyLogActivity.class)));
        view.findViewById(R.id.item_ai).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AIChatActivity.class)));

        // 主题换肤
        tvThemeName = view.findViewById(R.id.tv_theme_name);
        tvDarkStatus = view.findViewById(R.id.tv_dark_status);
        tvFontName = view.findViewById(R.id.tv_font_name);
        ivThemePreview = view.findViewById(R.id.iv_theme_preview);
        view.findViewById(R.id.item_theme).setOnClickListener(v -> showThemePicker());
        view.findViewById(R.id.item_dark).setOnClickListener(v -> toggleDark());
        view.findViewById(R.id.item_font).setOnClickListener(v -> showFontPicker());
        view.findViewById(R.id.item_theme_import).setOnClickListener(v -> showThemeImport());

        // 动态显示版本号
        try {
            String versionName = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            ((TextView) view.findViewById(R.id.tv_version)).setText("韷的小本本 v" + versionName);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateNotifyStatus();
        updateThemeLabel();
    }

    private void updateThemeLabel() {
        if (tvThemeName == null || tvDarkStatus == null) return;
        ThemeManager tm = ThemeManager.get(requireContext());
        tvThemeName.setText(ThemeManager.THEME_NAMES[tm.getThemeId()]);
        tvDarkStatus.setText(tm.isDark() ? "开" : "关");
        if (tvFontName != null) {
            tvFontName.setText(ThemeManager.FONT_NAMES[tm.getFontId()]);
        }
        // 主题插图（菲比主题显示对应表情包，其他主题隐藏）
        if (ivThemePreview != null) {
            int resId = tm.getThemeImageResId(requireContext());
            if (resId != 0) {
                ivThemePreview.setImageResource(resId);
                ivThemePreview.setVisibility(View.VISIBLE);
            } else {
                ivThemePreview.setVisibility(View.GONE);
            }
        }
    }

    private void showFontPicker() {
        ThemeManager tm = ThemeManager.get(requireContext());
        String[] names = ThemeManager.FONT_NAMES.clone();
        int current = tm.getFontId();
        names[current] = names[current] + "（当前）";
        new AlertDialog.Builder(requireContext())
                .setTitle("选择字体")
                .setItems(names, (d, which) -> {
                    if (which != current) {
                        tm.setFont(which);
                        requireActivity().recreate();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showThemeImport() {
        EditText input = new EditText(requireContext());
        input.setHint("粘贴 JSON：{\"primary\":\"#FF8B7CF6\",\"gradientStart\":\"#FF1A1B3A\",\"gradientEnd\":\"#FF3B2E6E\"}");
        input.setMinLines(3);
        new AlertDialog.Builder(requireContext())
                .setTitle("导入主题包")
                .setMessage("粘贴主题 JSON（primary 主色 / gradientStart 渐变起点 / gradientEnd 渐变终点）")
                .setView(input)
                .setPositiveButton("导入", (d, w) -> {
                    try {
                        JSONObject obj = new JSONObject(input.getText().toString().trim());
                        int primary = Color.parseColor(obj.getString("primary"));
                        int start = Color.parseColor(obj.getString("gradientStart"));
                        int end = Color.parseColor(obj.getString("gradientEnd"));
                        ThemeManager.get(requireContext()).setCustomTheme(primary, start, end);
                        requireActivity().recreate();
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "JSON 格式错误，请检查", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showThemePicker() {
        ThemeManager tm = ThemeManager.get(requireContext());
        String[] names = ThemeManager.THEME_NAMES.clone();
        int current = tm.getThemeId();
        names[current] = names[current] + "（当前）";
        new AlertDialog.Builder(requireContext())
                .setTitle("选择主题")
                .setItems(names, (d, which) -> {
                    if (which != current) {
                        tm.setTheme(which);
                        requireActivity().recreate();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void toggleDark() {
        ThemeManager tm = ThemeManager.get(requireContext());
        tm.setDark(!tm.isDark());
        requireActivity().recreate();
    }

    private boolean isNotificationEnabled() {
        String flat = Settings.Secure.getString(requireContext().getContentResolver(),
                "enabled_notification_listeners");
        return flat != null && flat.contains(requireContext().getPackageName());
    }

    private void updateNotifyStatus() {
        boolean enabled = isNotificationEnabled();
        tvNotifyStatus.setText(enabled ? "已开启" : "未开启");
        tvNotifyStatus.setTextColor(enabled ? 0xFF43C478 : 0xFF888888);
    }

    private void handleNotify() {
        if (isNotificationEnabled()) {
            Toast.makeText(requireContext(), "通知识别已开启，微信/支付宝消费通知会自动识别", Toast.LENGTH_SHORT).show();
        } else {
            new AlertDialog.Builder(requireContext())
                    .setTitle("开启通知识别")
                    .setMessage("需要授予「通知使用权」，才能自动识别微信/支付宝的消费通知。\n\n请在系统设置的「通知使用权」中开启「韷的小本本」。")
                    .setPositiveButton("去开启", (d, w) ->
                            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)))
                    .setNegativeButton("取消", null)
                    .show();
        }
    }

    private void exportCsv() {
        String path = Exporter.exportCsv(requireContext(), db);
        showResult("导出成功", "账单已导出为 CSV 文件：\n" + path, path == null);
    }

    private void backupDb() {
        String path = Exporter.backupDatabase(requireContext(), db);
        showResult("备份成功", "数据库已备份到：\n" + path, path == null);
    }

    private void showResult(String title, String message, boolean failed) {
        if (failed) {
            Toast.makeText(requireContext(), "操作失败，请重试", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("知道了", null)
                .show();
    }
}
