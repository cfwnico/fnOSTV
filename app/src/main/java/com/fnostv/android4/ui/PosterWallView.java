package com.fnostv.android4.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import com.fnostv.android4.net.FnosFileEntry;

import java.util.ArrayList;
import java.util.List;

public final class PosterWallView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<String> titles = new ArrayList<String>();
    private final int[] colors = {
            0xFF243B55, 0xFF2C2A4A, 0xFF45343D, 0xFF213C3C,
            0xFF3B2D26, 0xFF283546, 0xFF352C45, 0xFF25362D
    };

    public PosterWallView(Context context) {
        super(context);
        titles.add("电影");
        titles.add("剧集");
        titles.add("纪录片");
        titles.add("动画");
        titles.add("音乐");
        titles.add("家庭影院");
    }

    public void setMediaEntries(List<FnosFileEntry> entries) {
        titles.clear();
        if (entries != null) {
            for (int i = 0; i < entries.size() && titles.size() < 24; i++) {
                FnosFileEntry entry = entries.get(i);
                if (entry != null && entry.name.length() > 0) {
                    titles.add(entry.name);
                }
            }
        }
        if (titles.size() == 0) {
            titles.add("电影");
            titles.add("剧集");
            titles.add("纪录片");
            titles.add("动画");
            titles.add("音乐");
            titles.add("家庭影院");
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF06080B);
        canvas.drawRect(0, 0, width, height, paint);

        int posterW = Math.max(90, width / 9);
        int posterH = posterW * 3 / 2;
        int gap = Math.max(10, posterW / 10);
        int startX = -posterW / 2;
        int startY = -posterH / 3;
        int index = 0;
        for (int y = startY; y < height + posterH; y += posterH + gap) {
            int offset = ((y / Math.max(1, posterH + gap)) & 1) == 0 ? 0 : -(posterW / 3);
            for (int x = startX + offset; x < width + posterW; x += posterW + gap) {
                drawPoster(canvas, x, y, posterW, posterH, index++);
            }
        }

        paint.setColor(0xB8000000);
        canvas.drawRect(0, 0, width, height, paint);
    }

    private void drawPoster(Canvas canvas, int left, int top, int width, int height, int index) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(colors[index % colors.length]);
        canvas.drawRect(left, top, left + width, top + height, paint);
        paint.setColor(0x662F86F6);
        canvas.drawRect(left, top, left + width, top + height / 3, paint);
        paint.setColor(0xDDFFFFFF);
        paint.setTextSize(Math.max(12, width / 8));
        String title = titles.get(index % titles.size());
        if (title.length() > 8) {
            title = title.substring(0, 8);
        }
        canvas.drawText(title, left + width / 10, top + height - height / 6, paint);
    }
}
