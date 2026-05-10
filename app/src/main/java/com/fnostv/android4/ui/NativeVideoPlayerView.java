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
import com.fnostv.android4.util.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public final class NativeVideoPlayerView {
    public interface Listener {
        void onNativeVideoError(FnosFileEntry entry, String url, String reason);
    }

    private static final int SEEK_STEP_MS = 10000;
    private static final int LARGE_SEEK_STEP_MS = 30000;
    private static final int CONTROL_HIDE_DELAY_MS = 5000;
    private static final int PROGRESS_INTERVAL_MS = 1000;
    private static final int PREPARE_TIMEOUT_MS = 20000;
    private static final int MODE_FILL = 0;
    private static final int MODE_FIT = 1;
    private static final float[] SPEEDS = new float[]{1.0f, 1.25f, 1.5f, 2.0f, 0.75f};
    private static boolean ijkLoaded;

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
    private TextView hintView;
    private SurfaceHolder surfaceHolder;
    private FnosFileEntry currentEntry;
    private String currentUrl = "";
    private IjkMediaPlayer currentPlayer;
    private boolean surfaceReady;
    private boolean dragging;
    private boolean suppressErrors;
    private boolean prepared;
    private boolean preferHardwareCodec;
    private boolean retriedSoftwareCodec;
    private int bufferingCount;
    private int speedIndex;
    private int sourceIndex;
    private int pendingSeekMs = -1;
    private int pictureMode = MODE_FILL;
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
                    currentPlayer.setDisplay(holder);
                } else if (currentUrl.length() > 0 && view.getVisibility() == View.VISIBLE) {
                    startPlayer();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                surfaceHolder = holder;
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                surfaceReady = false;
                surfaceHolder = null;
                if (currentPlayer != null) {
                    currentPlayer.setDisplay(null);
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
                seekTo(progressToPosition(bar.getProgress()));
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
        pendingSeekMs = -1;
        suppressErrors = false;
        prepared = false;
        preferHardwareCodec = entry != null && entry.prefersHardwarePlayback();
        retriedSoftwareCodec = false;
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
        suppressErrors = true;
        releasePlayer(true);
        view.setVisibility(View.GONE);
        view.setKeepScreenOn(false);
        currentEntry = null;
        currentUrl = "";
        playbackSources.clear();
        sourceIndex = 0;
        pendingSeekMs = -1;
        prepared = false;
    }

    private void startPlayer() {
        if (currentUrl.length() == 0 || !surfaceReady) {
            return;
        }
        try {
            ensureIjkLoaded();
            IjkMediaPlayer player = new IjkMediaPlayer();
            configurePlayer(player, preferHardwareCodec);
            currentPlayer = player;
            player.setDisplay(surfaceHolder);
            player.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(IMediaPlayer mediaPlayer) {
                    handler.removeCallbacks(prepareTimeoutRunnable);
                    prepared = true;
                    videoWidth = mediaPlayer.getVideoWidth();
                    videoHeight = mediaPlayer.getVideoHeight();
                    videoView.setVideoSize(videoWidth, videoHeight);
                    loadingView.setVisibility(View.GONE);
                    mediaPlayer.start();
                    if (pendingSeekMs > 0) {
                        mediaPlayer.seekTo(pendingSeekMs);
                        pendingSeekMs = -1;
                    }
                    applyPlaybackSpeed(SPEEDS[speedIndex]);
                    Logger.d("IJK prepared file=" + fileName() + " format=" + formatLabel()
                            + " hw=" + preferHardwareCodec
                            + " source=" + currentSource().label
                            + " size=" + videoWidth + "x" + videoHeight
                            + " duration=" + mediaPlayer.getDuration());
                    updateProgress();
                    updateControlText();
                    showControlsTemporarily();
                }
            });
            player.setOnVideoSizeChangedListener(new IMediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(IMediaPlayer mediaPlayer, int width, int height, int sarNum, int sarDen) {
                    videoWidth = width;
                    videoHeight = height;
                    videoView.setVideoSize(videoWidth, videoHeight);
                    updateControlText();
                }
            });
            player.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(IMediaPlayer mediaPlayer, int what, int extra) {
                    loadingView.setVisibility(View.GONE);
                    if (suppressErrors || currentUrl.length() == 0) {
                        return true;
                    }
                    notifyPlaybackError("播放器错误 what=" + what + " extra=" + extra);
                    return true;
                }
            });
            player.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(IMediaPlayer mediaPlayer, int what, int extra) {
                    handlePlayerInfo(what, extra);
                    return false;
                }
            });
            player.setDataSource(currentUrl);
            player.prepareAsync();
            Logger.d("IJK preparing file=" + fileName() + " format=" + formatLabel()
                    + " hw=" + preferHardwareCodec
                    + " source=" + currentSource().label
                    + " url=" + redactedUrl(currentUrl));
        } catch (Exception ex) {
            loadingView.setVisibility(View.GONE);
            notifyPlaybackError("播放器初始化失败：" + ex.getMessage());
        }
    }

    private static synchronized void ensureIjkLoaded() {
        if (!ijkLoaded) {
            IjkMediaPlayer.loadLibrariesOnce(null);
            ijkLoaded = true;
        }
    }

    private void configurePlayer(IjkMediaPlayer player, boolean hardwareCodec) {
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", hardwareCodec ? 1 : 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", hardwareCodec ? 1 : 5);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-buffer-size", 4 * 1024 * 1024);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", 2);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max_cached_duration", 30000);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzemaxduration", 100L);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 32 * 1024L);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "fflags", "fastseek");
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
    }

    private void retryWithSoftwareCodec(String reason) {
        Logger.w("IJK retry software file=" + fileName() + " reason=" + reason);
        retriedSoftwareCodec = true;
        preferHardwareCodec = false;
        prepared = false;
        loadingView.setVisibility(View.VISIBLE);
        showHint(reason);
        releasePlayer(true);
        if (surfaceReady) {
            startPlayer();
        }
        handler.removeCallbacks(prepareTimeoutRunnable);
        handler.postDelayed(prepareTimeoutRunnable, PREPARE_TIMEOUT_MS);
    }

    private void handlePlayerInfo(int what, int extra) {
        if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_START) {
            bufferingCount++;
            loadingView.setVisibility(View.VISIBLE);
            Logger.w("IJK buffering start file=" + fileName()
                    + " count=" + bufferingCount
                    + " source=" + currentSource().label
                    + " pos=" + position()
                    + " extra=" + extra);
            return;
        }
        if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_END) {
            loadingView.setVisibility(View.GONE);
            Logger.d("IJK buffering end file=" + fileName()
                    + " source=" + currentSource().label
                    + " pos=" + position()
                    + " extra=" + extra);
            return;
        }
        if (what == IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
            Logger.d("IJK first frame file=" + fileName()
                    + " source=" + currentSource().label
                    + " pos=" + position());
            return;
        }
        Logger.d("IJK info what=" + what + " extra=" + extra + " file=" + fileName());
    }

    private void releasePlayer(boolean clearCurrent) {
        if (currentPlayer == null) {
            return;
        }
        try {
            currentPlayer.setDisplay(null);
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
        Logger.w("IJK playback error file=" + fileName() + " format=" + formatLabel()
                + " hw=" + preferHardwareCodec
                + " source=" + currentSource().label
                + " reason=" + reason);
        if (preferHardwareCodec && !retriedSoftwareCodec) {
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
        int target = Math.max(0, Math.min(duration(), position() + deltaMs));
        seekTo(target);
        int seconds = Math.abs(deltaMs) / 1000;
        showHint(deltaMs > 0 ? "快进 " + seconds + " 秒" : "快退 " + seconds + " 秒");
        showControlsTemporarily();
    }

    private void seekTo(int targetMs) {
        try {
            if (currentPlayer != null) {
                currentPlayer.seekTo(targetMs);
            }
            updateProgress();
        } catch (RuntimeException ignored) {
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
        try {
            Method method = currentPlayer.getClass().getMethod("setSpeed", float.class);
            method.invoke(currentPlayer, speed);
            if (!currentPlayer.isPlaying()) {
                currentPlayer.start();
            }
            return true;
        } catch (Exception ex) {
            return speed == 1.0f;
        }
    }

    private void cyclePictureMode() {
        pictureMode = pictureMode == MODE_FILL ? MODE_FIT : MODE_FILL;
        videoView.setResizeMode(pictureMode);
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
        pendingSeekMs = savedPosition;
        prepared = false;
        retriedSoftwareCodec = false;
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
        if (!isVisible() || dragging) {
            return;
        }
        int duration = duration();
        int position = position();
        if (duration > 0) {
            seekBar.setProgress(Math.max(0, Math.min(1000, position * 1000 / duration)));
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
        return duration <= 0 ? 0 : duration * progress / 1000;
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

    private void showControls() {
        controlBar.setVisibility(View.VISIBLE);
        titleView.setVisibility(View.VISIBLE);
    }

    private void hideControls() {
        if (isVisible() && currentPlayer != null && currentPlayer.isPlaying()) {
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
            if (resizeMode == MODE_FILL || videoWidth <= 0 || videoHeight <= 0) {
                setMeasuredDimension(width, height);
                return;
            }
            float videoAspect = (float) videoWidth / (float) videoHeight;
            float viewAspect = height == 0 ? videoAspect : (float) width / (float) height;
            if (videoAspect > viewAspect) {
                height = (int) (width / videoAspect);
            } else {
                width = (int) (height * videoAspect);
            }
            setMeasuredDimension(width, height);
        }
    }
}
