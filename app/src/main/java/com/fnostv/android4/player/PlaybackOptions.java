package com.fnostv.android4.player;

public final class PlaybackOptions {
    public static final int DECODER_AUTO = 0;
    public static final int DECODER_HARDWARE = 1;
    public static final int DECODER_SOFTWARE = 2;

    public static final int CACHE_BALANCED = 0;
    public static final int CACHE_STABLE = 1;
    public static final int CACHE_REMOTE = 2;

    public final int decoderMode;
    public final int cacheMode;
    public final int networkCachingMs;
    public final int fileCachingMs;
    public final boolean allowFrameDrop;

    private PlaybackOptions(int decoderMode, int cacheMode, int networkCachingMs, int fileCachingMs, boolean allowFrameDrop) {
        this.decoderMode = decoderMode;
        this.cacheMode = cacheMode;
        this.networkCachingMs = networkCachingMs;
        this.fileCachingMs = fileCachingMs;
        this.allowFrameDrop = allowFrameDrop;
    }

    public static PlaybackOptions forUrl(String url, boolean preferHardwareCodec) {
        int mode = preferHardwareCodec ? DECODER_HARDWARE : DECODER_SOFTWARE;
        int cache = isRemoteUrl(url) ? CACHE_REMOTE : CACHE_STABLE;
        if (cache == CACHE_REMOTE) {
            return new PlaybackOptions(mode, cache, 6000, 3000, false);
        }
        return new PlaybackOptions(mode, cache, 3000, 2000, false);
    }

    public PlaybackOptions withSoftwareDecoder() {
        return new PlaybackOptions(DECODER_SOFTWARE, cacheMode, networkCachingMs, fileCachingMs, allowFrameDrop);
    }

    public boolean useHardwareDecoder() {
        return decoderMode == DECODER_HARDWARE || decoderMode == DECODER_AUTO;
    }

    public String decoderLabel() {
        if (decoderMode == DECODER_HARDWARE) {
            return "hardware";
        }
        if (decoderMode == DECODER_SOFTWARE) {
            return "software";
        }
        return "auto";
    }

    public String cacheLabel() {
        if (cacheMode == CACHE_REMOTE) {
            return "remote";
        }
        if (cacheMode == CACHE_STABLE) {
            return "stable";
        }
        return "balanced";
    }

    public String describe() {
        return "decoder=" + decoderLabel()
                + " cache=" + cacheLabel()
                + " network=" + networkCachingMs
                + " file=" + fileCachingMs
                + " framedrop=" + allowFrameDrop;
    }

    private static boolean isRemoteUrl(String url) {
        if (url == null) {
            return false;
        }
        String lower = url.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("rtsp://");
    }
}
