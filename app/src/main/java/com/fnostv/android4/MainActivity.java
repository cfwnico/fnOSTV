package com.fnostv.android4;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
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
import com.fnostv.android4.net.FnosFileEntry;
import com.fnostv.android4.net.FnosFileList;
import com.fnostv.android4.net.FnosRpcClient;
import com.fnostv.android4.net.FnosRpcException;
import com.fnostv.android4.net.FnosSession;
import com.fnostv.android4.net.FnosSessionStore;
import com.fnostv.android4.tv.RemoteActions;
import com.fnostv.android4.tv.RemoteKeyHandler;
import com.fnostv.android4.ui.NativeFileBrowserView;
import com.fnostv.android4.ui.NativeHomeView;
import com.fnostv.android4.ui.NativeVideoPlayerView;
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

public final class MainActivity extends Activity implements WebViewEvents, RemoteActions, NativeHomeView.Listener, NativeFileBrowserView.Listener, NativeVideoPlayerView.Listener {
    private FrameLayout root;
    private WebView webView;
    private NativeHomeView nativeHomeView;
    private NativeFileBrowserView fileBrowserView;
    private NativeVideoPlayerView nativeVideoPlayerView;
    private ProgressBar progressBar;
    private StatusOverlay statusOverlay;
    private FullscreenVideoController fullscreenVideoController;
    private RemoteKeyHandler remoteKeyHandler;
    private ProfileStore store;
    private FnosSessionStore sessionStore;
    private ServerProfile profile;
    private boolean settingsOpen;
    private boolean nativeAuthRunning;
    private String currentFilePath = "";
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
        sessionStore = new FnosSessionStore(this);
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
        if (nativeVideoPlayerView != null && nativeVideoPlayerView.onKeyDown(keyCode, event)) {
            return true;
        }
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

        nativeHomeView = new NativeHomeView(this, this);
        root.addView(nativeHomeView.create(), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        fileBrowserView = new NativeFileBrowserView(this, this);
        root.addView(fileBrowserView.create(), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        nativeVideoPlayerView = new NativeVideoPlayerView(this, this);
        root.addView(nativeVideoPlayerView.create(), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

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
        nativeHomeView.hide();
        fileBrowserView.hide();
        nativeVideoPlayerView.hide();
        if (!profile.isReady()) {
            showStatus("首次使用请配置飞牛服务地址");
            openSettings();
            return;
        }
        statusOverlay.hide();
        if (profile.username.length() == 0 || profile.password.length() == 0) {
            openSettings("原生模式需要填写 fnOS 管理员账号和密码");
            return;
        }
        startNativeAuthentication();
    }

    private void startNativeAuthentication() {
        if (nativeAuthRunning) {
            return;
        }
        nativeAuthRunning = true;
        showStatus("正在连接 fnOS 原生 RPC 服务...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final NativeAuthResult result = authenticateNative();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        nativeAuthRunning = false;
                        if (result.success) {
                            showNativeHome();
                        } else {
                            handleServerConnectionFailure(result.message);
                        }
                    }
                });
            }
        }, "fnos-native-auth").start();
    }

    private NativeAuthResult authenticateNative() {
        try {
            FnosSession session = sessionStore.load();
            if (session.hasToken()) {
                try {
                    FnosRpcClient authClient = new FnosRpcClient(profile, sessionStore.getOrCreateDeviceId());
                    if (authClient.authToken(session)) {
                        Logger.d("Native RPC token auth succeeded");
                        return NativeAuthResult.success();
                    }
                } catch (FnosRpcException ex) {
                    Logger.w("Native RPC token auth failed, retrying login: " + ex.getMessage());
                }
                sessionStore.clear();
            }
            FnosRpcClient loginClient = new FnosRpcClient(profile, sessionStore.getOrCreateDeviceId());
            FnosSession newSession = loginClient.login();
            sessionStore.save(newSession);
            Logger.d("Native RPC login succeeded");
            return NativeAuthResult.success();
        } catch (FnosRpcException ex) {
            sessionStore.clear();
            Logger.w("Native RPC login failed: " + ex.getMessage());
            return NativeAuthResult.failure("原生登录失败：" + ex.getMessage());
        } catch (RuntimeException ex) {
            sessionStore.clear();
            Logger.w("Native RPC login crashed: " + ex.getMessage());
            return NativeAuthResult.failure("原生登录异常：" + ex.getMessage());
        }
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

    private void showNativeHome() {
        cancelLoadTimeout();
        progressBar.setVisibility(View.GONE);
        statusOverlay.hide();
        webView.setVisibility(View.GONE);
        fileBrowserView.hide();
        nativeVideoPlayerView.hide();
        nativeHomeView.show();
    }

    @Override
    public void onHomeAction(String action) {
        if (NativeHomeView.ACTION_SETTINGS.equals(action)) {
            openSettings();
            return;
        }
        if (NativeHomeView.ACTION_FILES.equals(action)) {
            openFileBrowser("");
            return;
        }
        if (NativeHomeView.ACTION_MEDIA.equals(action)) {
            showStatus("影视中心接口正在反查");
            return;
        }
        showStatus("最近播放正在接入");
    }

    @Override
    public void onFileEntrySelected(FnosFileEntry entry) {
        if (entry.directory) {
            openFileBrowser(entry.path);
            return;
        }
        playFileEntry(entry);
    }

    @Override
    public void onNativeVideoError(FnosFileEntry entry, String url, String reason) {
        Logger.w("Native video playback failed: " + reason
                + " file=" + (entry == null ? "" : entry.name)
                + " format=" + (entry == null ? "" : entry.formatLabel()));
        nativeVideoPlayerView.hide();
        openExternalPlayer(entry, url, "内置播放器无法播放"
                + (reason == null || reason.length() == 0 ? "" : "（" + reason + "）")
                + "，正在尝试外部播放器");
    }

    @Override
    public boolean goBack() {
        if (nativeVideoPlayerView.isVisible()) {
            nativeVideoPlayerView.hide();
            return true;
        }
        if (fullscreenVideoController.isShowing()) {
            fullscreenVideoController.hide();
            return true;
        }
        if (fileBrowserView.isVisible()) {
            if (currentFilePath.length() == 0) {
                showNativeHome();
            } else {
                openFileBrowser(parentPath(currentFilePath));
            }
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
        if (nativeVideoPlayerView.toggle()) {
            return true;
        }
        webView.loadUrl("javascript:(function(){var v=document.querySelector('video');if(v){v.paused?v.play():v.pause();}})()");
        return true;
    }

    private void showStatus(String message) {
        statusOverlay.show(message);
    }

    private void openFileBrowser(final String path) {
        currentFilePath = path == null ? "" : path;
        nativeHomeView.hide();
        webView.setVisibility(View.GONE);
        showStatus("正在加载目录...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final FileLoadResult result = loadFileList(currentFilePath);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (result.success) {
                            statusOverlay.hide();
                            fileBrowserView.show(result.list.path, result.list.entries);
                        } else {
                            showStatus(result.message);
                            nativeHomeView.show();
                        }
                    }
                });
            }
        }, "fnos-file-list").start();
    }

    private FileLoadResult loadFileList(String path) {
        try {
            FnosSession session = sessionStore.load();
            if (!session.hasToken()) {
                return FileLoadResult.failure("登录会话已失效");
            }
            FnosRpcClient client = new FnosRpcClient(profile, sessionStore.getOrCreateDeviceId());
            return FileLoadResult.success(client.listDir(session, path));
        } catch (FnosRpcException ex) {
            Logger.w("Native file list failed: " + ex.getMessage());
            return FileLoadResult.failure("文件库加载失败：" + ex.getMessage());
        } catch (RuntimeException ex) {
            Logger.w("Native file list crashed: " + ex.getMessage());
            return FileLoadResult.failure("文件库加载异常：" + ex.getMessage());
        }
    }

    private String parentPath(String path) {
        int index = path == null ? -1 : path.lastIndexOf('/');
        return index > 0 ? path.substring(0, index) : "";
    }

    private void playFileEntry(FnosFileEntry entry) {
        if (!entry.isVideo()) {
            showStatus("暂不支持打开该文件\n" + entry.name);
            return;
        }
        String url = entry.playbackUrl();
        if (url.length() > 0) {
            playResolvedUrl(entry, url);
            return;
        }
        resolveAndPlayFile(entry);
    }

    private void playResolvedUrl(FnosFileEntry entry, String url) {
        statusOverlay.hide();
        if (entry.canTryNativePlayback()) {
            nativeVideoPlayerView.play(entry, url);
            return;
        }
        openExternalPlayer(entry, url, "当前格式 " + entry.formatLabel()
                + " 超出 Android 4 内置播放器兼容范围，正在尝试外部播放器");
    }

    private void resolveAndPlayFile(final FnosFileEntry entry) {
        showStatus("正在准备播放直链...\n" + entry.name);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final PlaybackResolveResult result = resolvePlaybackUrl(entry);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (result.success) {
                            playResolvedUrl(entry, result.url);
                        } else {
                            showStatus(result.message);
                        }
                    }
                });
            }
        }, "fnos-playback-url").start();
    }

    private PlaybackResolveResult resolvePlaybackUrl(FnosFileEntry entry) {
        try {
            FnosSession session = sessionStore.load();
            if (!session.hasToken()) {
                return PlaybackResolveResult.failure("登录会话已失效");
            }
            FnosRpcClient client = new FnosRpcClient(profile, sessionStore.getOrCreateDeviceId());
            return PlaybackResolveResult.success(client.downloadUrl(session, entry.path));
        } catch (FnosRpcException ex) {
            Logger.w("Native playback url failed: " + ex.getMessage());
            return PlaybackResolveResult.failure("视频直链准备失败：" + ex.getMessage());
        } catch (RuntimeException ex) {
            Logger.w("Native playback url crashed: " + ex.getMessage());
            return PlaybackResolveResult.failure("视频直链准备异常：" + ex.getMessage());
        }
    }

    private void openExternalPlayer(FnosFileEntry entry, String url, String fallbackMessage) {
        if (url == null || url.length() == 0) {
            showStatus(fallbackMessage + "\n但缺少可用播放地址");
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), entry == null ? "video/*" : entry.mimeType());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (entry != null) {
                intent.putExtra("title", entry.name);
            }
            if (intent.resolveActivity(getPackageManager()) == null) {
                showStatus(fallbackMessage + "\n未找到外部播放器，请安装 VLC、MX Player 或系统兼容播放器");
                return;
            }
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            showStatus(fallbackMessage + "\n未找到可播放该视频的外部应用");
        } catch (RuntimeException ex) {
            Logger.w("External player failed: " + ex.getMessage());
            showStatus(fallbackMessage + "\n外部播放器启动失败");
        }
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

    private static final class NativeAuthResult {
        final boolean success;
        final String message;

        private NativeAuthResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        static NativeAuthResult success() {
            return new NativeAuthResult(true, "");
        }

        static NativeAuthResult failure(String message) {
            return new NativeAuthResult(false, message);
        }
    }

    private static final class FileLoadResult {
        final boolean success;
        final FnosFileList list;
        final String message;

        private FileLoadResult(boolean success, FnosFileList list, String message) {
            this.success = success;
            this.list = list;
            this.message = message;
        }

        static FileLoadResult success(FnosFileList list) {
            return new FileLoadResult(true, list, "");
        }

        static FileLoadResult failure(String message) {
            return new FileLoadResult(false, null, message);
        }
    }

    private static final class PlaybackResolveResult {
        final boolean success;
        final String url;
        final String message;

        private PlaybackResolveResult(boolean success, String url, String message) {
            this.success = success;
            this.url = url == null ? "" : url;
            this.message = message;
        }

        static PlaybackResolveResult success(String url) {
            return new PlaybackResolveResult(true, url, "");
        }

        static PlaybackResolveResult failure(String message) {
            return new PlaybackResolveResult(false, "", message);
        }
    }
}
