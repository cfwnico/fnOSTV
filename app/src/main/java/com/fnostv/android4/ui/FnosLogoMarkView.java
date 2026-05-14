package com.fnostv.android4.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

final class FnosLogoMarkView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    FnosLogoMarkView(Context context) {
        super(context);
        paint.setColor(FnosTheme.COLOR_PRIMARY);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float s = Math.min(getWidth(), getHeight());
        float left = (getWidth() - s) / 2f;
        float top = (getHeight() - s) / 2f;
        rect.set(left + s * 0.16f, top + s * 0.28f, left + s * 0.78f, top + s * 0.90f);
        canvas.drawArc(rect, 118, 292, true, paint);

        paint.setColor(FnosTheme.COLOR_APP_BG);
        canvas.drawCircle(left + s * 0.46f, top + s * 0.58f, s * 0.13f, paint);
        paint.setColor(FnosTheme.COLOR_PRIMARY);

        path.reset();
        path.moveTo(left + s * 0.48f, top + s * 0.12f);
        path.lineTo(left + s * 0.66f, top + s * 0.12f);
        path.lineTo(left + s * 0.56f, top + s * 0.34f);
        path.lineTo(left + s * 0.38f, top + s * 0.34f);
        path.close();
        canvas.drawPath(path, paint);

        path.reset();
        path.moveTo(left + s * 0.74f, top + s * 0.12f);
        path.lineTo(left + s * 0.92f, top + s * 0.12f);
        path.lineTo(left + s * 0.80f, top + s * 0.34f);
        path.lineTo(left + s * 0.62f, top + s * 0.34f);
        path.close();
        canvas.drawPath(path, paint);
    }
}
