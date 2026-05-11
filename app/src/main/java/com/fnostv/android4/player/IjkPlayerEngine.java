package com.fnostv.android4.player;

import android.view.SurfaceHolder;

import java.lang.reflect.Method;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public final class IjkPlayerEngine implements PlayerEngine {
    private static boolean loaded;

    private IjkMediaPlayer player;
    private Listener listener;

    @Override
    public String name() {
        return "IJK";
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void attachSurface(SurfaceHolder holder, int width, int height) {
        if (player != null) {
            player.setDisplay(holder);
        }
    }

    @Override
    public void resizeSurface(int width, int height) {
    }

    @Override
    public void detachSurface() {
        if (player != null) {
            player.setDisplay(null);
        }
    }

    @Override
    public void prepare(String url, PlaybackOptions options) throws Exception {
        ensureLoaded();
        player = new IjkMediaPlayer();
        configurePlayer(player, options);
        player.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mediaPlayer) {
                if (listener != null) {
                    listener.onPrepared(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight(), (int) Math.max(0, mediaPlayer.getDuration()));
                }
            }
        });
        player.setOnVideoSizeChangedListener(new IMediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(IMediaPlayer mediaPlayer, int width, int height, int sarNum, int sarDen) {
                if (listener != null) {
                    listener.onVideoSizeChanged(width, height);
                }
            }
        });
        player.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer mediaPlayer, int what, int extra) {
                if (listener != null) {
                    listener.onError("播放器错误 what=" + what + " extra=" + extra);
                }
                return true;
            }
        });
        player.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(IMediaPlayer mediaPlayer, int what, int extra) {
                handleInfo(what, extra);
                return false;
            }
        });
        player.setDataSource(url);
        player.prepareAsync();
    }

    @Override
    public void start() {
        if (player != null) {
            player.start();
        }
    }

    @Override
    public void pause() {
        if (player != null) {
            player.pause();
        }
    }

    @Override
    public void stop() {
        if (player != null) {
            player.stop();
        }
    }

    @Override
    public void seekTo(int positionMs) {
        if (player != null) {
            player.seekTo(positionMs);
        }
    }

    @Override
    public boolean setSpeed(float speed) {
        if (player == null) {
            return speed == 1.0f;
        }
        try {
            Method method = player.getClass().getMethod("setSpeed", float.class);
            method.invoke(player, speed);
            if (!player.isPlaying()) {
                player.start();
            }
            return true;
        } catch (Exception ex) {
            return speed == 1.0f;
        }
    }

    @Override
    public boolean isPlaying() {
        try {
            return player != null && player.isPlaying();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @Override
    public int getDuration() {
        try {
            return player == null ? 0 : (int) Math.max(0, player.getDuration());
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    @Override
    public int getCurrentPosition() {
        try {
            return player == null ? 0 : (int) Math.max(0, player.getCurrentPosition());
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    @Override
    public void release() {
        if (player == null) {
            return;
        }
        try {
            player.setDisplay(null);
            player.stop();
        } catch (RuntimeException ignored) {
        }
        try {
            player.release();
        } catch (RuntimeException ignored) {
        }
        player = null;
    }

    private static synchronized void ensureLoaded() {
        if (!loaded) {
            IjkMediaPlayer.loadLibrariesOnce(null);
            loaded = true;
        }
    }

    private void configurePlayer(IjkMediaPlayer player, PlaybackOptions options) {
        boolean hardwareCodec = options != null && options.useHardwareDecoder();
        int maxBufferSize = options == null ? 4 * 1024 * 1024 : Math.max(4, options.networkCachingMs / 1000) * 1024 * 1024;
        int maxCachedDuration = options == null ? 30000 : Math.max(15000, options.networkCachingMs * 5);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", hardwareCodec ? 1 : 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", options != null && options.allowFrameDrop ? 1 : 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-buffer-size", maxBufferSize);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", 2);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max_cached_duration", maxCachedDuration);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzemaxduration", 100L);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 32 * 1024L);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "fflags", "fastseek");
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
    }

    private void handleInfo(int what, int extra) {
        if (listener == null) {
            return;
        }
        if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_START) {
            listener.onBufferingStart(extra);
            return;
        }
        if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_END) {
            listener.onBufferingEnd(extra);
            return;
        }
        if (what == IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
            listener.onFirstFrame();
            return;
        }
        listener.onLog("info what=" + what + " extra=" + extra);
    }
}
