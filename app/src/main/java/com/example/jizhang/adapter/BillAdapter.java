package com.example.jizhang.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jizhang.R;
import com.example.jizhang.model.Category;
import com.example.jizhang.model.Transaction;
import com.example.jizhang.util.EmojiIcons;
import com.example.jizhang.util.Palette;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 账单列表适配器，按日期分组显示，支持多选批量删除
 */
public class BillAdapter extends RecyclerView.Adapter<BillAdapter.VH> {

    public interface Listener {
        void onClick(Transaction t);
        void onLongClick(Transaction t);
        void onSelectionChanged(int count);
    }

    private final List<Transaction> items = new ArrayList<>();
    private final Set<Long> selectedIds = new HashSet<>();
    private boolean selectionMode = false;
    private Map<Long, String> channelNames;
    private Map<Long, Category> categories;
    private Listener listener;

    public BillAdapter(Map<Long, String> channelNames, Map<Long, Category> categories) {
        this.channelNames = channelNames;
        this.categories = categories;
    }

    public void setMaps(Map<Long, String> channelNames, Map<Long, Category> categories) {
        this.channelNames = channelNames;
        this.categories = categories;
        notifyDataSetChanged();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<Transaction> list) {
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
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bill, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Transaction t = items.get(position);

        // 日期分组头
        boolean showHeader = position == 0 || !sameDay(t.dateTime, items.get(position - 1).dateTime);
        h.header.setVisibility(showHeader ? View.VISIBLE : View.GONE);
        if (showHeader) h.header.setText(formatHeader(t.dateTime));

        // 分类信息
        Category cat = categories.get(t.categoryId);
        String catName = cat != null ? cat.name : "未知";
        int colorIndex = cat != null ? cat.colorIndex : 0;

        h.icon.setText(EmojiIcons.category(catName));
        h.icon.getBackground().setTint(Palette.colorOf(colorIndex));
        h.category.setText(catName);

        String channel = channelNames.get(t.channelId);
        h.channelTime.setText((channel != null ? channel : "未知") + "  " + Palette.formatTime(t.dateTime));

        h.note.setText(t.note == null || t.note.isEmpty() ? "" : t.note);
        h.note.setVisibility(t.note == null || t.note.isEmpty() ? View.GONE : View.VISIBLE);

        // 金额
        String sign = t.isExpense() ? "-" : "+";
        int color = t.isExpense() ? Palette.COLOR_EXPENSE : Palette.COLOR_INCOME;
        h.amount.setText(sign + "¥" + Palette.formatAmount(t.amount));
        h.amount.setTextColor(color);

        if (selectionMode) {
            boolean selected = selectedIds.contains(t.id);
            h.cbSelect.setVisibility(View.VISIBLE);
            h.cbSelect.setChecked(selected);
            h.itemView.setOnClickListener(v -> toggleSelection(t.id));
            h.itemView.setOnLongClickListener(null);
        } else {
            h.cbSelect.setVisibility(View.GONE);
            h.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(t);
            });
            h.itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onLongClick(t);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private boolean sameDay(long a, long b) {
        Calendar ca = Calendar.getInstance();
        ca.setTimeInMillis(a);
        Calendar cb = Calendar.getInstance();
        cb.setTimeInMillis(b);
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR);
    }

    private String formatHeader(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        if (c.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && c.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            return "今天";
        }
        if (c.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR)
                && c.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)) {
            return "昨天";
        }
        return String.format(Locale.CHINA, "%d年%d月%d日",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView header, icon, category, note, channelTime, amount;
        CheckBox cbSelect;

        VH(@NonNull View v) {
            super(v);
            header = v.findViewById(R.id.tv_header);
            icon = v.findViewById(R.id.tv_category_icon);
            category = v.findViewById(R.id.tv_category_name);
            note = v.findViewById(R.id.tv_note);
            channelTime = v.findViewById(R.id.tv_channel_time);
            amount = v.findViewById(R.id.tv_amount);
            cbSelect = v.findViewById(R.id.cb_select);
        }
    }
}
