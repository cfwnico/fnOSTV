package com.fnostv.android4.ui;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import java.lang.reflect.Method;
import java.util.Locale;

import com.fnostv.android4.net.FnosFileEntry;

public final class NativeVideoPlayerView {
    public interface Listener {
        void onNativeVideoError(FnosFileEntry entry, String url);
    }

    private static final int SEEK_STEP_MS = 10000;
    private static final int LARGE_SEEK_STEP_MS = 30000;
    private static final int CONTROL_HIDE_DELAY_MS = 5000;
    private static final int PROGRESS_INTERVAL_MS = 1000;
    private static final int MODE_FILL = 0;
    private static final int MODE_FIT = 1;
    private static final float[] SPEEDS = new float[]{1.0f, 1.25f, 1.5f, 2.0f, 0.75f};

    private final Context context;
    private final Listener listener;
    private final Handler handler = new Handler();
    private FrameLayout view;
    private PlaybackVideoView videoView;
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
    private FnosFileEntry currentEntry;
    private String currentUrl = "";
    private MediaPlayer currentPlayer;
    private boolean dragging;
    private boolean suppressErrors;
    private int speedIndex;
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

    public NativeVideoPlayerView(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public View create() {
        view = new FrameLayout(context);
        view.setBackgroundColor(Color.BLACK);
        view.setVisibility(View.GONE);

        videoView = new PlaybackVideoView(context);
        videoView.setResizeMode(pictureMode);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer player) {
                currentPlayer = player;
                videoWidth = player.getVideoWidth();
                videoHeight = player.getVideoHeight();
                videoView.setVideoSize(videoWidth, videoHeight);
                loadingView.setVisibility(View.GONE);
                videoView.start();
                videoView.requestFocus();
                updateProgress();
                updateControlText();
                showControlsTemporarily();
            }
        });
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer player, int what, int extra) {
                loadingView.setVisibility(View.GONE);
                if (suppressErrors || currentUrl.length() == 0) {
                    return true;
                }
                listener.onNativeVideoError(currentEntry, currentUrl);
                return true;
            }
        });
        view.addView(videoView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        titleView = new TextView(context);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(16);
        titleView.setSingleLine(true);
        titleView.setBackgroundColor(0x99000000);
        titleView.setPadding(dp(18), dp(10), dp(18), dp(10));
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP);
        view.addView(titleView, titleParams);

        controlBar = new LinearLayout(context);
        controlBar.setOrientation(LinearLayout.VERTICAL);
        controlBar.setBackgroundColor(0xCC000000);
        controlBar.setPadding(dp(18), dp(10), dp(18), dp(12));

        LinearLayout controlRow = new LinearLayout(context);
        controlRow.setOrientation(LinearLayout.HORIZONTAL);
        controlRow.setGravity(Gravity.CENTER_VERTICAL);

        playStateView = controlText("暂停/播放");
        timeView = controlText("00:00 / 00:00");
        speedView = controlText("倍速 1.0x");
        pictureView = controlText("画面 铺满");
        resolutionView = controlText("分辨率 原画");
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
        hintView.setText("左右键快退/快进，确认键暂停，菜单键倍速，上键切换画面");

        controlBar.addView(controlRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        controlBar.addView(seekBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        controlBar.addView(hintView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams controlParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        view.addView(controlBar, controlParams);

        loadingView = new ProgressBar(context);
        FrameLayout.LayoutParams loadingParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        view.addView(loadingView, loadingParams);
        return view;
    }

    public void play(FnosFileEntry entry, String url) {
        currentEntry = entry;
        currentUrl = url == null ? "" : url;
        suppressErrors = false;
        titleView.setText(entry == null ? "" : entry.name);
        loadingView.setVisibility(View.VISIBLE);
        view.setVisibility(View.VISIBLE);
        view.setKeepScreenOn(true);
        enterImmersiveMode();
        speedIndex = 0;
        videoWidth = 0;
        videoHeight = 0;
        currentPlayer = null;
        videoView.setResizeMode(pictureMode);
        videoView.setVideoURI(Uri.parse(currentUrl));
        handler.removeCallbacks(progressRunnable);
        handler.post(progressRunnable);
        showControlsTemporarily();
    }

    public boolean isVisible() {
        return view != null && view.getVisibility() == View.VISIBLE;
    }

    public boolean toggle() {
        if (!isVisible()) {
            return false;
        }
        if (videoView.isPlaying()) {
            videoView.pause();
            showHint("已暂停");
        } else {
            videoView.start();
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
            hideControls();
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
        suppressErrors = true;
        videoView.stopPlayback();
        view.setVisibility(View.GONE);
        view.setKeepScreenOn(false);
        currentEntry = null;
        currentUrl = "";
        currentPlayer = null;
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
            videoView.seekTo(targetMs);
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
            showHint("当前 Android 版本不支持倍速播放");
        }
        updateControlText();
        showControlsTemporarily();
    }

    private boolean applyPlaybackSpeed(float speed) {
        if (currentPlayer == null || Build.VERSION.SDK_INT < 23) {
            return speed == 1.0f;
        }
        try {
            Method getParams = currentPlayer.getClass().getMethod("getPlaybackParams");
            Object params = getParams.invoke(currentPlayer);
            Method setSpeed = params.getClass().getMethod("setSpeed", float.class);
            Object updated = setSpeed.invoke(params, speed);
            Method setParams = currentPlayer.getClass().getMethod("setPlaybackParams", params.getClass());
            setParams.invoke(currentPlayer, updated);
            if (!videoView.isPlaying()) {
                videoView.start();
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
        playStateView.setText(videoView != null && videoView.isPlaying() ? "播放中" : "已暂停");
        speedView.setText("倍速 " + formatSpeed(SPEEDS[speedIndex]));
        pictureView.setText(pictureMode == MODE_FILL ? "画面 铺满" : "画面 适应");
        if (videoWidth > 0 && videoHeight > 0) {
            resolutionView.setText("分辨率 " + videoWidth + "x" + videoHeight);
        } else {
            resolutionView.setText("分辨率 原画");
        }
    }

    private int duration() {
        try {
            return videoView == null ? 0 : Math.max(0, videoView.getDuration());
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private int position() {
        try {
            return videoView == null ? 0 : Math.max(0, videoView.getCurrentPosition());
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
        if (isVisible() && videoView.isPlaying()) {
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

    private static final class PlaybackVideoView extends VideoView {
        private int resizeMode = MODE_FILL;
        private int videoWidth;
        private int videoHeight;

        PlaybackVideoView(Context context) {
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
