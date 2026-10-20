package com.example.jizhang;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import com.example.jizhang.BaseActivity;
import androidx.fragment.app.Fragment;

import com.example.jizhang.fragment.BillFragment;
import com.example.jizhang.fragment.NoteFragment;
import com.example.jizhang.fragment.SettingsFragment;
import com.example.jizhang.fragment.StatsFragment;
import com.example.jizhang.util.ThemeManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * 主界面：底部导航切换 账单 / 统计 / 设置
 */
public class MainActivity extends BaseActivity {

    private final Fragment billFragment = new BillFragment();
    private final Fragment statsFragment = new StatsFragment();
    private final Fragment noteFragment = new NoteFragment();
    private final Fragment settingsFragment = new SettingsFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.get(this).getThemeResId());
        super.onCreate(savedInstanceState);
        ThemeManager.get(this).apply(this);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        ThemeManager.Profile profile = ThemeManager.get(this).current();
        bottomNav.setBackgroundColor(profile.card);
        bottomNav.setItemIconTintList(null); // 图标用原始色，后续换萌系图标
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_bill) {
                switchFragment(billFragment);
                return true;
            } else if (id == R.id.nav_overview) {
                startActivity(new Intent(this, FinanceOverviewActivity.class));
                return false;
            } else if (id == R.id.nav_stats) {
                switchFragment(statsFragment);
                return true;
            } else if (id == R.id.nav_note) {
                switchFragment(noteFragment);
                return true;
            } else if (id == R.id.nav_settings) {
                switchFragment(settingsFragment);
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_bill);
        }

        // 主动识别引导：启动时自动检测通知使用权，未开启则弹窗一键引导
        checkListenerPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从系统设置页返回后复查：刚开启成功则提示已激活
        if (listenerGuidePending && isListenerEnabled()) {
            listenerGuidePending = false;
            android.widget.Toast.makeText(this,
                    "✅ 自动记账已激活，支付后无需任何操作", android.widget.Toast.LENGTH_LONG).show();
        }
    }

    /** 通知使用权是否已授予本 App */
    private boolean isListenerEnabled() {
        String pkg = getPackageName();
        String listeners = android.provider.Settings.Secure.getString(
                getContentResolver(), "enabled_notification_listeners");
        return listeners != null && listeners.contains(pkg);
    }

    /** 主动检测：未开启通知使用权时弹窗引导（点"暂不"后 7 天内不再打扰） */
    private void checkListenerPermission() {
        if (isListenerEnabled()) {
            return;
        }
        android.content.SharedPreferences sp =
                getSharedPreferences("app_config", MODE_PRIVATE);
        long lastSkip = sp.getLong("listener_guide_skip_at", 0);
        if (System.currentTimeMillis() - lastSkip < 7L * 24 * 3600 * 1000) {
            return;
        }
        listenerGuidePending = true;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🔔 开启自动记账")
                .setMessage("开启「通知使用权」后，微信/支付宝的收支会自动记入账本，"
                        + "支付完成即入账，全程无需手动操作。\n\n"
                        + "· 本 App 不联网，所有数据仅保存在本机\n"
                        + "· 也可稍后在 设置→通知使用权 中随时开启")
                .setPositiveButton("去开启", (d, w) -> {
                    listenerGuidePending = true;
                    startActivity(new Intent(
                            android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                })
                .setNegativeButton("暂不", (d, w) -> sp.edit()
                        .putLong("listener_guide_skip_at", System.currentTimeMillis()).apply())
                .show();
    }

    /** 标记：引导流程已发起（返回 App 后复查结果用） */
    private boolean listenerGuidePending = false;

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
