package com.fnostv.android4.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class NativeHomeView {
    public interface Listener {
        void onHomeAction(String action);
    }

    public static final String ACTION_RECENT = "recent";
    public static final String ACTION_FILES = "files";
    public static final String ACTION_MEDIA = "media";
    public static final String ACTION_SETTINGS = "settings";

    private final Context context;
    private final Listener listener;
    private LinearLayout view;
    private Button firstButton;

    public NativeHomeView(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public View create() {
        view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(48), dp(36), dp(48), dp(36));
        view.setBackgroundColor(0xFF101820);
        view.setVisibility(View.GONE);

        TextView title = new TextView(context);
        title.setText("fnOSTV");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        view.addView(title, titleParams());

        TextView subtitle = new TextView(context);
        subtitle.setText("原生轻量客户端");
        subtitle.setTextColor(0xFFB8C7D9);
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        view.addView(subtitle, subtitleParams());

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        view.addView(actions, actionsParams());

        firstButton = actionButton("最近", ACTION_RECENT);
        actions.addView(firstButton, buttonParams());
        actions.addView(actionButton("文件库", ACTION_FILES), buttonParams());
        actions.addView(actionButton("影视入口", ACTION_MEDIA), buttonParams());
        actions.addView(actionButton("设置", ACTION_SETTINGS), buttonParams());
        return view;
    }

    public View getView() {
        return view;
    }

    public void show() {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
        if (firstButton != null) {
            firstButton.requestFocus();
        }
    }

    public void hide() {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    private Button actionButton(String label, final String action) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextSize(18);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setFocusable(true);
        button.setBackgroundColor(0xFF24364A);
        button.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                v.setBackgroundColor(hasFocus ? 0xFF2D8CFF : 0xFF24364A);
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onHomeAction(action);
            }
        });
        return button;
    }

    private LinearLayout.LayoutParams titleParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams subtitleParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(8);
        params.bottomMargin = dp(36);
        return params;
    }

    private LinearLayout.LayoutParams actionsParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(150), dp(84));
        params.leftMargin = dp(10);
        params.rightMargin = dp(10);
        return params;
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
