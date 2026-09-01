package com.example.jizhang.fragment;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jizhang.NoteEditActivity;
import com.example.jizhang.R;
import com.example.jizhang.adapter.NoteAdapter;
import com.example.jizhang.db.DatabaseHelper;
import com.example.jizhang.model.Note;
import com.example.jizhang.service.AlarmReceiver;
import com.example.jizhang.util.NaturalLangParser;
import com.example.jizhang.util.Palette;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * 笔记 / 备忘录页
 */
public class NoteFragment extends Fragment {

    public static final String EXTRA_ID = "extra_id";

    private DatabaseHelper db;
    private NoteAdapter adapter;
    private TextView tvEmpty;
    private TextView tabNote, tabTodo;
    private int currentTab = 0; // 0=笔记, 1=待办

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_note, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new DatabaseHelper(requireContext());
        tvEmpty = view.findViewById(R.id.tv_empty);
        tabNote = view.findViewById(R.id.tab_note);
        tabTodo = view.findViewById(R.id.tab_todo);
        tabNote.setOnClickListener(v -> switchTab(0));
        tabTodo.setOnClickListener(v -> switchTab(1));

        RecyclerView recyclerView = view.findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NoteAdapter();
        adapter.setListener(new NoteAdapter.Listener() {
            @Override
            public void onClick(Note n) {
                Intent intent = new Intent(requireContext(), NoteEditActivity.class);
                intent.putExtra(NoteEditActivity.EXTRA_ID, n.id);
                startActivity(intent);
            }

            @Override
            public void onLongClick(Note n) {
                Long t = NaturalLangParser.parseTimeHint(n.title + " " + n.content);
                if (n.todo > 0 && t != null) {
                    showNoteMenu(n, t);
                } else {
                    confirmDelete(n);
                }
            }

            @Override
            public void onTodoToggle(Note n) {
                n.todo = n.todo == 2 ? 1 : 2;
                n.updateTime = System.currentTimeMillis();
                db.updateNote(n);
                reload();
            }
        });
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = view.findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), NoteEditActivity.class);
            intent.putExtra(NoteEditActivity.EXTRA_TODO, currentTab); // 0=笔记, 1=待办
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    private void switchTab(int tab) {
        currentTab = tab;
        boolean noteTab = tab == 0;
        tabNote.setTextColor(noteTab ? 0xFF4DB6AC : 0xFF888888);
        tabNote.setTextSize(noteTab ? 15 : 14);
        tabTodo.setTextColor(noteTab ? 0xFF888888 : 0xFF4DB6AC);
        tabTodo.setTextSize(noteTab ? 14 : 15);
        reload();
    }

    private void reload() {
        List<Note> list = db.queryNotesByTodo(currentTab);
        adapter.setItems(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirmDelete(Note n) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除笔记")
                .setMessage("确定删除「" + (n.title == null ? "无标题" : n.title) + "」吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    db.deleteNote(n.id);
                    reload();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showNoteMenu(Note n, long time) {
        String[] items = {"删除", "设置闹钟提醒（" + Palette.formatDateTime(time) + "）"};
        new AlertDialog.Builder(requireContext())
                .setTitle(n.title)
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        confirmDelete(n);
                    } else {
                        setAlarm(n, time);
                    }
                })
                .show();
    }

    private void setAlarm(Note n, long time) {
        if (time <= System.currentTimeMillis()) {
            Toast.makeText(requireContext(), "该时间已过，请先修改待办时间", Toast.LENGTH_SHORT).show();
            return;
        }
        AlarmManager am = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            Toast.makeText(requireContext(), "请在系统设置中允许「闹钟和提醒」权限", Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
            } catch (Exception ignored) {
            }
            return;
        }
        Intent intent = new Intent(requireContext(), AlarmReceiver.class);
        intent.putExtra("title", n.title);
        PendingIntent pi = PendingIntent.getBroadcast(requireContext(), (int) n.id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
        Toast.makeText(requireContext(), "已设置闹钟：" + Palette.formatDateTime(time), Toast.LENGTH_SHORT).show();
    }
}