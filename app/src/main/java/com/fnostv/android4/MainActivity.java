package com.fnostv.android4;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fnostv.android4.config.ProfileStore;
import com.fnostv.android4.config.ServerProfile;
import com.fnostv.android4.util.Constants;
import com.fnostv.android4.web.FnosChromeClient;
import com.fnostv.android4.web.FnosDownloadListener;
import com.fnostv.android4.web.FnosWebViewClient;
import com.fnostv.android4.web.FullscreenVideoController;
import com.fnostv.android4.web.LoginScript;
import com.fnostv.android4.web.WebViewConfigurator;
import com.fnostv.android4.web.WebViewEvents;

public final class MainActivity extends Activity implements WebViewEvents {
    private FrameLayout root;
    private WebView webView;
    private ProgressBar progressBar;
    private TextView statusView;
    private FullscreenVideoController fullscreenVideoController;
    private ProfileStore store;
    private ServerProfile profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        store = new ProfileStore(this);
        CookieSyncManager.createInstance(this);
        buildLayout();
        configureWebView();
        loadProfileOrSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CookieSyncManager.getInstance().startSync();
        enterFullScreen();
    }

    @Override
    protected void onPause() {
        CookieSyncManager.getInstance().stopSync();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_SETTINGS) {
            loadProfileOrSettings();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            openSettings();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (fullscreenVideoController.isShowing()) {
                fullscreenVideoController.hide();
                return true;
            }
            if (webView.canGoBack()) {
                webView.goBack();
                return true;
            }
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_SPACE) {
            webView.loadUrl("javascript:(function(){var v=document.querySelector('video');if(v){v.paused?v.play():v.pause();}})()");
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void buildLayout() {
        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF101820);

        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        fullscreenVideoController = new FullscreenVideoController(root, webView);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(Constants.PROGRESS_BAR_HEIGHT_DP),
                Gravity.TOP);
        root.addView(progressBar, progressParams);

        statusView = new TextView(this);
        statusView.setTextColor(0xFFFFFFFF);
        statusView.setTextSize(18);
        statusView.setGravity(Gravity.CENTER);
        statusView.setBackgroundColor(0xCC101820);
        statusView.setVisibility(View.GONE);
        root.addView(statusView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);
    }

    private void configureWebView() {
        WebViewConfigurator.configure(this, webView);
        webView.setWebViewClient(new FnosWebViewClient(this, this));
        webView.setWebChromeClient(new FnosChromeClient(this, fullscreenVideoController));
        webView.setDownloadListener(new FnosDownloadListener(this));
    }

    private void loadProfileOrSettings() {
        profile = store.load();
        if (!profile.isReady()) {
            showStatus("首次使用请配置飞牛服务地址");
            openSettings();
            return;
        }
        statusView.setVisibility(View.GONE);
        webView.loadUrl(profile.baseUrl);
    }

    private void openSettings() {
        startActivityForResult(new Intent(this, SettingsActivity.class), Constants.REQUEST_SETTINGS);
    }

    private void showStatus(String message) {
        statusView.setText(message);
        statusView.setVisibility(View.VISIBLE);
    }

    private void enterFullScreen() {
        fullscreenVideoController.enterImmersiveMode();
    }

    @Override
    public void onPageLoadStarted() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPageLoadFinished(WebView view) {
        progressBar.setVisibility(View.GONE);
        CookieSyncManager.getInstance().sync();
        if (profile.autoLogin && profile.username.length() > 0 && profile.password.length() > 0) {
            view.loadUrl(LoginScript.build(profile));
        }
    }

    @Override
    public void onProgressChanged(int progress) {
        progressBar.setProgress(progress);
        progressBar.setVisibility(progress >= 100 ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onMainFrameError(String description) {
        showStatus("加载失败：" + description + "\n按菜单键修改服务器设置");
    }

    @Override
    public void onSslError(final SslErrorHandler handler, SslError error) {
        if (profile != null && profile.trustSslErrors) {
            handler.proceed();
            return;
        }
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("SSL 证书异常")
                .setMessage("当前服务器证书无法验证，是否继续访问？")
                .setPositiveButton("继续", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handler.proceed();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handler.cancel();
                    }
                })
                .show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
