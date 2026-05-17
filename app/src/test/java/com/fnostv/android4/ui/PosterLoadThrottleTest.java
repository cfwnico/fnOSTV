package com.fnostv.android4.ui;

public final class PosterLoadThrottleTest {
    public static void main(String[] args) {
        rejectsDuplicateInFlightUrl();
        limitsActiveLoads();
        finishAllowsUrlToStartAgain();
    }

    private static void rejectsDuplicateInFlightUrl() {
        PosterLoadThrottle throttle = new PosterLoadThrottle(2);

        assertTrue(throttle.tryStart("http://host/a.webp"));
        assertFalse(throttle.tryStart("http://host/a.webp"));
    }

    private static void limitsActiveLoads() {
        PosterLoadThrottle throttle = new PosterLoadThrottle(2);

        assertTrue(throttle.tryStart("http://host/a.webp"));
        assertTrue(throttle.tryStart("http://host/b.webp"));
        assertFalse(throttle.tryStart("http://host/c.webp"));
    }

    private static void finishAllowsUrlToStartAgain() {
        PosterLoadThrottle throttle = new PosterLoadThrottle(1);

        assertTrue(throttle.tryStart("http://host/a.webp"));
        throttle.finish("http://host/a.webp");
        assertTrue(throttle.tryStart("http://host/a.webp"));
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
