package com.fnostv.android4.ui;

import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fnostv.android4.config.ServerProfile;
import com.fnostv.android4.net.RecentPlaybackStore;

public final class SettingsForm {
    public interface Listener {
        void onSaveRequested(ServerProfile profile);

        void onCancelRequested();
    }

    private final Context context;
    private final Listener listener;
    private EditText urlInput;
    private EditText usernameInput;
    private EditText passwordInput;
    private CheckBox autoLoginInput;
    private CheckBox trustSslInput;

    public SettingsForm(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public View create(ServerProfile profile) {
        return create(profile, null);
    }

    public View create(ServerProfile profile, String errorMessage) {
        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(0xFF020407);

        PosterWallView posterWallView = new PosterWallView(context);
        posterWallView.setMediaEntries(new RecentPlaybackStore(context).list());
        root.addView(posterWallView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(dp(40), dp(34), dp(40), dp(34));
        panel.setBackgroundDrawable(FnosTheme.stroked(FnosTheme.COLOR_PANEL, 0xFF394353, 16, context));
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(dp(448), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        root.addView(panel, panelParams);

        TextView logo = new TextView(context);
        logo.setText("fnOSTV");
        logo.setTextColor(FnosTheme.COLOR_TEXT);
        logo.setTextSize(30);
        logo.setGravity(Gravity.CENTER);
        panel.addView(logo, rowParams(0, 4));

        TextView subtitle = new TextView(context);
        subtitle.setText("原生轻量影视客户端");
        subtitle.setTextColor(FnosTheme.COLOR_TEXT_MUTED);
        subtitle.setTextSize(15);
        subtitle.setGravity(Gravity.CENTER);
        panel.addView(subtitle, rowParams(0, 24));

        if (errorMessage != null && errorMessage.length() > 0) {
            TextView error = new TextView(context);
            error.setText(errorMessage);
            error.setTextColor(0xFFFFD6D6);
            error.setTextSize(14);
            error.setPadding(dp(12), dp(8), dp(12), dp(8));
            error.setBackgroundDrawable(FnosTheme.rounded(FnosTheme.COLOR_ERROR, 6, context));
            panel.addView(error, rowParams(0, 14));
        }

        urlInput = input("服务器地址，例如 http://192.168.1.20:5666", false);
        urlInput.setText(profile.baseUrl);
        panel.addView(urlInput, rowParams(0, 12));

        usernameInput = input("用户名", false);
        usernameInput.setText(profile.username);
        panel.addView(usernameInput, rowParams(0, 12));

        passwordInput = input("密码", true);
        passwordInput.setText(profile.password);
        panel.addView(passwordInput, rowParams(0, 12));

        LinearLayout options = new LinearLayout(context);
        options.setOrientation(LinearLayout.HORIZONTAL);
        options.setGravity(Gravity.CENTER_VERTICAL);
        autoLoginInput = checkbox("保持登录", profile.autoLogin);
        options.addView(autoLoginInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        trustSslInput = checkbox("信任 SSL 异常", profile.trustSslErrors);
        options.addView(trustSslInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        panel.addView(options, rowParams(0, 18));

        Button login = button("登录", true);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onSaveRequested(readProfile());
            }
        });
        panel.addView(login, rowParams(0, 12));

        Button cancel = button("取消", false);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onCancelRequested();
            }
        });
        panel.addView(cancel, rowParams(0, 0));

        urlInput.requestFocus();
        return root;
    }

    public void focusServerUrl() {
        if (urlInput != null) {
            urlInput.requestFocus();
        }
    }

    private ServerProfile readProfile() {
        return new ServerProfile(
                urlInput.getText().toString(),
                usernameInput.getText().toString(),
                passwordInput.getText().toString(),
                autoLoginInput.isChecked(),
                trustSslInput.isChecked());
    }

    private EditText input(String hint, boolean password) {
        EditText input = new EditText(context);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setHintTextColor(0xFF7D8794);
        input.setTextColor(FnosTheme.COLOR_TEXT);
        input.setTextSize(16);
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setBackgroundDrawable(FnosTheme.stroked(0xFF20252E, 0xFF454E5D, 6, context));
        if (password) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        }
        return input;
    }

    private CheckBox checkbox(String text, boolean checked) {
        CheckBox checkBox = new CheckBox(context);
        checkBox.setText(text);
        checkBox.setTextColor(FnosTheme.COLOR_TEXT_MUTED);
        checkBox.setTextSize(13);
        checkBox.setChecked(checked);
        return checkBox;
    }

    private Button button(String text, boolean primary) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextSize(16);
        button.setTextColor(FnosTheme.COLOR_TEXT);
        button.setAllCaps(false);
        button.setMinHeight(dp(54));
        button.setBackgroundDrawable(FnosTheme.rounded(primary ? FnosTheme.COLOR_PRIMARY : FnosTheme.COLOR_CARD, 8, context));
        FocusStyler.apply(button, primary ? FnosTheme.COLOR_PRIMARY : FnosTheme.COLOR_CARD, 0xFF4EA1FF, 8);
        return button;
    }

    private LinearLayout.LayoutParams rowParams(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(top);
        params.bottomMargin = dp(bottom);
        return params;
    }

    private int dp(int value) {
        return FnosTheme.dp(context, value);
    }
}
