package com.example.jizhang.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.example.jizhang.model.StatItem;
import com.example.jizhang.util.Palette;

import java.util.ArrayList;
import java.util.List;

/**
 * 自绘环形饼图，中心显示总额，下方显示图例
 */
public class PieChartView extends View {

    private final List<StatItem> data = new ArrayList<>();
    private String centerLabel = "总计";

    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint legendPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(List<StatItem> items, String centerLabel) {
        data.clear();
        if (items != null) data.addAll(items);
        this.centerLabel = centerLabel;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        double total = 0;
        for (StatItem s : data) total += s.value;

        float ringRadius = Math.min(w, h * 0.42f) * 0.5f * 0.72f;
        float cx = w / 2f;
        float cy = h * 0.24f;

        if (total <= 0) {
            textPaint.setColor(0xFF999999);
            textPaint.setTextSize(dp(14));
            canvas.drawText("暂无数据", cx, cy, textPaint);
            return;
        }

        RectF oval = new RectF(cx - ringRadius, cy - ringRadius, cx + ringRadius, cy + ringRadius);

        // 绘制圆环
        float startAngle = -90f;
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(ringRadius * 0.55f);
        for (int i = 0; i < data.size(); i++) {
            StatItem s = data.get(i);
            float sweep = (float) (s.value / total * 360f);
            arcPaint.setColor(Palette.colorOf(s.colorIndex));
            canvas.drawArc(oval, startAngle, sweep, false, arcPaint);
            startAngle += sweep;
        }

        // 中心文字
        textPaint.setColor(0xFF333333);
        textPaint.setTextSize(dp(15));
        textPaint.setFakeBoldText(true);
        canvas.drawText(centerLabel, cx, cy - dp(6), textPaint);
        textPaint.setTextSize(dp(18));
        canvas.drawText("¥" + Palette.formatAmount(total), cx, cy + dp(16), textPaint);

        // 图例
        float legendY = cy + ringRadius + dp(16);
        float legendX = dp(16);
        float maxX = w - dp(16);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setFakeBoldText(false);
        textPaint.setTextSize(dp(12));

        for (int i = 0; i < data.size(); i++) {
            StatItem s = data.get(i);
            legendPaint.setColor(Palette.colorOf(s.colorIndex));
            float pct = (float) (s.value / total * 100f);
            String label = s.name + " " + String.format("%.1f%%", pct);

            if (legendX + dp(80) > maxX) {
                legendY += dp(22);
                legendX = dp(16);
            }
            canvas.drawCircle(legendX + dp(5), legendY - dp(4), dp(5), legendPaint);
            textPaint.setColor(0xFFC8C8D8);
            canvas.drawText(label, legendX + dp(16), legendY, textPaint);
            legendX += dp(16) + textPaint.measureText(label) + dp(18);
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
