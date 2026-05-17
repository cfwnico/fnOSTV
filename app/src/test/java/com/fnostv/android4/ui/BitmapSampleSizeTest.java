package com.fnostv.android4.ui;

public final class BitmapSampleSizeTest {
    public static void main(String[] args) {
        largePosterUsesPowerOfTwoDownsample();
        widePosterUsesModerateDownsample();
        smallPosterKeepsOriginalSize();
        invalidBoundsKeepOriginalSize();
    }

    private static void largePosterUsesPowerOfTwoDownsample() {
        assertEquals(4, BitmapSampleSize.forBounds(4000, 3000, 720, 720));
    }

    private static void widePosterUsesModerateDownsample() {
        assertEquals(2, BitmapSampleSize.forBounds(1600, 900, 720, 720));
    }

    private static void smallPosterKeepsOriginalSize() {
        assertEquals(1, BitmapSampleSize.forBounds(500, 300, 720, 720));
    }

    private static void invalidBoundsKeepOriginalSize() {
        assertEquals(1, BitmapSampleSize.forBounds(0, 300, 720, 720));
        assertEquals(1, BitmapSampleSize.forBounds(300, 0, 720, 720));
        assertEquals(1, BitmapSampleSize.forBounds(300, 300, 0, 720));
        assertEquals(1, BitmapSampleSize.forBounds(300, 300, 720, 0));
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
