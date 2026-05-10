package com.fnostv.android4.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

public final class StatusOverlay {
    private final TextView view;

    public StatusOverlay(Context context) {
        view = new TextView(context);
        view.setTextColor(0xFFFFFFFF);
        view.setTextSize(18);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundColor(0xCC101820);
        view.setVisibility(View.GONE);
    }

    public TextView getView() {
        return view;
    }

    public void show(String message) {
        view.setText(message);
        view.setVisibility(View.VISIBLE);
    }

    public void hide() {
        view.setVisibility(View.GONE);
    }

    public static FrameLayout.LayoutParams layoutParams() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
    }
}
