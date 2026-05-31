package com.fnostv.android4.player;

import android.content.Context;
import android.net.Uri;
import android.view.SurfaceHolder;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.IVLCVout;

import java.util.ArrayList;

public final class VlcPlayerEngine implements PlayerEngine {
    private final Context context;
    private LibVLC libVlc;
    private MediaPlayer player;
    private Listener listener;
    private SurfaceHolder surfaceHolder;
    private int surfaceWidth;
    private int surfaceHeight;
    private boolean prepared;
    private boolean fillMode;

    public VlcPlayerEngine(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public String name() {
        return "VLC";
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void attachSurface(SurfaceHolder holder, int width, int height) {
        surfaceHolder = holder;
        surfaceWidth = Math.max(0, width);
        surfaceHeight = Math.max(0, height);
        if (player != null && holder != null) {
            IVLCVout out = player.getVLCVout();
            out.detachViews();
            if (surfaceWidth > 0 && surfaceHeight > 0) {
                out.setWindowSize(surfaceWidth, surfaceHeight);
            }
            out.setVideoSurface(holder.getSurface(), holder);
            out.attachViews();
            applyDisplayMode();
        }
    }

    @Override
    public void resizeSurface(int width, int height) {
        surfaceWidth = Math.max(0, width);
        surfaceHeight = Math.max(0, height);
        if (player != null && surfaceWidth > 0 && surfaceHeight > 0) {
            player.getVLCVout().setWindowSize(surfaceWidth, surfaceHeight);
            applyDisplayMode();
        }
    }

    @Override
    public void setFillMode(boolean fillMode) {
        this.fillMode = fillMode;
        applyDisplayMode();
    }

    @Override
    public void detachSurface() {
        surfaceHolder = null;
        if (player != null) {
            player.getVLCVout().detachViews();
        }
    }

    @Override
    public void prepare(String url, String authorizationToken, PlaybackOptions playbackOptions) {
        PlaybackOptions playback = playbackOptions == null ? PlaybackOptions.forUrl(url, true) : playbackOptions;
        ArrayList<String> vlcOptions = new ArrayList<String>();
        vlcOptions.add("--network-caching=" + playback.networkCachingMs);
        vlcOptions.add("--file-caching=" + playback.fileCachingMs);
        vlcOptions.add("--live-caching=" + Math.max(1500, playback.networkCachingMs / 2));
        vlcOptions.add("--clock-jitter=0");
        vlcOptions.add("--clock-synchro=0");
        vlcOptions.add("--http-reconnect");
        if (playback.allowFrameDrop) {
            vlcOptions.add("--drop-late-frames");
            vlcOptions.add("--skip-frames");
        } else {
            vlcOptions.add("--no-drop-late-frames");
            vlcOptions.add("--no-skip-frames");
        }
        if (playback.fastDecode) {
            vlcOptions.add("--avcodec-fast");
        }
        if (playback.loopFilterSkip > 0) {
            vlcOptions.add("--avcodec-skiploopfilter=" + playback.loopFilterSkip);
        }
        libVlc = new LibVLC(context, vlcOptions);
        player = new MediaPlayer(libVlc);
        player.setEventListener(new MediaPlayer.EventListener() {
            @Override
            public void onEvent(MediaPlayer.Event event) {
                handleEvent(event);
            }
        });
        if (surfaceHolder != null) {
            attachSurface(surfaceHolder, surfaceWidth, surfaceHeight);
        }
        Media media = new Media(libVlc, Uri.parse(url));
        media.setHWDecoderEnabled(playback.useHardwareDecoder(), false);
        media.addOption(":network-caching=" + playback.networkCachingMs);
        media.addOption(":file-caching=" + playback.fileCachingMs);
        media.addOption(":live-caching=" + Math.max(1500, playback.networkCachingMs / 2));
        media.addOption(":clock-jitter=0");
        media.addOption(":clock-synchro=0");
        if (playback.allowFrameDrop) {
            media.addOption(":drop-late-frames");
            media.addOption(":skip-frames");
        } else {
            media.addOption(":no-drop-late-frames");
            media.addOption(":no-skip-frames");
        }
        if (playback.fastDecode) {
            media.addOption(":avcodec-fast");
        }
        if (playback.loopFilterSkip > 0) {
            media.addOption(":avcodec-skiploopfilter=" + playback.loopFilterSkip);
        }
        media.addOption(":http-reconnect");
        if (authorizationToken != null && authorizationToken.length() > 0) {
            media.addOption(":http-header=Authorization: " + authorizationToken);
        }
        player.setMedia(media);
        media.release();
        player.play();
    }

    @Override
    public void start() {
        if (player != null) {
            player.play();
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
            player.setTime(Math.max(0, positionMs));
        }
    }

    @Override
    public boolean setSpeed(float speed) {
        if (player == null) {
            return speed == 1.0f;
        }
        try {
            player.setRate(speed);
            return true;
        } catch (RuntimeException ex) {
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
            return player == null ? 0 : (int) Math.max(0, player.getLength());
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    @Override
    public int getCurrentPosition() {
        try {
            return player == null ? 0 : (int) Math.max(0, player.getTime());
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    @Override
    public void release() {
        if (player != null) {
            try {
                player.getVLCVout().detachViews();
                player.stop();
            } catch (RuntimeException ignored) {
            }
            player.release();
            player = null;
        }
        if (libVlc != null) {
            libVlc.release();
            libVlc = null;
        }
        prepared = false;
    }

    private void handleEvent(MediaPlayer.Event event) {
        if (listener == null || event == null) {
            return;
        }
        if (event.type == MediaPlayer.Event.Opening) {
            listener.onBufferingStart(0);
            return;
        }
        if (event.type == MediaPlayer.Event.Buffering) {
            return;
        }
        if (event.type == MediaPlayer.Event.Playing) {
            if (!prepared) {
                prepared = true;
                listener.onPrepared(0, 0, getDuration());
            } else {
                listener.onBufferingEnd(0);
            }
            listener.onFirstFrame();
            return;
        }
        if (event.type == MediaPlayer.Event.Paused || event.type == MediaPlayer.Event.Stopped) {
            listener.onLog("event type=" + event.type);
            return;
        }
        if (event.type == MediaPlayer.Event.EncounteredError) {
            listener.onError("VLC 播放器错误");
            return;
        }
        listener.onLog("event type=" + event.type);
    }

    private void applyDisplayMode() {
        if (player == null) {
            return;
        }
        if (fillMode && surfaceWidth > 0 && surfaceHeight > 0) {
            player.setAspectRatio(surfaceWidth + ":" + surfaceHeight);
        } else {
            player.setAspectRatio(null);
        }
        player.setScale(0);
    }
}
