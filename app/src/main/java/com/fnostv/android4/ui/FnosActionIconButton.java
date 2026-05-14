package com.fnostv.android4.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

final class FnosActionIconButton extends View {
    static final String TYPE_SEARCH = "search";
    static final String TYPE_USER = "user";
    static final String TYPE_SETTINGS = "settings";

    private final String type;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    FnosActionIconButton(Context context, String type) {
        super(context);
        this.type = type;
        paint.setColor(FnosTheme.COLOR_TEXT);
        paint.setStrokeWidth(FnosTheme.dp(context, 2));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight()) * 0.58f;
        float left = (getWidth() - size) / 2f;
        float top = (getHeight() - size) / 2f;
        if (TYPE_SEARCH.equals(type)) {
            drawSearch(canvas, left, top, size);
        } else if (TYPE_USER.equals(type)) {
            drawUser(canvas, left, top, size);
        } else if (TYPE_SETTINGS.equals(type)) {
            drawSettings(canvas, left, top, size);
        }
    }

    private void drawSearch(Canvas canvas, float left, float top, float s) {
        canvas.drawCircle(left + s * 0.43f, top + s * 0.43f, s * 0.26f, paint);
        canvas.drawLine(left + s * 0.62f, top + s * 0.62f, left + s * 0.82f, top + s * 0.82f, paint);
    }

    private void drawUser(Canvas canvas, float left, float top, float s) {
        canvas.drawCircle(left + s * 0.50f, top + s * 0.34f, s * 0.18f, paint);
        rect.set(left + s * 0.22f, top + s * 0.58f, left + s * 0.78f, top + s * 0.90f);
        canvas.drawArc(rect, 204, 132, false, paint);
    }

    private void drawSettings(Canvas canvas, float left, float top, float s) {
        float cx = left + s * 0.50f;
        float cy = top + s * 0.50f;
        float inner = s * 0.16f;
        float outer = s * 0.35f;
        canvas.drawCircle(cx, cy, inner, paint);
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2d * i / 8d;
            float x1 = cx + (float) Math.cos(angle) * (outer - s * 0.07f);
            float y1 = cy + (float) Math.sin(angle) * (outer - s * 0.07f);
            float x2 = cx + (float) Math.cos(angle) * outer;
            float y2 = cy + (float) Math.sin(angle) * outer;
            canvas.drawLine(x1, y1, x2, y2, paint);
        }
    }
}
