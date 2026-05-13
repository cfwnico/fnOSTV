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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.fnostv.android4.config.ProfileStore;
import com.fnostv.android4.config.ServerProfile;
import com.fnostv.android4.media.MediaIndexStore;
import com.fnostv.android4.media.MediaLibraryClassifier;
import com.fnostv.android4.media.MediaLibraryStore;
import com.fnostv.android4.net.FavoriteStore;
import com.fnostv.android4.net.FnosFileEntry;
import com.fnostv.android4.net.FnosFileList;
import com.fnostv.android4.net.FnosMediaCounts;
import com.fnostv.android4.net.FnosPlaybackSource;
import com.fnostv.android4.net.FnosRestClient;
import com.fnostv.android4.net.FnosRpcClient;
import com.fnostv.android4.net.FnosRpcException;
import com.fnostv.android4.net.FnosSession;
import com.fnostv.android4.net.FnosSessionStore;
import com.fnostv.android4.net.RecentPlaybackStore;
import com.fnostv.android4.tv.RemoteActions;
import com.fnostv.android4.tv.RemoteKeyHandler;
import com.fnostv.android4.ui.HomePosterSlots;
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

import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity implements WebViewEvents, RemoteActions, NativeHomeView.Listener, NativeFileBrowserView.Listener, NativeVideoPlayerView.Listener {
    private static final int BROWSER_MODE_FILES = 0;
    private static final int BROWSER_MODE_RECENT = 1;
    private static final int BROWSER_MODE_MEDIA = 2;
    private static final int BROWSER_MODE_FAVORITES = 3;
    private static final int BROWSER_MODE_CATEGORY = 4;

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
    private RecentPlaybackStore recentPlaybackStore;
    private FavoriteStore favoriteStore;
    private MediaLibraryStore mediaLibraryStore;
    private MediaIndexStore mediaIndexStore;
    private ServerProfile profile;
    private boolean settingsOpen;
    private boolean nativeAuthRunning;
    private String currentFilePath = "";
    private int browserMode = BROWSER_MODE_FILES;
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
        recentPlaybackStore = new RecentPlaybackStore(this);
        favoriteStore = new FavoriteStore(this);
        mediaLibraryStore = new MediaLibraryStore(this);
        mediaIndexStore = new MediaIndexStore(this);
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
        fileBrowserView.setPosterBaseUrl(profile.baseUrl);
        fileBrowserView.setPosterAuthorizationToken("");
        nativeHomeView.setPosterBaseUrl(profile.baseUrl);
        nativeHomeView.setPosterAuthorizationToken("");
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
        return openSettings(null, null);
    }

    private boolean openSettings(String errorMessage) {
        return openSettings(errorMessage, null);
    }

    private boolean openSettings(String errorMessage, String page) {
        if (settingsOpen) {
            return true;
        }
        settingsOpen = true;
        Intent intent = new Intent(this, SettingsActivity.class);
        if (errorMessage != null && errorMessage.length() > 0) {
            intent.putExtra(Constants.EXTRA_SETTINGS_ERROR_MESSAGE, errorMessage);
        }
        if (page != null && page.length() > 0) {
            intent.putExtra(Constants.EXTRA_SETTINGS_PAGE, page);
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
        nativeHomeView.updateUser(profile == null ? "" : profile.username, profile == null ? "" : profile.username, true);
        updateHomeCounts();
        nativeHomeView.show();
    }

    @Override
    public void onHomeAction(String action) {
        if (NativeHomeView.ACTION_HOME.equals(action)) {
            showNativeHome();
            return;
        }
        if (NativeHomeView.ACTION_FAVORITES.equals(action)) {
            openFavorites();
            return;
        }
        if (NativeHomeView.ACTION_ALL.equals(action)) {
            openMediaCenter("");
            return;
        }
        if (NativeHomeView.ACTION_MOVIES.equals(action)) {
            openCategory("电影", NativeHomeView.ACTION_MOVIES);
            return;
        }
        if (NativeHomeView.ACTION_TV.equals(action)) {
            openCategory("电视节目", NativeHomeView.ACTION_TV);
            return;
        }
        if (NativeHomeView.ACTION_OTHER.equals(action)) {
            openCategory("其他", NativeHomeView.ACTION_OTHER);
            return;
        }
        if (NativeHomeView.ACTION_SEARCH.equals(action)) {
            openSearchDialog();
            return;
        }
        if (NativeHomeView.ACTION_USER.equals(action)) {
            openSettings(null, "users");
            return;
        }
        if (NativeHomeView.ACTION_USER_PASSWORD.equals(action)) {
            openSettings(null, "password");
            return;
        }
        if (NativeHomeView.ACTION_USER_PREFERENCE.equals(action)) {
            openSettings(null, "preference");
            return;
        }
        if (NativeHomeView.ACTION_USER_APPEARANCE.equals(action)) {
            openSettings(null, "appearance");
            return;
        }
        if (NativeHomeView.ACTION_HELP.equals(action)) {
            showStatus("帮助中心入口已复刻，当前 Android 4 版本暂未接入在线帮助页。");
            return;
        }
        if (NativeHomeView.ACTION_ABOUT.equals(action)) {
            showStatus("飞牛影视 Android4 复刻版\n已兼容原生播放、媒体库和设置页。");
            return;
        }
        if (NativeHomeView.ACTION_LOGOUT.equals(action)) {
            sessionStore.clear();
            openSettings("已退出登录，请重新登录。");
            return;
        }
        if (NativeHomeView.ACTION_SETTINGS.equals(action)) {
            openSettings();
            return;
        }
        if (NativeHomeView.ACTION_FILES.equals(action)) {
            openFileBrowser("");
            return;
        }
        if (NativeHomeView.ACTION_RECENT.equals(action)) {
            openRecentPlayback();
            return;
        }
        if (NativeHomeView.ACTION_MEDIA.equals(action)) {
            openMediaCenter("");
            return;
        }
        showStatus("暂不支持该入口：" + action);
    }

    @Override
    public void onFileEntrySelected(FnosFileEntry entry) {
        if (entry.directory && browserMode == BROWSER_MODE_MEDIA) {
            openMediaCenter(entry.path);
            return;
        }
        if (entry.directory && browserMode == BROWSER_MODE_RECENT) {
            return;
        }
        if (entry.directory) {
            openFileBrowser(entry.path);
            return;
        }
        playFileEntry(entry);
    }

    @Override
    public void onFileFavoriteToggled(FnosFileEntry entry) {
        boolean added = favoriteStore.toggle(entry);
        updateHomeCounts();
        Toast.makeText(this, added ? "已加入收藏：" + entry.name : "已取消收藏：" + entry.name, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean isFileFavorite(FnosFileEntry entry) {
        return favoriteStore.isFavorite(entry);
    }

    @Override
    public void onBrowserAction(String action) {
        onHomeAction(action);
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
            if (browserMode == BROWSER_MODE_FAVORITES || browserMode == BROWSER_MODE_CATEGORY) {
                showNativeHome();
                return true;
            }
            if (browserMode == BROWSER_MODE_RECENT) {
                showNativeHome();
                return true;
            }
            if (browserMode == BROWSER_MODE_MEDIA) {
                if (currentFilePath.length() == 0) {
                    showNativeHome();
                } else {
                    openMediaCenter(parentPath(currentFilePath));
                }
                return true;
            }
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
        browserMode = BROWSER_MODE_FILES;
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

    private void openRecentPlayback() {
        browserMode = BROWSER_MODE_RECENT;
        currentFilePath = "";
        nativeHomeView.hide();
        webView.setVisibility(View.GONE);
        showStatus("正在加载继续观看...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final MediaLoadResult result = loadRecentPlayback();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusOverlay.hide();
                        fileBrowserView.showCustom(result.title, result.subtitle, result.list.entries, result.sortEntries);
                    }
                });
            }
        }, "fnos-rest-recent").start();
    }

    private void openFavorites() {
        browserMode = BROWSER_MODE_FAVORITES;
        currentFilePath = "";
        nativeHomeView.hide();
        webView.setVisibility(View.GONE);
        showStatus("正在加载收藏...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final MediaLoadResult result = loadFavorites();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusOverlay.hide();
                        fileBrowserView.showCustom(result.title, result.subtitle, result.list.entries, result.sortEntries);
                    }
                });
            }
        }, "fnos-rest-favorites").start();
    }

    private void openCategory(final String title, final String category) {
        browserMode = BROWSER_MODE_CATEGORY;
        currentFilePath = "";
        nativeHomeView.hide();
        webView.setVisibility(View.GONE);
        showStatus("正在加载" + title + "...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final MediaLoadResult result = loadCategory(title, category);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusOverlay.hide();
                        fileBrowserView.showCustom(result.title, result.subtitle, result.list.entries, result.sortEntries);
                    }
                });
            }
        }, "fnos-rest-category").start();
    }

    private void openSearchDialog() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("输入片名或路径关键词");
        input.setTextColor(0xFFFFFFFF);
        input.setHintTextColor(0xFF8C96A3);
        input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this)
                .setTitle("搜索")
                .setView(input)
                .setPositiveButton("搜索", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openSearchResults(input.getText().toString());
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void openSearchResults(String keyword) {
        browserMode = BROWSER_MODE_CATEGORY;
        currentFilePath = "";
        nativeHomeView.hide();
        webView.setVisibility(View.GONE);
        statusOverlay.hide();
        List<FnosFileEntry> entries = filterSearch(knownMediaEntries(), keyword);
        fileBrowserView.showCustom("搜索", keyword == null || keyword.length() == 0 ? "全部本机记录" : "关键词：" + keyword, entries, false);
    }

    private void openMediaCenter(final String path) {
        browserMode = BROWSER_MODE_MEDIA;
        currentFilePath = path == null ? "" : path;
        nativeHomeView.hide();
        webView.setVisibility(View.GONE);
        showStatus("正在加载影视入口...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final MediaLoadResult result = loadMediaCenter(currentFilePath);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
        if (result.success) {
            statusOverlay.hide();
            fileBrowserView.showCustom(result.title, result.subtitle, result.list.entries, result.sortEntries);
                        } else {
                            showStatus(result.message);
                            nativeHomeView.show();
                        }
                    }
                });
            }
        }, "fnos-media-center").start();
    }

    private MediaLoadResult loadMediaCenter(String path) {
        if (path == null || path.length() == 0) {
            try {
                FnosRestClient client = newRestClient();
                FnosFileList libraries = client.mediaLibraries();
                if (libraries.entries.size() > 0) {
                    FnosFileEntry first = libraries.entries.get(0);
                    FnosFileList list = client.mediaItems(first.path, NativeHomeView.ACTION_ALL, 50);
                    return MediaLoadResult.success(first.name.length() == 0 ? "影视大全" : first.name, "fnOS 影视媒体库", list, false);
                }
                FnosFileList list = client.mediaItems("", NativeHomeView.ACTION_ALL, 50);
                if (list.entries.size() > 0) {
                    return MediaLoadResult.success("影视大全", "fnOS 影视条目", list, false);
                }
            } catch (FnosRpcException ex) {
                Logger.w("Media center REST API unavailable: " + ex.getMessage());
            } catch (RuntimeException ex) {
                Logger.w("Media center REST API crashed: " + ex.getMessage());
            }
            List<FnosFileEntry> indexed = mediaIndexStore.list();
            if (indexed.size() > 0) {
                return MediaLoadResult.success("影视大全", "本地媒体库索引", new FnosFileList("mediaIndex", indexed), true);
            }
            try {
                FnosSession session = sessionStore.load();
                if (session.hasToken()) {
                    FnosRpcClient client = new FnosRpcClient(profile, sessionStore.getOrCreateDeviceId());
                    FnosFileList list = client.mediaCenterEntries(session);
                    if (list.entries.size() > 0) {
                        return MediaLoadResult.success("影视中心", "fnOS mediaCenter", list, false);
                    }
                }
            } catch (FnosRpcException ex) {
                Logger.w("Media center native API unavailable: " + ex.getMessage());
            } catch (RuntimeException ex) {
                Logger.w("Media center native API crashed: " + ex.getMessage());
            }
        } else if (isRestMediaPath(path)) {
            try {
                FnosFileList list = newRestClient().mediaItems(path, NativeHomeView.ACTION_ALL, 50);
                return MediaLoadResult.success("影视大全", path, list, false);
            } catch (FnosRpcException ex) {
                Logger.w("Media center REST child unavailable: " + ex.getMessage());
            } catch (RuntimeException ex) {
                Logger.w("Media center REST child crashed: " + ex.getMessage());
            }
        }
        FileLoadResult fallback = loadFileList(path);
        if (fallback.success) {
            String subtitle = path == null || path.length() == 0 ? "影视中心 API 未稳定，已进入文件模式" : path;
            return MediaLoadResult.success("影视入口（文件模式）", subtitle, fallback.list, true);
        }
        return MediaLoadResult.failure(fallback.message);
    }

    private MediaLoadResult loadCategory(String title, String category) {
        try {
            FnosFileList list = newRestClient().mediaItems("", category, 50);
            return MediaLoadResult.success(title, list.entries.size() == 0 ? "暂无内容" : "fnOS 影视服务", list, false);
        } catch (FnosRpcException ex) {
            Logger.w("REST category failed: " + ex.getMessage());
        } catch (RuntimeException ex) {
            Logger.w("REST category crashed: " + ex.getMessage());
        }
        List<FnosFileEntry> entries = filterCategory(knownMediaEntries(), category);
        return MediaLoadResult.success(title, entries.size() == 0 ? "暂无内容，先从影视大全或文件库播放/收藏媒体" : "来自最近播放和收藏", new FnosFileList(category, entries), false);
    }

    private MediaLoadResult loadFavorites() {
        try {
            FnosFileList list = newRestClient().favoriteItems();
            return MediaLoadResult.success("收藏", list.entries.size() == 0 ? "暂无收藏" : "fnOS 收藏", list, false);
        } catch (FnosRpcException ex) {
            Logger.w("REST favorites failed: " + ex.getMessage());
        } catch (RuntimeException ex) {
            Logger.w("REST favorites crashed: " + ex.getMessage());
        }
        List<FnosFileEntry> entries = favoriteStore.list();
        return MediaLoadResult.success("收藏", entries.size() == 0 ? "暂无收藏，文件列表中按右键可收藏" : "本机收藏", new FnosFileList("favorite", entries), false);
    }

    private MediaLoadResult loadRecentPlayback() {
        try {
            FnosFileList list = newRestClient().recentItems();
            return MediaLoadResult.success("继续观看", list.entries.size() == 0 ? "暂无播放记录" : "fnOS 继续观看", list, false);
        } catch (FnosRpcException ex) {
            Logger.w("REST recent failed: " + ex.getMessage());
        } catch (RuntimeException ex) {
            Logger.w("REST recent crashed: " + ex.getMessage());
        }
        List<FnosFileEntry> entries = recentPlaybackStore.list();
        return MediaLoadResult.success("最近播放", entries.size() == 0 ? "暂无播放记录" : "本机播放记录", new FnosFileList("recent", entries), false);
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

    private FnosRestClient newRestClient() throws FnosRpcException {
        if (profile == null || !profile.isReady()) {
            throw new FnosRpcException("服务配置不可用");
        }
        return new FnosRestClient(profile);
    }

    private boolean isRestMediaPath(String path) {
        if (path == null || path.length() == 0) {
            return false;
        }
        return path.indexOf('/') < 0 && path.indexOf('\\') < 0 && path.indexOf(':') < 0;
    }

    private void updateHomeCounts() {
        int libraryCount = mediaLibraryStore.listOrSeedDefault().size();
        List<FnosFileEntry> known = knownMediaEntries();
        List<FnosFileEntry> favorites = favoriteStore.list();
        List<FnosFileEntry> recent = recentPlaybackStore.list();
        nativeHomeView.updatePosterCards(HomePosterSlots.from(known, recent, favorites));
        nativeHomeView.updateCounts(
                favorites.size(),
                libraryCount,
                known.size(),
                filterCategory(known, NativeHomeView.ACTION_MOVIES).size(),
                filterCategory(known, NativeHomeView.ACTION_TV).size(),
                filterCategory(known, NativeHomeView.ACTION_OTHER).size(),
                recent.size());
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FnosRestClient client = newRestClient();
                    final FnosMediaCounts counts = client.mediaCounts();
                    final FnosFileList recentList = client.recentItems();
                    final FnosFileList favoriteList = client.favoriteItems();
                    final List<FnosFileEntry> mediaEntries = loadHomeMediaEntries(client);
                    final String posterToken = client.authorizationToken();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            nativeHomeView.setPosterAuthorizationToken(posterToken);
                            fileBrowserView.setPosterAuthorizationToken(posterToken);
                            nativeHomeView.updateCounts(
                                    counts.favoriteCount,
                                    counts.libraryCount,
                                    counts.totalCount,
                                    counts.movieCount,
                                    counts.tvCount,
                                    counts.otherCount,
                                    recentList.entries.size());
                            nativeHomeView.updatePosterCards(HomePosterSlots.from(mediaEntries, recentList.entries, favoriteList.entries));
                        }
                    });
                } catch (FnosRpcException ex) {
                    Logger.w("REST home counts failed: " + ex.getMessage());
                } catch (RuntimeException ex) {
                    Logger.w("REST home counts crashed: " + ex.getMessage());
                }
            }
        }, "fnos-rest-home-counts").start();
    }

    private List<FnosFileEntry> loadHomeMediaEntries(FnosRestClient client) throws FnosRpcException {
        FnosFileList libraries = client.mediaLibraries();
        if (libraries.entries.size() > 0) {
            FnosFileEntry first = libraries.entries.get(0);
            return client.mediaItems(first.path, NativeHomeView.ACTION_ALL, 12).entries;
        }
        return client.mediaItems("", NativeHomeView.ACTION_ALL, 12).entries;
    }

    private List<FnosFileEntry> knownMediaEntries() {
        List<FnosFileEntry> values = new ArrayList<FnosFileEntry>();
        appendUnique(values, mediaIndexStore.list());
        appendUnique(values, recentPlaybackStore.list());
        appendUnique(values, favoriteStore.list());
        return values;
    }

    private void appendUnique(List<FnosFileEntry> target, List<FnosFileEntry> source) {
        if (source == null) {
            return;
        }
        for (int i = 0; i < source.size(); i++) {
            FnosFileEntry entry = source.get(i);
            if (entry != null && entry.isVideo() && !containsPath(target, entry.path)) {
                target.add(entry);
            }
        }
    }

    private boolean containsPath(List<FnosFileEntry> entries, String path) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).path.equals(path)) {
                return true;
            }
        }
        return false;
    }

    private List<FnosFileEntry> filterCategory(List<FnosFileEntry> entries, String category) {
        List<FnosFileEntry> filtered = new ArrayList<FnosFileEntry>();
        for (int i = 0; i < entries.size(); i++) {
            FnosFileEntry entry = entries.get(i);
            if (NativeHomeView.ACTION_MOVIES.equals(category) && isMovie(entry)) {
                filtered.add(entry);
            } else if (NativeHomeView.ACTION_TV.equals(category) && isTv(entry)) {
                filtered.add(entry);
            } else if (NativeHomeView.ACTION_OTHER.equals(category) && !isMovie(entry) && !isTv(entry)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    private List<FnosFileEntry> filterSearch(List<FnosFileEntry> entries, String keyword) {
        String value = keyword == null ? "" : keyword.toLowerCase();
        List<FnosFileEntry> filtered = new ArrayList<FnosFileEntry>();
        for (int i = 0; i < entries.size(); i++) {
            FnosFileEntry entry = entries.get(i);
            if (value.length() == 0
                    || entry.name.toLowerCase().indexOf(value) >= 0
                    || entry.path.toLowerCase().indexOf(value) >= 0) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    private boolean isMovie(FnosFileEntry entry) {
        if (entry == null || !entry.isVideo()) {
            return false;
        }
        return MediaLibraryClassifier.inferCategory(entry.name, entry.path).equals("movie");
    }

    private boolean isTv(FnosFileEntry entry) {
        if (entry == null) {
            return false;
        }
        return MediaLibraryClassifier.inferCategory(entry.name, entry.path).equals("tv");
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
        resolveAndPlaySources(entry);
    }

    private void playResolvedUrl(FnosFileEntry entry, String url) {
        statusOverlay.hide();
        recentPlaybackStore.remember(entry);
        if (entry.canTryNativePlayback()) {
            nativeVideoPlayerView.play(entry, url);
            return;
        }
        openExternalPlayer(entry, url, "当前格式 " + entry.formatLabel()
                + " 超出 Android 4 内置播放器兼容范围，正在尝试外部播放器");
    }

    private void playResolvedSources(FnosFileEntry entry, List<FnosPlaybackSource> sources) {
        statusOverlay.hide();
        recentPlaybackStore.remember(entry);
        if (entry.canTryNativePlayback()) {
            nativeVideoPlayerView.play(entry, sources);
            return;
        }
        FnosPlaybackSource first = sources == null || sources.size() == 0 ? null : sources.get(0);
        openExternalPlayer(entry, first == null ? "" : first.url, "当前格式 " + entry.formatLabel()
                + " 超出 Android 4 内置播放器兼容范围，正在尝试外部播放器");
    }

    private void resolveAndPlaySources(final FnosFileEntry entry) {
        showStatus("正在准备播放源...\n" + entry.name);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final PlaybackSourcesResult result = resolvePlaybackSources(entry);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (result.success) {
                            playResolvedSources(entry, result.sources);
                        } else {
                            showStatus(result.message);
                        }
                    }
                });
            }
        }, "fnos-playback-sources").start();
    }

    private PlaybackSourcesResult resolvePlaybackSources(FnosFileEntry entry) {
        try {
            FnosSession session = sessionStore.load();
            if (!session.hasToken()) {
                return PlaybackSourcesResult.failure("登录会话已失效");
            }
            FnosRpcClient client = new FnosRpcClient(profile, sessionStore.getOrCreateDeviceId());
            return PlaybackSourcesResult.success(client.playbackSources(session, entry));
        } catch (FnosRpcException ex) {
            Logger.w("Native playback sources failed: " + ex.getMessage());
            return PlaybackSourcesResult.failure("播放源准备失败：" + ex.getMessage());
        } catch (RuntimeException ex) {
            Logger.w("Native playback sources crashed: " + ex.getMessage());
            return PlaybackSourcesResult.failure("播放源准备异常：" + ex.getMessage());
        }
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

    private static final class MediaLoadResult {
        final boolean success;
        final String title;
        final String subtitle;
        final FnosFileList list;
        final boolean sortEntries;
        final String message;

        private MediaLoadResult(boolean success, String title, String subtitle, FnosFileList list, boolean sortEntries, String message) {
            this.success = success;
            this.title = title;
            this.subtitle = subtitle;
            this.list = list;
            this.sortEntries = sortEntries;
            this.message = message;
        }

        static MediaLoadResult success(String title, String subtitle, FnosFileList list, boolean sortEntries) {
            return new MediaLoadResult(true, title, subtitle, list, sortEntries, "");
        }

        static MediaLoadResult failure(String message) {
            return new MediaLoadResult(false, "", "", null, true, message);
        }
    }

    private static final class PlaybackSourcesResult {
        final boolean success;
        final List<FnosPlaybackSource> sources;
        final String message;

        private PlaybackSourcesResult(boolean success, List<FnosPlaybackSource> sources, String message) {
            this.success = success;
            this.sources = sources == null ? new ArrayList<FnosPlaybackSource>() : sources;
            this.message = message;
        }

        static PlaybackSourcesResult success(List<FnosPlaybackSource> sources) {
            return new PlaybackSourcesResult(true, sources, "");
        }

        static PlaybackSourcesResult failure(String message) {
            return new PlaybackSourcesResult(false, null, message);
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
