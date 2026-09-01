package com.example.jizhang.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import com.example.jizhang.model.TrendPoint;
import com.example.jizhang.util.Palette;

import java.util.ArrayList;
import java.util.List;

/**
 * 自绘趋势折线图：展示支出与收入随时间的走势
 */
public class LineChartView extends View {

    private final List<TrendPoint> data = new ArrayList<>();

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        gridPaint.setColor(0x33FFFFFF);
        gridPaint.setStrokeWidth(1);
        textPaint.setColor(0xFF999999);
        textPaint.setTextSize(dp(11));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2.5f));
    }

    public void setData(List<TrendPoint> points) {
        data.clear();
        if (points != null) data.addAll(points);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (data.isEmpty()) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("暂无数据", w / 2f, h / 2f, textPaint);
            return;
        }

        double max = 0;
        for (TrendPoint p : data) {
            max = Math.max(max, Math.max(p.expense, p.income));
        }
        if (max <= 0) max = 1;

        float left = dp(48);
        float top = dp(20);
        float right = w - dp(8);
        float bottom = h - dp(30);
        float chartW = right - left;
        float chartH = bottom - top;

        // 网格
        textPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= 4; i++) {
            float y = top + chartH * i / 4f;
            canvas.drawLine(left, y, right, y, gridPaint);
            double val = max * (4 - i) / 4f;
            canvas.drawText(Palette.formatAmountShort(val), left - dp(6), y + dp(4), textPaint);
        }

        int n = data.size();
        float stepX = n > 1 ? chartW / (n - 1) : chartW;
        float x0 = left;

        // 支出折线
        drawLine(canvas, x0, stepX, top, bottom, chartH, max, true);
        // 收入折线
        drawLine(canvas, x0, stepX, top, bottom, chartH, max, false);

        // X 轴标签（最多显示 6 个）
        textPaint.setTextAlign(Paint.Align.CENTER);
        int labelStep = Math.max(1, (int) Math.ceil(n / 6.0));
        for (int i = 0; i < n; i += labelStep) {
            float x = x0 + stepX * i;
            String label = Palette.formatMonthDay(data.get(i).dateMillis);
            canvas.drawText(label, x, bottom + dp(16), textPaint);
        }
    }

    private void drawLine(Canvas canvas, float x0, float stepX, float top, float bottom, float chartH, double maxVal, boolean expense) {
        Path path = new Path();
        int n = data.size();
        boolean started = false;
        for (int i = 0; i < n; i++) {
            TrendPoint p = data.get(i);
            double val = expense ? p.expense : p.income;
            float x = x0 + stepX * i;
            float y = bottom - (float) (val / maxVal * chartH);
            if (!started) {
                path.moveTo(x, y);
                started = true;
            } else {
                path.lineTo(x, y);
            }
        }
        // 折线渐变：顶部亮色到底部暗色
        linePaint.setShader(new LinearGradient(0, top, 0, bottom,
                expense ? 0xFFFF8A80 : 0xFF81E6A0,
                expense ? Palette.COLOR_EXPENSE : Palette.COLOR_INCOME,
                Shader.TileMode.CLAMP));
        canvas.drawPath(path, linePaint);
        linePaint.setShader(null);

        pointPaint.setColor(expense ? Palette.COLOR_EXPENSE : Palette.COLOR_INCOME);
        for (int i = 0; i < n; i++) {
            TrendPoint p = data.get(i);
            double val = expense ? p.expense : p.income;
            float x = x0 + stepX * i;
            float y = bottom - (float) (val / maxVal * chartH);
            canvas.drawCircle(x, y, dp(3), pointPaint);
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
