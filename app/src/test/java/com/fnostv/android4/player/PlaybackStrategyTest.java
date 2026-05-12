package com.fnostv.android4.player;

public final class PlaybackStrategyTest {
    public static void main(String[] args) {
        highRiskRemoteMkvUsesSoftwareFluentProfile();
        normalMp4KeepsHardwareStableProfile();
        frequentBufferingRaisesCacheWithoutUnboundedGrowth();
    }

    private static void highRiskRemoteMkvUsesSoftwareFluentProfile() {
        PlaybackOptions options = PlaybackStrategy.initialOptions(
                "http://nas.local/video/movie.mkv",
                "Movie.2160p.HEVC.mkv",
                8L * 1024L * 1024L * 1024L,
                true);

        assertEquals(PlaybackOptions.DECODER_SOFTWARE, options.decoderMode);
        assertEquals(PlaybackOptions.CACHE_REMOTE, options.cacheMode);
        assertEquals(PlaybackOptions.PROFILE_FLUENT, options.profileMode);
        assertTrue(options.networkCachingMs >= 8000);
        assertTrue(options.allowFrameDrop);
        assertTrue(options.fastDecode);
        assertTrue(options.loopFilterSkip >= 4);
    }

    private static void normalMp4KeepsHardwareStableProfile() {
        PlaybackOptions options = PlaybackStrategy.initialOptions(
                "http://nas.local/video/movie.mp4",
                "Movie.1080p.H264.mp4",
                1024L * 1024L * 1024L,
                true);

        assertEquals(PlaybackOptions.DECODER_HARDWARE, options.decoderMode);
        assertEquals(PlaybackOptions.CACHE_REMOTE, options.cacheMode);
        assertEquals(PlaybackOptions.PROFILE_STABLE, options.profileMode);
        assertFalse(options.allowFrameDrop);
        assertFalse(options.fastDecode);
    }

    private static void frequentBufferingRaisesCacheWithoutUnboundedGrowth() {
        PlaybackOptions current = new PlaybackOptions(
                PlaybackOptions.DECODER_SOFTWARE,
                PlaybackOptions.CACHE_REMOTE,
                PlaybackOptions.PROFILE_STABLE,
                9000,
                4500,
                false,
                false,
                0);

        PlaybackOptions options = PlaybackStrategy.onFrequentBuffering(current);

        assertEquals(PlaybackOptions.PROFILE_FLUENT, options.profileMode);
        assertEquals(10000, options.networkCachingMs);
        assertEquals(5000, options.fileCachingMs);
        assertTrue(options.allowFrameDrop);
        assertTrue(options.fastDecode);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }

    private static void assertTrue(boolean value) {
        if (!value) {
            throw new AssertionError("Expected true");
        }
    }

    private static void assertFalse(boolean value) {
        if (value) {
            throw new AssertionError("Expected false");
        }
    }
}
