package com.fnostv.android4.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;

public final class FnosTheme {
    public static final int COLOR_APP_BG = 0xFF151A21;
    public static final int COLOR_SIDEBAR = 0xFF10151B;
    public static final int COLOR_PANEL = 0xE61B2029;
    public static final int COLOR_CARD = 0xFF1D242D;
    public static final int COLOR_CARD_FOCUSED = 0xFF26384B;
    public static final int COLOR_PRIMARY = 0xFF2F86F6;
    public static final int COLOR_TEXT = 0xFFFFFFFF;
    public static final int COLOR_TEXT_MUTED = 0xFF9DA8B6;
    public static final int COLOR_TEXT_DIM = 0xFF6F7B89;
    public static final int COLOR_STROKE = 0xFF303946;
    public static final int COLOR_ERROR = 0xFF64292F;

    private FnosTheme() {
    }

    public static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static GradientDrawable rounded(int color, int radiusDp, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    public static GradientDrawable stroked(int color, int strokeColor, int radiusDp, Context context) {
        GradientDrawable drawable = rounded(color, radiusDp, context);
        drawable.setStroke(dp(context, 1), strokeColor);
        return drawable;
    }
}
