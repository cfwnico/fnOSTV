package com.fnostv.android4.media;

public final class MediaLibraryClassifierTest {
    public static void main(String[] args) {
        assertEquals("/video/Movies", MediaLibraryClassifier.normalizePath(" /video/Movies/ "));
        assertEquals("", MediaLibraryClassifier.normalizePath(" / "));
        assertEquals(MediaLibraryCategory.MOVIE, MediaLibraryClassifier.inferCategory("Movies", "/volume1/Movies"));
        assertEquals(MediaLibraryCategory.TV, MediaLibraryClassifier.inferCategory("Show.S01E02.mkv", "/volume1/TV/Show"));
        assertEquals(MediaLibraryCategory.OTHER, MediaLibraryClassifier.inferCategory("family.mov", "/volume1/HomeVideo"));
        assertTrue(MediaLibraryClassifier.isSupportedVideo("clip.rmvb"));
        assertFalse(MediaLibraryClassifier.isSupportedVideo("poster.jpg"));
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
