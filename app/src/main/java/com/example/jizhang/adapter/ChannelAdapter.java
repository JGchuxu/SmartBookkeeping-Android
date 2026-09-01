package com.example.jizhang.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jizhang.R;
import com.example.jizhang.model.Channel;

import java.util.ArrayList;
import java.util.List;

/**
 * 渠道管理列表适配器
 */
public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.VH> {

    public interface Listener {
        void onEdit(Channel c);
        void onDelete(Channel c);
    }

    private final List<Channel> items = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener l) {
        this.listener = l;
    }

    public void setItems(List<Channel> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_channel, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Channel c = items.get(position);
        h.name.setText(c.name);
        h.edit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(c);
        });
        h.delete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(c);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name;
        ImageView edit, delete;

        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.tv_channel_name);
            edit = v.findViewById(R.id.btn_edit);
            delete = v.findViewById(R.id.btn_delete);
        }
    }
}
