package com.example.jizhang.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jizhang.R;
import com.example.jizhang.model.PendingBill;
import com.example.jizhang.util.Palette;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 待确认账单列表适配器（支持批量选择）
 */
public class PendingAdapter extends RecyclerView.Adapter<PendingAdapter.VH> {

    public interface Listener {
        void onConfirm(PendingBill p);
        void onIgnore(PendingBill p);
        void onSelectionChanged(int count);
    }

    private final List<PendingBill> items = new ArrayList<>();
    private final Set<Long> selectedIds = new HashSet<>();
    private boolean selectionMode = false;
    private Listener listener;

    public void setListener(Listener l) {
        this.listener = l;
    }

    public void setItems(List<PendingBill> list) {
        items.clear();
        if (list != null) items.addAll(list);
        selectedIds.clear();
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(boolean mode) {
        selectionMode = mode;
        if (!mode) selectedIds.clear();
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(selectedIds.size());
    }

    public Set<Long> getSelectedIds() {
        return selectedIds;
    }

    private void toggleSelection(long id) {
        if (selectedIds.contains(id)) selectedIds.remove(id);
        else selectedIds.add(id);
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(selectedIds.size());
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        PendingBill p = items.get(position);
        h.source.setText(p.source);
        h.source.getBackground().setTint(p.isExpense() ? Palette.COLOR_EXPENSE : Palette.COLOR_INCOME);

        String sign = p.isExpense() ? "-" : "+";
        h.amount.setText(sign + "¥" + Palette.formatAmount(p.amount));
        h.amount.setTextColor(p.isExpense() ? Palette.COLOR_EXPENSE : Palette.COLOR_INCOME);

        h.content.setText(p.content == null ? "" : p.content);
        h.time.setText(Palette.formatDateTime(p.dateTime));

        if (selectionMode) {
            h.cbSelect.setVisibility(View.VISIBLE);
            h.cbSelect.setChecked(selectedIds.contains(p.id));
            h.confirm.setVisibility(View.GONE);
            h.ignore.setVisibility(View.GONE);
            h.itemView.setOnClickListener(v -> toggleSelection(p.id));
        } else {
            h.cbSelect.setVisibility(View.GONE);
            h.confirm.setVisibility(View.VISIBLE);
            h.ignore.setVisibility(View.VISIBLE);
            h.itemView.setOnClickListener(null);
        }

        h.confirm.setOnClickListener(v -> {
            if (listener != null) listener.onConfirm(p);
        });
        h.ignore.setOnClickListener(v -> {
            if (listener != null) listener.onIgnore(p);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView source, amount, content, time, confirm, ignore;
        CheckBox cbSelect;

        VH(@NonNull View v) {
            super(v);
            source = v.findViewById(R.id.tv_source);
            amount = v.findViewById(R.id.tv_amount);
            content = v.findViewById(R.id.tv_content);
            time = v.findViewById(R.id.tv_time);
            confirm = v.findViewById(R.id.btn_confirm);
            ignore = v.findViewById(R.id.btn_ignore);
            cbSelect = v.findViewById(R.id.cb_select);
        }
    }
}
