package com.example.jizhang.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
 * 分类选择网格适配器（记账录入页使用）
 */
public class CategoryGridAdapter extends RecyclerView.Adapter<CategoryGridAdapter.VH> {

    public interface OnCategoryClick {
        void onClick(Category c);
        void onAddClick();
    }

    private final List<Category> items = new ArrayList<>();
    private long selectedId = -1;
    private OnCategoryClick listener;

    public void setListener(OnCategoryClick l) {
        this.listener = l;
    }

    public void setItems(List<Category> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void setSelectedId(long id) {
        this.selectedId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_icon, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        if (position == items.size()) {
            h.icon.setText("+");
            h.icon.getBackground().setTint(0xFFEEEEEE);
            h.name.setText("添加");
            h.icon.setAlpha(0.8f);
            h.name.setTextColor(0xFF888888);
            h.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onAddClick();
            });
            return;
        }
        Category c = items.get(position);
        h.icon.setText(EmojiIcons.category(c.name));
        h.icon.getBackground().setTint(Palette.colorOf(c.colorIndex));
        h.name.setText(c.name);

        boolean selected = c.id == selectedId;
        h.icon.setAlpha(selected ? 1.0f : 0.38f);
        h.name.setTextColor(selected ? 0xFF333333 : 0xFFAAAAAA);

        h.itemView.setOnClickListener(v -> {
            selectedId = c.id;
            notifyDataSetChanged();
            if (listener != null) listener.onClick(c);
        });
    }

    @Override
    public int getItemCount() {
        return items.size() + 1;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView icon, name;

        VH(@NonNull View v) {
            super(v);
            icon = v.findViewById(R.id.tv_icon);
            name = v.findViewById(R.id.tv_name);
        }
    }
}
