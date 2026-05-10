package com.fnostv.android4.ui;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.fnostv.android4.net.FnosFileEntry;

public final class NativeVideoPlayerView {
    public interface Listener {
        void onNativeVideoError(FnosFileEntry entry, String url);
    }

    private final Context context;
    private final Listener listener;
    private FrameLayout view;
    private VideoView videoView;
    private ProgressBar loadingView;
    private TextView titleView;
    private FnosFileEntry currentEntry;
    private String currentUrl = "";

    public NativeVideoPlayerView(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public View create() {
        view = new FrameLayout(context);
        view.setBackgroundColor(Color.BLACK);
        view.setVisibility(View.GONE);

        videoView = new VideoView(context);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer player) {
                loadingView.setVisibility(View.GONE);
                videoView.start();
                videoView.requestFocus();
            }
        });
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer player, int what, int extra) {
                loadingView.setVisibility(View.GONE);
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
        titleView.setText(entry == null ? "" : entry.name);
        loadingView.setVisibility(View.VISIBLE);
        view.setVisibility(View.VISIBLE);
        videoView.setVideoURI(Uri.parse(currentUrl));
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
        } else {
            videoView.start();
        }
        return true;
    }

    public void hide() {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            return;
        }
        videoView.stopPlayback();
        view.setVisibility(View.GONE);
        currentEntry = null;
        currentUrl = "";
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
