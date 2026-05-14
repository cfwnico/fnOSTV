package com.fnostv.android4.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

final class FnosSidebarIconView extends View {
    static final String TYPE_HOME = "home";
    static final String TYPE_FAVORITE = "favorite";
    static final String TYPE_LIBRARY = "library";
    static final String TYPE_ALL = "all";
    static final String TYPE_MOVIE = "movie";
    static final String TYPE_TV = "tv";
    static final String TYPE_OTHER = "other";

    private final String type;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    FnosSidebarIconView(Context context, String type) {
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
        float w = getWidth();
        float h = getHeight();
        float s = Math.min(w, h);
        float left = (w - s) / 2f;
        float top = (h - s) / 2f;
        if (TYPE_HOME.equals(type)) {
            drawHome(canvas, left, top, s);
        } else if (TYPE_FAVORITE.equals(type)) {
            drawFavorite(canvas, left, top, s);
        } else if (TYPE_LIBRARY.equals(type) || TYPE_MOVIE.equals(type)) {
            drawFilmReel(canvas, left, top, s);
        } else if (TYPE_ALL.equals(type)) {
            drawGrid(canvas, left, top, s);
        } else if (TYPE_TV.equals(type)) {
            drawTv(canvas, left, top, s);
        } else if (TYPE_OTHER.equals(type)) {
            drawFolder(canvas, left, top, s);
        }
    }

    private void drawHome(Canvas canvas, float left, float top, float s) {
        Path roof = new Path();
        roof.moveTo(left + s * 0.18f, top + s * 0.48f);
        roof.lineTo(left + s * 0.50f, top + s * 0.22f);
        roof.lineTo(left + s * 0.82f, top + s * 0.48f);
        canvas.drawPath(roof, paint);

        Path body = new Path();
        body.moveTo(left + s * 0.28f, top + s * 0.46f);
        body.lineTo(left + s * 0.28f, top + s * 0.80f);
        body.lineTo(left + s * 0.72f, top + s * 0.80f);
        body.lineTo(left + s * 0.72f, top + s * 0.46f);
        canvas.drawPath(body, paint);
    }

    private void drawFavorite(Canvas canvas, float left, float top, float s) {
        Path heart = new Path();
        heart.moveTo(left + s * 0.50f, top + s * 0.80f);
        heart.cubicTo(left + s * 0.18f, top + s * 0.58f, left + s * 0.16f, top + s * 0.28f, left + s * 0.36f, top + s * 0.25f);
        heart.cubicTo(left + s * 0.46f, top + s * 0.23f, left + s * 0.50f, top + s * 0.33f, left + s * 0.50f, top + s * 0.33f);
        heart.cubicTo(left + s * 0.50f, top + s * 0.33f, left + s * 0.56f, top + s * 0.23f, left + s * 0.66f, top + s * 0.25f);
        heart.cubicTo(left + s * 0.84f, top + s * 0.28f, left + s * 0.84f, top + s * 0.58f, left + s * 0.50f, top + s * 0.80f);
        canvas.drawPath(heart, paint);
    }

    private void drawFilmReel(Canvas canvas, float left, float top, float s) {
        rect.set(left + s * 0.18f, top + s * 0.18f, left + s * 0.82f, top + s * 0.82f);
        canvas.drawOval(rect, paint);
        canvas.drawCircle(left + s * 0.50f, top + s * 0.50f, s * 0.06f, paint);
        canvas.drawCircle(left + s * 0.50f, top + s * 0.32f, s * 0.035f, paint);
        canvas.drawCircle(left + s * 0.66f, top + s * 0.58f, s * 0.035f, paint);
        canvas.drawCircle(left + s * 0.34f, top + s * 0.58f, s * 0.035f, paint);
        canvas.drawLine(left + s * 0.74f, top + s * 0.74f, left + s * 0.86f, top + s * 0.86f, paint);
    }

    private void drawGrid(Canvas canvas, float left, float top, float s) {
        float cell = s * 0.22f;
        float gap = s * 0.12f;
        float startX = left + s * 0.22f;
        float startY = top + s * 0.22f;
        drawRoundRect(canvas, startX, startY, cell, cell);
        drawRoundRect(canvas, startX + cell + gap, startY, cell, cell);
        drawRoundRect(canvas, startX, startY + cell + gap, cell, cell);
        drawRoundRect(canvas, startX + cell + gap, startY + cell + gap, cell, cell);
    }

    private void drawTv(Canvas canvas, float left, float top, float s) {
        rect.set(left + s * 0.18f, top + s * 0.32f, left + s * 0.82f, top + s * 0.72f);
        canvas.drawRoundRect(rect, s * 0.05f, s * 0.05f, paint);
        canvas.drawLine(left + s * 0.42f, top + s * 0.78f, left + s * 0.58f, top + s * 0.78f, paint);
        canvas.drawLine(left + s * 0.50f, top + s * 0.72f, left + s * 0.50f, top + s * 0.78f, paint);
        canvas.drawLine(left + s * 0.40f, top + s * 0.26f, left + s * 0.50f, top + s * 0.32f, paint);
        canvas.drawLine(left + s * 0.60f, top + s * 0.26f, left + s * 0.50f, top + s * 0.32f, paint);
    }

    private void drawFolder(Canvas canvas, float left, float top, float s) {
        Path folder = new Path();
        folder.moveTo(left + s * 0.16f, top + s * 0.34f);
        folder.lineTo(left + s * 0.40f, top + s * 0.34f);
        folder.lineTo(left + s * 0.48f, top + s * 0.42f);
        folder.lineTo(left + s * 0.84f, top + s * 0.42f);
        folder.lineTo(left + s * 0.84f, top + s * 0.76f);
        folder.lineTo(left + s * 0.16f, top + s * 0.76f);
        folder.close();
        canvas.drawPath(folder, paint);
    }

    private void drawRoundRect(Canvas canvas, float x, float y, float w, float h) {
        rect.set(x, y, x + w, y + h);
        canvas.drawRoundRect(rect, w * 0.18f, h * 0.18f, paint);
    }
}
