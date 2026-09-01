package com.example.jizhang.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import com.example.jizhang.model.StatItem;
import com.example.jizhang.util.Palette;

import java.util.ArrayList;
import java.util.List;

/**
 * 自绘分组柱状图：每个渠道展示支出（红）与收入（绿）两根柱子
 */
public class BarChartView extends View {

    private final List<StatItem> expense = new ArrayList<>();
    private final List<StatItem> income = new ArrayList<>();

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        gridPaint.setColor(0x33FFFFFF);
        gridPaint.setStrokeWidth(1);
        textPaint.setColor(0xFF999999);
        textPaint.setTextSize(dp(11));
    }

    public void setData(List<StatItem> expense, List<StatItem> income) {
        this.expense.clear();
        this.income.clear();
        if (expense != null) this.expense.addAll(expense);
        if (income != null) this.income.addAll(income);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        int n = Math.max(expense.size(), income.size());
        if (n == 0) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("暂无数据", w / 2f, h / 2f, textPaint);
            return;
        }

        double max = 0;
        for (StatItem s : expense) max = Math.max(max, s.value);
        for (StatItem s : income) max = Math.max(max, s.value);
        if (max <= 0) max = 1;

        float left = dp(48);
        float top = dp(24);
        float right = w - dp(8);
        float bottom = h - dp(34);
        float chartW = right - left;
        float chartH = bottom - top;

        // 图例
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(0xFFC8C8D8);
        float legendX = left;
        barPaint.setColor(Palette.COLOR_EXPENSE);
        canvas.drawRect(legendX, dp(6), legendX + dp(12), dp(6) + dp(12), barPaint);
        canvas.drawText("支出", legendX + dp(16), dp(6) + dp(10), textPaint);
        legendX += dp(16) + textPaint.measureText("支出") + dp(24);
        barPaint.setColor(Palette.COLOR_INCOME);
        canvas.drawRect(legendX, dp(6), legendX + dp(12), dp(6) + dp(12), barPaint);
        canvas.drawText("收入", legendX + dp(16), dp(6) + dp(10), textPaint);

        // 网格与 Y 轴
        textPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= 4; i++) {
            float y = top + chartH * i / 4f;
            canvas.drawLine(left, y, right, y, gridPaint);
            double val = max * (4 - i) / 4f;
            canvas.drawText(Palette.formatAmountShort(val), left - dp(6), y + dp(4), textPaint);
        }

        // 柱子
        float groupW = chartW / n;
        float barW = Math.min(groupW * 0.28f, dp(22));
        for (int i = 0; i < n; i++) {
            float centerX = left + groupW * i + groupW / 2f;

            double exp = i < expense.size() ? expense.get(i).value : 0;
            double inc = i < income.size() ? income.get(i).value : 0;

            float expH = (float) (exp / max * chartH);
            float incH = (float) (inc / max * chartH);

            // 支出柱：亮红→暗红渐变
            barPaint.setShader(new LinearGradient(0, bottom - expH, 0, bottom,
                    0xFFFF8A80, Palette.COLOR_EXPENSE, Shader.TileMode.CLAMP));
            canvas.drawRect(centerX - barW - dp(2), bottom - expH, centerX - dp(2), bottom, barPaint);
            barPaint.setShader(null);
            // 收入柱：亮绿→暗绿渐变
            barPaint.setShader(new LinearGradient(0, bottom - incH, 0, bottom,
                    0xFF81E6A0, Palette.COLOR_INCOME, Shader.TileMode.CLAMP));
            canvas.drawRect(centerX + dp(2), bottom - incH, centerX + barW + dp(2), bottom, barPaint);
            barPaint.setShader(null);

            // X 轴标签
            String name = i < expense.size() ? expense.get(i).name : (i < income.size() ? income.get(i).name : "");
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(0xFF888888);
            if (name.length() > 4) {
                canvas.save();
                canvas.rotate(-45f, centerX, bottom + dp(8));
                canvas.drawText(name, centerX, bottom + dp(14), textPaint);
                canvas.restore();
            } else {
                canvas.drawText(name, centerX, bottom + dp(18), textPaint);
            }
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
