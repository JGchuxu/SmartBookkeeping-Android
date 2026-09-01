package com.example.jizhang.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jizhang.R;
import com.example.jizhang.model.Category;
import com.example.jizhang.util.EmojiIcons;
import com.example.jizhang.util.Palette;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类管理列表适配器
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {

    public interface Listener {
        void onEdit(Category c);
        void onDelete(Category c);
    }

    private final List<Category> items = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener l) {
        this.listener = l;
    }

    public void setItems(List<Category> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Category c = items.get(position);
        h.icon.setText(EmojiIcons.category(c.name));
        h.icon.getBackground().setTint(Palette.colorOf(c.colorIndex));
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
        TextView icon, name;
        ImageView edit, delete;

        VH(@NonNull View v) {
            super(v);
            icon = v.findViewById(R.id.tv_category_icon);
            name = v.findViewById(R.id.tv_category_name);
            edit = v.findViewById(R.id.btn_edit);
            delete = v.findViewById(R.id.btn_delete);
        }
    }
}
