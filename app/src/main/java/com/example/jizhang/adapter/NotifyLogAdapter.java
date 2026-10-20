package com.example.jizhang.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jizhang.R;
import com.example.jizhang.model.NotifyLog;
import com.example.jizhang.model.Transaction;
import com.example.jizhang.util.Palette;

import java.util.ArrayList;
import java.util.List;

/**
 * 通知识别日志列表适配器
 */
public class NotifyLogAdapter extends RecyclerView.Adapter<NotifyLogAdapter.VH> {

    private final List<NotifyLog> items = new ArrayList<>();

    public void setItems(List<NotifyLog> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notify_log, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        NotifyLog l = items.get(position);
        h.source.setText(l.source);
        // 支付宝蓝 / 微信绿
        int sourceColor = "支付宝".equals(l.source) ? 0xFF1677FF : 0xFF07C160;
        h.source.getBackground().setTint(sourceColor);

        // 状态：入账 / 解析失败 / 广告过滤 / 重复跳过 / 无关通知
        if (l.parsed) {
            boolean income = l.resultType == Transaction.TYPE_INCOME;
            h.status.setText((income ? "收入" : "支出") + " ¥" + Palette.formatAmount(l.resultAmount));
            h.status.setTextColor(income ? Palette.COLOR_INCOME : Palette.COLOR_EXPENSE);
        } else {
            // 负数编码 = 处理原因（诊断用）
            String reason;
            int color = 0xFF999999;
            switch (l.resultType) {
                case -1:
                    reason = "❓ 解析失败（已转待处理）";
                    color = 0xFFFF8C00; // 橙：需要用户关注
                    break;
                case -2:
                    reason = "🚫 广告/营销（已过滤）";
                    break;
                case -3:
                    reason = "🔁 重复通知（已跳过）";
                    break;
                case -4:
                    reason = "📴 无关通知";
                    break;
                default:
                    reason = "未识别";
            }
            h.status.setText(reason);
            h.status.setTextColor(color);
        }

        h.title.setText(l.title == null || l.title.isEmpty() ? "（无标题）" : l.title);
        h.text.setText(l.text == null || l.text.isEmpty() ? "（无文本内容）" : l.text);
        h.time.setText(Palette.formatDateTime(l.dateTime));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView source, status, title, text, time;

        VH(@NonNull View v) {
            super(v);
            source = v.findViewById(R.id.tv_source);
            status = v.findViewById(R.id.tv_status);
            title = v.findViewById(R.id.tv_title);
            text = v.findViewById(R.id.tv_text);
            time = v.findViewById(R.id.tv_time);
        }
    }
}
