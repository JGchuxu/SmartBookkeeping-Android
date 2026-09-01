package com.example.jizhang.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jizhang.R;
import com.example.jizhang.util.Palette;

/**
 * 颜色选择器适配器（54 色，按 9 列网格展示）
 */
public class ColorPickerAdapter extends RecyclerView.Adapter<ColorPickerAdapter.VH> {

    public interface OnColorSelected {
        void onSelected(int colorIndex);
    }

    private int selectedIndex = 0;
    private OnColorSelected listener;

    public void setListener(OnColorSelected l) {
        this.listener = l;
    }

    public void setSelectedIndex(int idx) {
        this.selectedIndex = idx;
        notifyDataSetChanged();
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_color_picker, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        int color = Palette.colorOf(position);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        if (position == selectedIndex) {
            drawable.setStroke((int) dp(h, 3), 0xFF333333);
        }
        h.dot.setBackground(drawable);

        h.itemView.setOnClickListener(v -> {
            selectedIndex = position;
            notifyDataSetChanged();
            if (listener != null) listener.onSelected(position);
        });
    }

    @Override
    public int getItemCount() {
        return Palette.colorCount();
    }

    private float dp(VH h, float v) {
        return v * h.itemView.getResources().getDisplayMetrics().density;
    }

    static class VH extends RecyclerView.ViewHolder {
        View dot;

        VH(@NonNull View v) {
            super(v);
            dot = v.findViewById(R.id.color_dot);
        }
    }
}