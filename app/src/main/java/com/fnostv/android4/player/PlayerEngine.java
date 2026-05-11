package com.fnostv.android4.player;

import android.view.SurfaceHolder;

public interface PlayerEngine {
    interface Listener {
        void onPrepared(int videoWidth, int videoHeight, int durationMs);

        void onVideoSizeChanged(int videoWidth, int videoHeight);

        void onBufferingStart(int extra);

        void onBufferingEnd(int extra);

        void onFirstFrame();

        void onError(String reason);

        void onLog(String message);
    }

    String name();

    void setListener(Listener listener);

    void attachSurface(SurfaceHolder holder, int width, int height);

    void resizeSurface(int width, int height);

    void detachSurface();

    void prepare(String url, PlaybackOptions options) throws Exception;

    void start();

    void pause();

    void stop();

    void seekTo(int positionMs);

    boolean setSpeed(float speed);

    boolean isPlaying();

    int getDuration();

    int getCurrentPosition();

    void release();
}
