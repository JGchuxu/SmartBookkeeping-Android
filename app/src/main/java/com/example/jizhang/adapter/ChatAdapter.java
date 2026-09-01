package com.example.jizhang.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jizhang.R;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 聊天消息适配器
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    public static class ChatMessage {
        public boolean fromUser;
        public String content;

        public ChatMessage(boolean fromUser, String content) {
            this.fromUser = fromUser;
            this.content = content;
        }
    }

    private static final int TYPE_USER = 0;
    private static final int TYPE_AI = 1;

    private final List<ChatMessage> items = new ArrayList<>();

    public void add(ChatMessage m) {
        items.add(m);
        notifyItemInserted(items.size() - 1);
    }

    public void updateLast(String content) {
        if (items.isEmpty()) return;
        items.get(items.size() - 1).content = content;
        notifyItemChanged(items.size() - 1);
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).fromUser ? TYPE_USER : TYPE_AI;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_USER ? R.layout.item_chat_user : R.layout.item_chat_ai;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        h.content.setText(items.get(position).content);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView content;

        VH(@NonNull View v) {
            super(v);
            content = v.findViewById(R.id.tv_content);
        }
    }
}
