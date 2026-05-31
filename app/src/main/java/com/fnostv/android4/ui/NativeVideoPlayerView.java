package com.fnostv.android4.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.fnostv.android4.net.FnosFileEntry;
import com.fnostv.android4.net.FnosPlaybackSource;
import com.fnostv.android4.player.IjkPlayerEngine;
import com.fnostv.android4.player.NumberSeekShortcut;
import com.fnostv.android4.player.PlayerEngine;
import com.fnostv.android4.player.PlaybackOptions;
import com.fnostv.android4.player.PlaybackStrategy;
import com.fnostv.android4.player.VlcPlayerEngine;
import com.fnostv.android4.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NativeVideoPlayerView {
    public interface Listener {
        void onNativeVideoError(FnosFileEntry entry, String url, String reason);
    }

    private static final int SEEK_STEP_MS = 10000;
    private static final int LARGE_SEEK_STEP_MS = 30000;
    private static final int CONTROL_HIDE_DELAY_MS = 5000;
    private static final int PROGRESS_INTERVAL_MS = 1000;
    private static final int PREPARE_TIMEOUT_MS = 20000;
    private static final int SEEK_COMMIT_DELAY_MS = 260;
    private static final int SEEK_SETTLE_DELAY_MS = 1200;
    private static final int MODE_FIT = 0;
    private static final int MODE_FILL = 1;
    private static final float[] SPEEDS = new float[]{1.0f, 1.25f, 1.5f, 2.0f, 0.75f};

    private final Context context;
    private final Listener listener;
    private final Handler handler = new Handler();
    private final List<FnosPlaybackSource> playbackSources = new ArrayList<FnosPlaybackSource>();
    private FrameLayout view;
    private PlaybackSurfaceView videoView;
    private ProgressBar loadingView;
    private TextView titleView;
    private LinearLayout controlBar;
    private SeekBar seekBar;
    private TextView timeView;
    private TextView playStateView;
    private TextView speedView;
    private TextView pictureView;
    private TextView resolutionView;
    private TextView statusView;
    private TextView hintView;
    private SurfaceHolder surfaceHolder;
    private FnosFileEntry currentEntry;
    private String currentUrl = "";
    private PlayerEngine currentPlayer;
    private PlaybackOptions playbackOptions;
    private boolean surfaceReady;
    private boolean dragging;
    private boolean seeking;
    private boolean suppressErrors;
    private boolean prepared;
    private boolean preferHardwareCodec;
    private boolean retriedSoftwareCodec;
    private boolean retriedIjkEngine;
    private boolean firstFrameSeen;
    private long prepareStartedAtMs;
    private int bufferingCount;
    private int speedIndex;
    private int sourceIndex;
    private int pendingSeekMs = -1;
    private int pendingSeekRequestMs = -1;
    private int pictureMode = MODE_FIT;
    private int videoWidth;
    private int videoHeight;

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            if (isVisible()) {
                handler.postDelayed(this, PROGRESS_INTERVAL_MS);
            }
        }
    };
    private final Runnable hideControlsRunnable = new Runnable() {
        @Override
        public void run() {
            hideControls();
        }
    };
    private final Runnable prepareTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (isVisible() && !prepared && !suppressErrors) {
                notifyPlaybackError("播放器准备超时");
            }
        }
    };
    private final Runnable seekCommitRunnable = new Runnable() {
        @Override
        public void run() {
            commitPendingSeek();
        }
    };
    private final Runnable seekSettledRunnable = new Runnable() {
        @Override
        public void run() {
            seeking = false;
            if (prepared) {
                loadingView.setVisibility(View.GONE);
            }
            updateProgress();
        }
    };

    public NativeVideoPlayerView(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public View create() {
        view = new FrameLayout(context);
        view.setBackgroundColor(Color.BLACK);
        view.setVisibility(View.GONE);

        videoView = new PlaybackSurfaceView(context);
        videoView.setResizeMode(pictureMode);
        videoView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                surfaceHolder = holder;
                surfaceReady = true;
                if (currentPlayer != null) {
                    currentPlayer.attachSurface(holder, videoView.getWidth(), videoView.getHeight());
                    currentPlayer.setFillMode(pictureMode == MODE_FILL);
                } else if (currentUrl.length() > 0 && view.getVisibility() == View.VISIBLE) {
                    startPlayer();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                surfaceHolder = holder;
                if (currentPlayer != null) {
                    currentPlayer.resizeSurface(width, height);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                surfaceReady = false;
                surfaceHolder = null;
                if (currentPlayer != null) {
                    currentPlayer.detachSurface();
                }
            }
        });
        view.addView(videoView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER));

        titleView = new TextView(context);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(16);
        titleView.setSingleLine(true);
        titleView.setBackgroundColor(0x99000000);
        titleView.setPadding(dp(18), dp(10), dp(18), dp(10));
        view.addView(titleView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP));

        controlBar = new LinearLayout(context);
        controlBar.setOrientation(LinearLayout.VERTICAL);
        controlBar.setBackgroundColor(0xCC000000);
        controlBar.setPadding(dp(18), dp(10), dp(18), dp(12));

        LinearLayout controlRow = new LinearLayout(context);
        controlRow.setOrientation(LinearLayout.HORIZONTAL);
        controlRow.setGravity(Gravity.CENTER_VERTICAL);
        playStateView = controlText("播放/暂停");
        timeView = controlText("00:00 / 00:00");
        speedView = controlText("倍速 1.0x");
        pictureView = controlText("画面 铺满");
        resolutionView = controlText("清晰度 原画");
        controlRow.addView(playStateView);
        controlRow.addView(timeView);
        controlRow.addView(speedView);
        controlRow.addView(pictureView);
        controlRow.addView(resolutionView);
        statusView = controlText(playbackStatusLabel());
        statusView.setTextColor(0xFFBFD8FF);
        statusView.setTextSize(12);

        seekBar = new SeekBar(context);
        seekBar.setMax(1000);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) {
                    updateTimeLabel(progressToPosition(progress), duration());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
                dragging = true;
                showControls();
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                dragging = false;
                requestSeekTo(progressToPosition(bar.getProgress()));
                showControlsTemporarily();
            }
        });

        hintView = new TextView(context);
        hintView.setTextColor(0xFFE6EDF3);
        hintView.setTextSize(13);
        hintView.setSingleLine(true);
        hintView.setText("左右快退/快进，确认暂停，菜单倍速，上键画面，下键清晰度");

        controlBar.addView(controlRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        controlBar.addView(statusView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        controlBar.addView(seekBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        controlBar.addView(hintView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        view.addView(controlBar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM));

        loadingView = new ProgressBar(context);
        view.addView(loadingView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));
        return view;
    }

    public void play(FnosFileEntry entry, String url) {
        List<FnosPlaybackSource> sources = new ArrayList<FnosPlaybackSource>();
        sources.add(new FnosPlaybackSource("原画", url));
        play(entry, sources);
    }

    public void play(FnosFileEntry entry, List<FnosPlaybackSource> sources) {
        currentEntry = entry;
        playbackSources.clear();
        if (sources != null) {
            for (int i = 0; i < sources.size(); i++) {
                FnosPlaybackSource source = sources.get(i);
                if (source != null && source.isValid()) {
                    playbackSources.add(source);
                }
            }
        }
        if (playbackSources.size() == 0) {
            playbackSources.add(new FnosPlaybackSource("原画", ""));
        }
        sourceIndex = 0;
        currentUrl = currentSource().url;
        playbackOptions = buildPlaybackOptions(currentUrl);
        pendingSeekMs = -1;
        pendingSeekRequestMs = -1;
        seeking = false;
        suppressErrors = false;
        prepared = false;
        preferHardwareCodec = entry != null && entry.prefersHardwarePlayback();
        retriedSoftwareCodec = false;
        retriedIjkEngine = false;
        firstFrameSeen = false;
        prepareStartedAtMs = 0L;
        bufferingCount = 0;
        speedIndex = 0;
        videoWidth = 0;
        videoHeight = 0;
        titleView.setText(entry == null ? "" : entry.name);
        loadingView.setVisibility(View.VISIBLE);
        view.setVisibility(View.VISIBLE);
        view.setKeepScreenOn(true);
        enterImmersiveMode();
        videoView.setResizeMode(pictureMode);
        releasePlayer(false);
        if (surfaceReady) {
            startPlayer();
        }
        handler.removeCallbacks(progressRunnable);
        handler.post(progressRunnable);
        handler.removeCallbacks(prepareTimeoutRunnable);
        handler.postDelayed(prepareTimeoutRunnable, PREPARE_TIMEOUT_MS);
        updateControlText();
        showControlsTemporarily();
    }

    public boolean isVisible() {
        return view != null && view.getVisibility() == View.VISIBLE;
    }

    public boolean toggle() {
        if (!isVisible() || currentPlayer == null) {
            return false;
        }
        if (currentPlayer.isPlaying()) {
            currentPlayer.pause();
            showHint("已暂停");
        } else {
            currentPlayer.start();
            showHint("继续播放");
        }
        updateControlText();
        showControlsTemporarily();
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!isVisible()) {
            return false;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            return toggle();
        }
        int numberSeekPercent = NumberSeekShortcut.percentForKey(keyCode);
        if (numberSeekPercent >= 0) {
            seekToPercent(numberSeekPercent);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
            seekBy(keyCode == KeyEvent.KEYCODE_MEDIA_REWIND ? -LARGE_SEEK_STEP_MS : -SEEK_STEP_MS);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
            seekBy(keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD ? LARGE_SEEK_STEP_MS : SEEK_STEP_MS);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            cyclePictureMode();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            cycleQuality();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            cycleSpeed();
            return true;
        }
        showControlsTemporarily();
        return false;
    }

    public void hide() {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            return;
        }
        handler.removeCallbacks(progressRunnable);
        handler.removeCallbacks(hideControlsRunnable);
        handler.removeCallbacks(prepareTimeoutRunnable);
        handler.removeCallbacks(seekCommitRunnable);
        handler.removeCallbacks(seekSettledRunnable);
        suppressErrors = true;
        releasePlayer(true);
        view.setVisibility(View.GONE);
        view.setKeepScreenOn(false);
        currentEntry = null;
        currentUrl = "";
        playbackSources.clear();
        sourceIndex = 0;
        pendingSeekMs = -1;
        pendingSeekRequestMs = -1;
        prepared = false;
        seeking = false;
    }

    private void startPlayer() {
        if (currentUrl.length() == 0 || !surfaceReady) {
            return;
        }
        try {
            final PlayerEngine player = createPlayerEngine();
            currentPlayer = player;
            player.attachSurface(surfaceHolder, videoView.getWidth(), videoView.getHeight());
            player.setFillMode(pictureMode == MODE_FILL);
            player.setListener(new PlayerEngine.Listener() {
                @Override
                public void onPrepared(int width, int height, int durationMs) {
                    handler.removeCallbacks(prepareTimeoutRunnable);
                    prepared = true;
                    videoWidth = width;
                    videoHeight = height;
                    videoView.setVideoSize(videoWidth, videoHeight);
                    loadingView.setVisibility(View.GONE);
                    player.start();
                    if (pendingSeekMs > 0) {
                        player.seekTo(pendingSeekMs);
                        pendingSeekMs = -1;
                    }
                    applyPlaybackSpeed(SPEEDS[speedIndex]);
                    Logger.d(player.name() + " prepared file=" + fileName() + " format=" + formatLabel()
                            + " hw=" + preferHardwareCodec
                            + " options=" + optionsLabel()
                            + " source=" + currentSource().label
                            + " size=" + videoWidth + "x" + videoHeight
                            + " duration=" + durationMs);
                    updateProgress();
                    updateControlText();
                    showControlsTemporarily();
                }

                @Override
                public void onVideoSizeChanged(int width, int height) {
                    videoWidth = width;
                    videoHeight = height;
                    videoView.setVideoSize(videoWidth, videoHeight);
                    Logger.d(player.name() + " video size changed file=" + fileName()
                            + " size=" + videoWidth + "x" + videoHeight
                            + " surface=" + videoView.getWidth() + "x" + videoView.getHeight()
                            + " picture=" + pictureModeLabel());
                    updateControlText();
                }
                @Override
                public void onBufferingStart(int extra) {
                    bufferingCount++;
                    loadingView.setVisibility(View.VISIBLE);
                    Logger.w(player.name() + " buffering start file=" + fileName()
                            + " count=" + bufferingCount
                            + " source=" + currentSource().label
                            + " pos=" + position()
                            + " extra=" + extra);
                    if (bufferingCount == 3 && prepared && !seeking && pendingSeekRequestMs < 0 && playbackOptions != null
                            && playbackOptions.profileMode != PlaybackOptions.PROFILE_FLUENT) {
                        retryWithFluentPlayback("缓冲频繁，切换流畅模式");
                    }
                }

                @Override
                public void onBufferingEnd(int extra) {
                    loadingView.setVisibility(View.GONE);
                    Logger.d(player.name() + " buffering end file=" + fileName()
                            + " source=" + currentSource().label
                            + " pos=" + position()
                            + " extra=" + extra);
                }

                @Override
                public void onFirstFrame() {
                    long startupMs = prepareStartedAtMs <= 0L
                            ? 0L
                            : Math.max(0L, System.currentTimeMillis() - prepareStartedAtMs);
                    Logger.d(player.name() + " first frame file=" + fileName()
                            + " source=" + currentSource().label
                            + " pos=" + position()
                            + " startupMs=" + startupMs
                            + " options=" + optionsLabel());
                    if (!firstFrameSeen) {
                        firstFrameSeen = true;
                        showHint("首帧 " + startupMs + "ms · " + playbackStatusLabel());
                        updateControlText();
                        showControlsTemporarily();
                    }
                }

                @Override
                public void onError(String reason) {
                    loadingView.setVisibility(View.GONE);
                    if (suppressErrors || currentUrl.length() == 0) {
                        return;
                    }
                    notifyPlaybackError(reason);
                }

                @Override
                public void onLog(String message) {
                    Logger.d(player.name() + " " + message + " file=" + fileName());
                }
            });
            prepareStartedAtMs = System.currentTimeMillis();
            showHint("正在启动 " + player.name() + " · " + playbackStatusLabel());
            updateControlText();
            player.prepare(currentUrl, currentSource().authorizationToken, playbackOptions);
            Logger.d(player.name() + " preparing file=" + fileName() + " format=" + formatLabel()
                    + " hw=" + preferHardwareCodec
                    + " options=" + optionsLabel()
                    + " source=" + currentSource().label
                    + " url=" + redactedUrl(currentUrl));
        } catch (Exception ex) {
            loadingView.setVisibility(View.GONE);
            notifyPlaybackError("播放器初始化失败：" + ex.getMessage());
        }
    }

    private PlayerEngine createPlayerEngine() {
        if (retriedIjkEngine) {
            return new IjkPlayerEngine();
        }
        return new VlcPlayerEngine(context);
    }

    private void retryWithSoftwareCodec(String reason) {
        Logger.w(engineName() + " retry software file=" + fileName() + " reason=" + reason);
        retriedSoftwareCodec = true;
        preferHardwareCodec = false;
        playbackOptions = playbackOptions == null
                ? buildPlaybackOptions(currentUrl).withSoftwareDecoder()
                : playbackOptions.withSoftwareDecoder();
        prepared = false;
        firstFrameSeen = false;
        loadingView.setVisibility(View.VISIBLE);
        showHint(reason);
        releasePlayer(true);
        if (surfaceReady) {
            startPlayer();
        }
        handler.removeCallbacks(prepareTimeoutRunnable);
        handler.postDelayed(prepareTimeoutRunnable, PREPARE_TIMEOUT_MS);
    }

    private void retryWithIjkEngine(String reason) {
        Logger.w("VLC fallback to IJK file=" + fileName() + " reason=" + reason);
        retriedIjkEngine = true;
        retriedSoftwareCodec = false;
        preferHardwareCodec = currentEntry != null && currentEntry.prefersHardwarePlayback();
        playbackOptions = buildPlaybackOptions(currentUrl);
        prepared = false;
        firstFrameSeen = false;
        loadingView.setVisibility(View.VISIBLE);
        showHint("VLC播放失败，切换兼容播放器");
        releasePlayer(true);
        if (surfaceReady) {
            startPlayer();
        }
        handler.removeCallbacks(prepareTimeoutRunnable);
        handler.postDelayed(prepareTimeoutRunnable, PREPARE_TIMEOUT_MS);
    }

    private void retryWithFluentPlayback(String reason) {
        Logger.w(engineName() + " fluent retry file=" + fileName() + " reason=" + reason
                + " options=" + optionsLabel());
        playbackOptions = PlaybackStrategy.onFrequentBuffering(playbackOptions);
        prepared = false;
        firstFrameSeen = false;
        bufferingCount = 0;
        int savedPosition = position();
        pendingSeekMs = savedPosition > 0 ? savedPosition : pendingSeekMs;
        loadingView.setVisibility(View.VISIBLE);
        showHint(reason);
        releasePlayer(true);
        if (surfaceReady) {
            startPlayer();
        }
        handler.removeCallbacks(prepareTimeoutRunnable);
        handler.postDelayed(prepareTimeoutRunnable, PREPARE_TIMEOUT_MS);
    }

    private void releasePlayer(boolean clearCurrent) {
        if (currentPlayer == null) {
            return;
        }
        try {
            currentPlayer.detachSurface();
            currentPlayer.stop();
        } catch (RuntimeException ignored) {
        }
        try {
            currentPlayer.release();
        } catch (RuntimeException ignored) {
        }
        currentPlayer = null;
    }

    private void notifyPlaybackError(String reason) {
        handler.removeCallbacks(prepareTimeoutRunnable);
        Logger.w(engineName() + " playback error file=" + fileName() + " format=" + formatLabel()
                + " hw=" + preferHardwareCodec
                + " options=" + optionsLabel()
                + " source=" + currentSource().label
                + " reason=" + reason);
        if (!retriedIjkEngine && !"IJK".equals(engineName())) {
            retryWithIjkEngine(reason);
            return;
        }
        if ("IJK".equals(engineName()) && preferHardwareCodec && !retriedSoftwareCodec) {
            retryWithSoftwareCodec("硬解失败，切换软解");
            return;
        }
        suppressErrors = true;
        releasePlayer(true);
        listener.onNativeVideoError(currentEntry, currentUrl, reason);
    }

    private FnosPlaybackSource currentSource() {
        if (playbackSources.size() == 0) {
            return new FnosPlaybackSource("原画", currentUrl);
        }
        int index = Math.max(0, Math.min(sourceIndex, playbackSources.size() - 1));
        return playbackSources.get(index);
    }

    private String fileName() {
        return currentEntry == null ? "" : currentEntry.name;
    }

    private String formatLabel() {
        return currentEntry == null ? "video" : currentEntry.formatLabel();
    }

    private String engineName() {
        return currentPlayer == null ? "Player" : currentPlayer.name();
    }

    private String optionsLabel() {
        return playbackOptions == null ? "" : playbackOptions.describe();
    }

    private PlaybackOptions buildPlaybackOptions(String url) {
        boolean preferHardware = currentEntry != null && currentEntry.prefersHardwarePlayback();
        long size = currentEntry == null ? 0L : currentEntry.size;
        return PlaybackStrategy.initialOptions(url, fileName(), size, preferHardware);
    }

    private String redactedUrl(String url) {
        if (url == null || url.length() == 0) {
            return "";
        }
        int query = url.indexOf('?');
        return query < 0 ? url : url.substring(0, query) + "?...";
    }

    private TextView controlText(String text) {
        TextView control = new TextView(context);
        control.setText(text);
        control.setTextColor(Color.WHITE);
        control.setTextSize(14);
        control.setSingleLine(true);
        control.setPadding(0, 0, dp(20), dp(4));
        return control;
    }

    private void seekBy(int deltaMs) {
        int base = pendingSeekRequestMs >= 0 ? pendingSeekRequestMs : position();
        int target = Math.max(0, Math.min(duration(), base + deltaMs));
        requestSeekTo(target);
        int seconds = Math.abs(deltaMs) / 1000;
        showHint(deltaMs > 0 ? "快进 " + seconds + " 秒" : "快退 " + seconds + " 秒");
        showControlsTemporarily();
    }

    private void seekToPercent(int percent) {
        int target = NumberSeekShortcut.targetMs(duration(), percent);
        if (target < 0) {
            showHint("暂时无法跳转");
            showControlsTemporarily();
            return;
        }
        requestSeekTo(target);
        showHint(NumberSeekShortcut.hintForPercent(percent));
        showControlsTemporarily();
    }

    private void requestSeekTo(int targetMs) {
        pendingSeekRequestMs = Math.max(0, targetMs);
        seeking = true;
        loadingView.setVisibility(View.VISIBLE);
        Logger.d(engineName() + " seek requested file=" + fileName()
                + " from=" + position()
                + " target=" + pendingSeekRequestMs
                + " duration=" + duration());
        int duration = duration();
        if (duration > 0) {
            seekBar.setProgress(toSeekBarProgress(pendingSeekRequestMs, duration));
        }
        updateTimeLabel(pendingSeekRequestMs, duration);
        handler.removeCallbacks(seekCommitRunnable);
        handler.removeCallbacks(seekSettledRunnable);
        handler.postDelayed(seekCommitRunnable, SEEK_COMMIT_DELAY_MS);
    }

    private void commitPendingSeek() {
        if (pendingSeekRequestMs < 0) {
            return;
        }
        int targetMs = pendingSeekRequestMs;
        pendingSeekRequestMs = -1;
        try {
            if (currentPlayer != null) {
                Logger.d(engineName() + " seek commit file=" + fileName()
                        + " target=" + targetMs
                        + " options=" + optionsLabel());
                currentPlayer.seekTo(targetMs);
            }
            pendingSeekMs = targetMs;
            handler.removeCallbacks(seekSettledRunnable);
            handler.postDelayed(seekSettledRunnable, SEEK_SETTLE_DELAY_MS);
        } catch (RuntimeException ignored) {
            seeking = false;
            if (prepared) {
                loadingView.setVisibility(View.GONE);
            }
            showHint("暂时无法跳转");
        }
    }

    private void cycleSpeed() {
        speedIndex = (speedIndex + 1) % SPEEDS.length;
        float speed = SPEEDS[speedIndex];
        if (applyPlaybackSpeed(speed)) {
            showHint("倍速 " + formatSpeed(speed));
        } else {
            speedIndex = 0;
            showHint("当前播放器不支持倍速播放");
        }
        updateControlText();
        showControlsTemporarily();
    }

    private boolean applyPlaybackSpeed(float speed) {
        if (currentPlayer == null) {
            return speed == 1.0f;
        }
        return currentPlayer.setSpeed(speed);
    }

    private void cyclePictureMode() {
        pictureMode = pictureMode == MODE_FILL ? MODE_FIT : MODE_FILL;
        videoView.setResizeMode(pictureMode);
        if (currentPlayer != null) {
            currentPlayer.setFillMode(pictureMode == MODE_FILL);
            currentPlayer.resizeSurface(videoView.getWidth(), videoView.getHeight());
        }
        updateControlText();
        showHint(pictureMode == MODE_FILL ? "画面铺满" : "画面适应");
        showControlsTemporarily();
    }

    private void cycleQuality() {
        if (playbackSources.size() <= 1) {
            showHint("当前视频没有多码率播放源");
            showControlsTemporarily();
            return;
        }
        int savedPosition = position();
        sourceIndex = (sourceIndex + 1) % playbackSources.size();
        currentUrl = currentSource().url;
        playbackOptions = buildPlaybackOptions(currentUrl);
        pendingSeekMs = savedPosition;
        pendingSeekRequestMs = -1;
        seeking = false;
        handler.removeCallbacks(seekCommitRunnable);
        handler.removeCallbacks(seekSettledRunnable);
        prepared = false;
        firstFrameSeen = false;
        retriedSoftwareCodec = false;
        retriedIjkEngine = false;
        preferHardwareCodec = currentEntry != null && currentEntry.prefersHardwarePlayback();
        videoWidth = 0;
        videoHeight = 0;
        loadingView.setVisibility(View.VISIBLE);
        showHint("切换清晰度 " + currentSource().label);
        releasePlayer(true);
        if (surfaceReady) {
            startPlayer();
        }
        handler.removeCallbacks(prepareTimeoutRunnable);
        handler.postDelayed(prepareTimeoutRunnable, PREPARE_TIMEOUT_MS);
        updateControlText();
        showControlsTemporarily();
    }

    private void updateProgress() {
        if (!isVisible() || dragging || seeking) {
            return;
        }
        int duration = duration();
        int position = position();
        if (duration > 0) {
            seekBar.setProgress(toSeekBarProgress(position, duration));
        } else {
            seekBar.setProgress(0);
        }
        updateTimeLabel(position, duration);
        updateControlText();
    }

    private void updateTimeLabel(int position, int duration) {
        timeView.setText(formatTime(position) + " / " + formatTime(duration));
    }

    private void updateControlText() {
        playStateView.setText(currentPlayer != null && currentPlayer.isPlaying() ? "播放中" : "已暂停");
        speedView.setText("倍速 " + formatSpeed(SPEEDS[speedIndex]));
        pictureView.setText(pictureMode == MODE_FILL ? "画面 铺满" : "画面 适应");
        String label = "清晰度 " + currentSource().label;
        if (videoWidth > 0 && videoHeight > 0) {
            label += " · " + videoWidth + "x" + videoHeight;
        }
        resolutionView.setText(label);
        if (statusView != null) {
            statusView.setText(playbackStatusLabel());
        }
    }

    private String playbackStatusLabel() {
        String engine = currentPlayer == null ? "--" : currentPlayer.name();
        if (playbackOptions == null) {
            return "内核 " + engine;
        }
        return "内核 " + engine
                + " · " + playbackOptions.decoderLabel()
                + " · " + playbackOptions.profileLabel()
                + " · " + playbackOptions.cacheLabel()
                + " " + playbackOptions.networkCachingMs + "ms";
    }

    private int duration() {
        try {
            return currentPlayer == null ? 0 : (int) Math.max(0, currentPlayer.getDuration());
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private int position() {
        try {
            return currentPlayer == null ? 0 : (int) Math.max(0, currentPlayer.getCurrentPosition());
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private int progressToPosition(int progress) {
        int duration = duration();
        return duration <= 0 ? 0 : (int) ((long) duration * Math.max(0, Math.min(1000, progress)) / 1000L);
    }

    private int toSeekBarProgress(int position, int duration) {
        return duration <= 0 ? 0 : (int) Math.max(0L, Math.min(1000L, (long) position * 1000L / (long) duration));
    }

    private String formatTime(int ms) {
        int totalSeconds = Math.max(0, ms / 1000);
        int seconds = totalSeconds % 60;
        int minutes = totalSeconds / 60 % 60;
        int hours = totalSeconds / 3600;
        if (hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    private String formatSpeed(float speed) {
        if (Math.abs(speed - Math.round(speed)) < 0.01f) {
            return String.format(Locale.US, "%.1fx", speed);
        }
        return String.format(Locale.US, "%.2fx", speed);
    }

    private String pictureModeLabel() {
        return pictureMode == MODE_FILL ? "fill" : "fit";
    }

    private void showControls() {
        controlBar.setVisibility(View.VISIBLE);
        titleView.setVisibility(View.VISIBLE);
    }

    private void hideControls() {
        if (isVisible() && !seeking && currentPlayer != null && currentPlayer.isPlaying()) {
            controlBar.setVisibility(View.GONE);
            titleView.setVisibility(View.GONE);
        }
    }

    private void showControlsTemporarily() {
        showControls();
        handler.removeCallbacks(hideControlsRunnable);
        handler.postDelayed(hideControlsRunnable, CONTROL_HIDE_DELAY_MS);
    }

    private void showHint(String message) {
        hintView.setText(message);
    }

    private void enterImmersiveMode() {
        if (!(context instanceof android.app.Activity)) {
            return;
        }
        Window window = ((android.app.Activity) context).getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 19) {
            view.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static final class PlaybackSurfaceView extends SurfaceView {
        private int resizeMode = MODE_FILL;
        private int videoWidth;
        private int videoHeight;

        PlaybackSurfaceView(Context context) {
            super(context);
        }

        void setResizeMode(int resizeMode) {
            this.resizeMode = resizeMode;
            requestLayout();
        }

        void setVideoSize(int videoWidth, int videoHeight) {
            this.videoWidth = videoWidth;
            this.videoHeight = videoHeight;
            requestLayout();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            setMeasuredDimension(width, height);
        }
    }
}
