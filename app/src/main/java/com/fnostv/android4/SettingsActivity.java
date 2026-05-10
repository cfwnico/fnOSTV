package com.fnostv.android4;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.fnostv.android4.config.ProfileStore;
import com.fnostv.android4.config.ProfileValidation;
import com.fnostv.android4.config.ProfileValidator;
import com.fnostv.android4.config.ServerProfile;

public final class SettingsActivity extends Activity {
    private ProfileStore store;
    private EditText urlInput;
    private EditText usernameInput;
    private EditText passwordInput;
    private CheckBox autoLoginInput;
    private CheckBox trustSslInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        store = new ProfileStore(this);
        buildLayout(store.load());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void buildLayout(ServerProfile profile) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(0xFF101820);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(48), dp(32), dp(48), dp(32));
        scrollView.addView(panel, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("fnOSTV 服务器设置");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(28);
        title.setGravity(Gravity.LEFT);
        panel.addView(title, rowParams());

        TextView hint = new TextView(this);
        hint.setText("填写飞牛影视 Web 地址；遥控器菜单键可随时回到这里。");
        hint.setTextColor(0xFFB6C2C0);
        hint.setTextSize(16);
        panel.addView(hint, rowParams());

        urlInput = input("服务器地址，例如 http://192.168.1.20:5666", false);
        urlInput.setText(profile.baseUrl);
        panel.addView(label("服务器地址"));
        panel.addView(urlInput, rowParams());

        usernameInput = input("用户名/账号", false);
        usernameInput.setText(profile.username);
        panel.addView(label("账号"));
        panel.addView(usernameInput, rowParams());

        passwordInput = input("密码", true);
        passwordInput.setText(profile.password);
        panel.addView(label("密码"));
        panel.addView(passwordInput, rowParams());

        autoLoginInput = checkbox("自动尝试登录", profile.autoLogin);
        panel.addView(autoLoginInput, rowParams());

        trustSslInput = checkbox("信任该服务器的 SSL 证书异常", profile.trustSslErrors);
        panel.addView(trustSslInput, rowParams());

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.RIGHT);
        panel.addView(actions, rowParams());

        Button cancel = button("取消");
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        actions.addView(cancel, buttonParams());

        Button save = button("保存并进入");
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });
        actions.addView(save, buttonParams());

        setContentView(scrollView);
    }

    private void saveProfile() {
        ServerProfile profile = new ServerProfile(
                urlInput.getText().toString(),
                usernameInput.getText().toString(),
                passwordInput.getText().toString(),
                autoLoginInput.isChecked(),
                trustSslInput.isChecked());
        ProfileValidation validation = ProfileValidator.validate(profile);
        if (!validation.isValid()) {
            Toast.makeText(this, validation.getMessage(), Toast.LENGTH_SHORT).show();
            urlInput.requestFocus();
            return;
        }
        store.save(profile);
        setResult(RESULT_OK);
        finish();
    }

    private TextView label(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(0xFFDDE6E3);
        label.setTextSize(15);
        label.setPadding(0, dp(12), 0, dp(4));
        return label;
    }

    private EditText input(String hint, boolean password) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setHintTextColor(0xFF77827F);
        input.setTextColor(0xFFFFFFFF);
        input.setTextSize(18);
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(12), 0, dp(12), 0);
        input.setBackgroundColor(0xFF1D2A2B);
        if (password) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        }
        return input;
    }

    private CheckBox checkbox(String text, boolean checked) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(text);
        checkBox.setTextColor(0xFFFFFFFF);
        checkBox.setTextSize(16);
        checkBox.setChecked(checked);
        return checkBox;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(16);
        button.setMinWidth(dp(128));
        return button;
    }

    private LinearLayout.LayoutParams rowParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(8);
        return params;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(48));
        params.leftMargin = dp(12);
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
