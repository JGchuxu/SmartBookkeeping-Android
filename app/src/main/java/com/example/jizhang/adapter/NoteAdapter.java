package com.example.jizhang.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jizhang.R;
import com.example.jizhang.model.Note;
import com.example.jizhang.util.NaturalLangParser;
import com.example.jizhang.util.Palette;

import java.util.ArrayList;
import java.util.List;

/**
 * 笔记列表适配器
 */
public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.VH> {

    public interface Listener {
        void onClick(Note n);
        void onLongClick(Note n);
        void onTodoToggle(Note n);
    }

    private final List<Note> items = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener l) {
        this.listener = l;
    }

    public void setItems(List<Note> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Note n = items.get(position);
        String title = n.title == null || n.title.isEmpty() ? "（无标题）" : n.title;
        if (n.todo > 0) {
            Long t = NaturalLangParser.parseTimeHint(n.title + " " + n.content);
            if (t != null) {
                title = title + "  ·  " + Palette.formatDateTime(t);
            }
        }
        h.title.setText(title);
        h.content.setText(n.content == null ? "" : n.content);
        h.content.setVisibility(n.content == null || n.content.isEmpty() ? View.GONE : View.VISIBLE);
        h.time.setText(Palette.formatDateTime(n.updateTime));
        h.colorBar.setBackgroundColor(Palette.colorOf(n.colorIndex));

        if (n.todo == 0) {
            h.todo.setVisibility(View.GONE);
            h.title.setPaintFlags(h.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            h.todo.setVisibility(View.VISIBLE);
            h.todo.setText(n.todo == 2 ? "☑" : "☐");
            if (n.todo == 2) {
                h.title.setPaintFlags(h.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                h.title.setPaintFlags(h.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            }
        }
        h.todo.setOnClickListener(v -> {
            if (listener != null) listener.onTodoToggle(n);
        });

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(n);
        });
        h.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongClick(n);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, content, time, todo;
        View colorBar;

        VH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.tv_title);
            content = v.findViewById(R.id.tv_content);
            time = v.findViewById(R.id.tv_time);
            todo = v.findViewById(R.id.tv_todo);
            colorBar = v.findViewById(R.id.color_bar);
        }
    }
}