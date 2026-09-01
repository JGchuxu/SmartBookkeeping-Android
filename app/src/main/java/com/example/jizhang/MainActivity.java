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
    }

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
