package com.fnostv.android4.player;

public final class PlaybackStrategy {
    private PlaybackStrategy() {
    }

    public static PlaybackOptions initialOptions(String url, String fileName, long size, boolean preferHardwareCodec) {
        String format = extension(fileName);
        int decoder = preferHardwareCodec ? PlaybackOptions.DECODER_HARDWARE : PlaybackOptions.DECODER_SOFTWARE;
        if (isLegacyFormat(format)) {
            decoder = PlaybackOptions.DECODER_SOFTWARE;
        }
        boolean highRisk = isHighRiskFormat(format, fileName, size);
        if (highRisk) {
            decoder = PlaybackOptions.DECODER_SOFTWARE;
        }
        int cache = isRemoteUrl(url) ? PlaybackOptions.CACHE_REMOTE : PlaybackOptions.CACHE_STABLE;
        int networkMs = cache == PlaybackOptions.CACHE_REMOTE ? 6000 : 3000;
        int fileMs = cache == PlaybackOptions.CACHE_REMOTE ? 3000 : 2000;
        boolean fluent = shouldPreferFluent(format, fileName, size);
        if (fluent) {
            networkMs = Math.max(networkMs, cache == PlaybackOptions.CACHE_REMOTE ? 8000 : 5000);
            fileMs = Math.max(fileMs, 2500);
        }
        int probeSizeKb = fluent ? 2048 : 768;
        int analyzeDurationMs = fluent ? 2500 : 1200;
        return new PlaybackOptions(
                decoder,
                cache,
                fluent ? PlaybackOptions.PROFILE_FLUENT : PlaybackOptions.PROFILE_STABLE,
                networkMs,
                fileMs,
                fluent,
                fluent,
                fluent ? 4 : 0,
                probeSizeKb,
                analyzeDurationMs);
    }

    public static PlaybackOptions onFrequentBuffering(PlaybackOptions current) {
        if (current == null) {
            return PlaybackOptions.forUrl("", true).withFluentFallback();
        }
        int networkMs = Math.min(10000, Math.max(current.networkCachingMs + 2000, 6000));
        int fileMs = Math.min(5000, Math.max(current.fileCachingMs + 1000, 3000));
        return new PlaybackOptions(
                current.decoderMode,
                current.cacheMode,
                PlaybackOptions.PROFILE_FLUENT,
                networkMs,
                fileMs,
                true,
                true,
                Math.max(4, current.loopFilterSkip),
                Math.max(2048, current.probeSizeKb),
                Math.max(2500, current.analyzeDurationMs));
    }

    private static boolean shouldPreferFluent(String format, String fileName, long size) {
        return isHighRiskFormat(format, fileName, size);
    }

    private static boolean isHighRiskFormat(String format, String fileName, long size) {
        String lowerName = lower(fileName);
        return format.equals("mkv")
                || format.equals("m2ts")
                || format.equals("ts")
                || format.equals("webm")
                || lowerName.indexOf("h265") >= 0
                || lowerName.indexOf("hevc") >= 0
                || lowerName.indexOf("x265") >= 0
                || lowerName.indexOf("2160p") >= 0
                || lowerName.indexOf("4k") >= 0
                || size > 4L * 1024L * 1024L * 1024L;
    }

    private static boolean isLegacyFormat(String format) {
        return format.equals("rm")
                || format.equals("rmvb")
                || format.equals("avi")
                || format.equals("wmv")
                || format.equals("asf")
                || format.equals("flv")
                || format.equals("vob");
    }

    private static String extension(String fileName) {
        String value = lower(fileName);
        int queryIndex = value.indexOf('?');
        if (queryIndex >= 0) {
            value = value.substring(0, queryIndex);
        }
        int index = value.lastIndexOf('.');
        if (index < 0 || index == value.length() - 1) {
            return "";
        }
        return value.substring(index + 1);
    }

    private static boolean isRemoteUrl(String url) {
        String lower = lower(url);
        return lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("rtsp://");
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
