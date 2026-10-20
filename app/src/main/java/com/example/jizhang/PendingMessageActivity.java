package com.example.jizhang;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jizhang.db.DatabaseHelper;
import com.example.jizhang.model.Note;
import com.example.jizhang.util.BillParser;
import com.example.jizhang.util.Palette;
import com.example.jizhang.util.ThemeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 待处理消息：通知识别失败后转入待办的通知列表
 * <p>
 * 每条可「去记账」（预填金额/备注）或「忽略」删除；处理完列表自动刷新。
 */
public class PendingMessageActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private MsgAdapter adapter;
    private TextView tvCount;
    private View emptyState;
    private final List<Note> messages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.get(this).getThemeResId());
        super.onCreate(savedInstanceState);
        ThemeManager.get(this).apply(this);
        setContentView(R.layout.activity_pending_msg);

        db = new DatabaseHelper(this);
        tvCount = findViewById(R.id.tv_count);
        emptyState = findViewById(R.id.empty_state);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MsgAdapter();
        recycler.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    /** 重新加载：todo=1 且标题含"待识别"（区别于用户自建待办） */
    private void reload() {
        messages.clear();
        for (Note n : db.queryNotesByTodo(1)) {
            if (n.title != null && n.title.contains("待识别")) {
                messages.add(n);
            }
        }
        adapter.notifyDataSetChanged();
        tvCount.setText(messages.size() + " 条");
        emptyState.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** 去记账：尽量预填金额与原文备注，同时把该消息从待办移除 */
    private void goBill(Note n) {
        Intent intent = new Intent(this, AddTransactionActivity.class);
        Double amount = BillParser.extractAmount(n.content);
        if (amount != null && amount > 0) {
            intent.putExtra(AddTransactionActivity.EXTRA_AMOUNT, amount);
        }
        intent.putExtra(AddTransactionActivity.EXTRA_NOTE, n.content);
        db.deleteNote(n.id);
        startActivity(intent);
    }

    private class MsgAdapter extends RecyclerView.Adapter<MsgHolder> {

        @NonNull
        @Override
        public MsgHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pending_msg, parent, false);
            return new MsgHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MsgHolder h, int position) {
            Note n = messages.get(position);
            h.tvSource.setText(n.title);
            h.tvContent.setText(n.content);
            h.tvTime.setText(Palette.formatTime(n.createTime));

            h.btnGo.setOnClickListener(v -> goBill(n));
            h.btnIgnore.setOnClickListener(v -> {
                db.deleteNote(n.id);
                Toast.makeText(v.getContext(), "已忽略", Toast.LENGTH_SHORT).show();
                reload();
            });
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }
    }

    static class MsgHolder extends RecyclerView.ViewHolder {

        final TextView tvSource, tvContent, tvTime, btnGo, btnIgnore;

        MsgHolder(@NonNull View itemView) {
            super(itemView);
            tvSource = itemView.findViewById(R.id.tv_source);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvTime = itemView.findViewById(R.id.tv_time);
            btnGo = itemView.findViewById(R.id.btn_go);
            btnIgnore = itemView.findViewById(R.id.btn_ignore);
        }
    }
}
