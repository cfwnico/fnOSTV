package com.fnostv.android4;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.fnostv.android4.config.ProfileStore;
import com.fnostv.android4.config.ServerProfile;
import com.fnostv.android4.tv.RemoteActions;
import com.fnostv.android4.tv.RemoteKeyHandler;
import com.fnostv.android4.ui.StatusOverlay;
import com.fnostv.android4.util.Constants;
import com.fnostv.android4.util.Logger;
import com.fnostv.android4.web.FnosChromeClient;
import com.fnostv.android4.web.FnosDownloadListener;
import com.fnostv.android4.web.FnosWebViewClient;
import com.fnostv.android4.web.FullscreenVideoController;
import com.fnostv.android4.web.LoginScript;
import com.fnostv.android4.web.WebViewConfigurator;
import com.fnostv.android4.web.WebViewEvents;

public final class MainActivity extends Activity implements WebViewEvents, RemoteActions {
    private FrameLayout root;
    private WebView webView;
    private ProgressBar progressBar;
    private StatusOverlay statusOverlay;
    private FullscreenVideoController fullscreenVideoController;
    private RemoteKeyHandler remoteKeyHandler;
    private ProfileStore store;
    private ServerProfile profile;
    private boolean settingsOpen;
    private final Handler loadTimeoutHandler = new Handler();
    private final Runnable loadTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            handleServerConnectionFailure("连接服务器超时");
        }
    };
    private final Runnable blankPageCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (isBlankWebViewPage(webView)) {
                handleServerConnectionFailure("连接服务器失败：页面无响应");
            } else {
                markPageUsable(webView);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        store = new ProfileStore(this);
        CookieSyncManager.createInstance(this);
        buildLayout();
        remoteKeyHandler = new RemoteKeyHandler(this);
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
            cancelLoadTimeout();
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_SETTINGS) {
            settingsOpen = false;
            loadProfileOrSettings();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (remoteKeyHandler.onKeyDown(keyCode, event)) {
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

        statusOverlay = new StatusOverlay(this);
        root.addView(statusOverlay.getView(), StatusOverlay.layoutParams());

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
        statusOverlay.hide();
        scheduleLoadTimeout();
        webView.loadUrl(profile.baseUrl);
    }

    @Override
    public boolean openSettings() {
        return openSettings(null);
    }

    private boolean openSettings(String errorMessage) {
        if (settingsOpen) {
            return true;
        }
        settingsOpen = true;
        Intent intent = new Intent(this, SettingsActivity.class);
        if (errorMessage != null && errorMessage.length() > 0) {
            intent.putExtra(Constants.EXTRA_SETTINGS_ERROR_MESSAGE, errorMessage);
        }
        startActivityForResult(intent, Constants.REQUEST_SETTINGS);
        return true;
    }

    @Override
    public boolean goBack() {
        if (fullscreenVideoController.isShowing()) {
            fullscreenVideoController.hide();
            return true;
        }
        if (webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return false;
    }

    @Override
    public boolean togglePlayback() {
        webView.loadUrl("javascript:(function(){var v=document.querySelector('video');if(v){v.paused?v.play():v.pause();}})()");
        return true;
    }

    private void showStatus(String message) {
        statusOverlay.show(message);
    }

    private void enterFullScreen() {
        fullscreenVideoController.enterImmersiveMode();
    }

    @Override
    public void onPageLoadStarted() {
        progressBar.setVisibility(View.VISIBLE);
        scheduleLoadTimeout();
    }

    @Override
    public void onPageLoadFinished(WebView view, String url) {
        Logger.d("Page finished url=" + url + " title=" + view.getTitle() + " height=" + view.getContentHeight());
        if (isWebViewErrorPage(view)) {
            handleServerConnectionFailure("连接服务器失败");
            return;
        }
        progressBar.setVisibility(View.GONE);
        verifyPageReadiness(view);
    }

    private void verifyPageReadiness(final WebView view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view.evaluateJavascript(pageProbeScript(), new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    Logger.d("Page probe result=" + value);
                    String failureMessage = pageProbeFailureMessage(value);
                    if (failureMessage != null) {
                        handleServerConnectionFailure(failureMessage);
                    } else {
                        markPageUsable(view);
                    }
                }
            });
            return;
        }
        if (isBlankWebViewPage(view)) {
            loadTimeoutHandler.removeCallbacks(blankPageCheckRunnable);
            loadTimeoutHandler.postDelayed(blankPageCheckRunnable, Constants.PAGE_CONTENT_CHECK_DELAY_MS);
        } else {
            markPageUsable(view);
        }
    }

    private String pageProbeScript() {
        return "(function(){try{"
                + "var body=document.body;if(!body){return '0|0';}"
                + "function ignored(e){var t=e&&e.tagName;return t==='SCRIPT'||t==='STYLE'||t==='META'||t==='LINK'||t==='NOSCRIPT';}"
                + "function visible(e){if(!e||ignored(e)){return false;}var s=window.getComputedStyle?getComputedStyle(e):e.currentStyle;"
                + "return !s||((s.display!=='none')&&(s.visibility!=='hidden')&&(s.opacity!=='0'));}"
                + "function read(n){if(!n){return '';}if(n.nodeType===3){return visible(n.parentNode)?n.nodeValue:'';}"
                + "if(n.nodeType!==1||!visible(n)){return '';}var tag=n.tagName;"
                + "var value=(tag==='INPUT'||tag==='TEXTAREA')?(n.placeholder||n.value||n.name||n.id||''):(tag==='BUTTON'||tag==='A'?(n.innerText||n.textContent||n.value||n.title||''):'');"
                + "for(var i=0;i<n.childNodes.length;i++){value+=' '+read(n.childNodes[i]);}return value;}"
                + "var text=read(body);"
                + "text=text.replace(/\\s+/g,'');"
                + "var nodes=body.getElementsByTagName('*').length;"
                + "var modules=document.querySelectorAll('script[type=module]').length;"
                + "var root=document.getElementById('root');"
                + "var rootChildren=root?root.childNodes.length:-1;"
                + "return text.length+'|'+nodes+'|'+modules+'|'+rootChildren;"
                + "}catch(e){return '0|0';}})()";
    }

    private String pageProbeFailureMessage(String value) {
        if (value == null) {
            return "连接服务器失败：页面无响应";
        }
        String normalized = value.replace("\"", "").trim();
        String[] parts = normalized.split("\\|");
        int textLength = parts.length > 0 ? parseInt(parts[0]) : 0;
        int moduleScripts = parts.length > 2 ? parseInt(parts[2]) : 0;
        int rootChildren = parts.length > 3 ? parseInt(parts[3]) : -1;
        if (textLength > 0) {
            return null;
        }
        if (moduleScripts > 0 && rootChildren == 0) {
            return "当前 fnOS 页面依赖新版浏览器内核，Android 4 WebView 不支持 ES Module，登录页无法渲染。请使用兼容版服务端、代理转换页面，或换用新版 Android/WebView 环境。";
        }
        return "连接服务器失败：页面无响应";
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void markPageUsable(WebView view) {
        cancelLoadTimeout();
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
    public void onMainFrameError(String description, String failingUrl) {
        Logger.w("Main page load failed: " + description + " url=" + failingUrl);
        handleServerConnectionFailure("连接服务器失败：" + description);
    }

    private void handleServerConnectionFailure(String message) {
        cancelLoadTimeout();
        Logger.w(message);
        showStatus(message + "\n正在返回登录设置页");
        webView.stopLoading();
        openSettings(message);
    }

    private boolean isWebViewErrorPage(WebView view) {
        String title = view.getTitle();
        if (title == null) {
            return false;
        }
        title = title.toLowerCase();
        return title.indexOf("web page not available") >= 0
                || title.indexOf("网页无法打开") >= 0
                || title.indexOf("page not found") >= 0;
    }

    private boolean isBlankWebViewPage(WebView view) {
        return view.getContentHeight() <= 0;
    }

    private void scheduleLoadTimeout() {
        cancelLoadTimeout();
        loadTimeoutHandler.postDelayed(loadTimeoutRunnable, Constants.MAIN_PAGE_LOAD_TIMEOUT_MS);
    }

    private void cancelLoadTimeout() {
        loadTimeoutHandler.removeCallbacks(loadTimeoutRunnable);
        loadTimeoutHandler.removeCallbacks(blankPageCheckRunnable);
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
