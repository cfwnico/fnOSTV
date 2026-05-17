package com.fnostv.android4.player;

import com.fnostv.android4.net.FnosPlaybackSource;

import java.util.ArrayList;
import java.util.List;

public final class PlaybackSourceSelectorTest {
    public static void main(String[] args) {
        filtersInvalidAndDuplicateSources();
        clampsSourceIndex();
        formatsDisplayLabels();
        appendsUsefulProtocolAndFormatHints();
    }

    private static void filtersInvalidAndDuplicateSources() {
        List<FnosPlaybackSource> input = new ArrayList<FnosPlaybackSource>();
        input.add(new FnosPlaybackSource("Original", "http://host/a.mp4"));
        input.add(new FnosPlaybackSource("Duplicate", "http://host/a.mp4"));
        input.add(new FnosPlaybackSource("Empty", ""));
        input.add(new FnosPlaybackSource("HD", "http://host/b.mp4"));

        List<FnosPlaybackSource> result = PlaybackSourceSelector.normalize(input);

        assertEquals(2, result.size());
        assertEquals("Original", result.get(0).label);
        assertEquals("http://host/a.mp4", result.get(0).url);
        assertEquals("HD", result.get(1).label);
        assertEquals("http://host/b.mp4", result.get(1).url);
    }

    private static void clampsSourceIndex() {
        List<FnosPlaybackSource> input = new ArrayList<FnosPlaybackSource>();
        input.add(new FnosPlaybackSource("Original", "http://host/a.mp4"));
        input.add(new FnosPlaybackSource("HD", "http://host/b.mp4"));

        assertEquals(0, PlaybackSourceSelector.clampIndex(input, -1));
        assertEquals(1, PlaybackSourceSelector.clampIndex(input, 9));
        assertEquals(0, PlaybackSourceSelector.clampIndex(new ArrayList<FnosPlaybackSource>(), 9));
    }

    private static void formatsDisplayLabels() {
        List<FnosPlaybackSource> input = new ArrayList<FnosPlaybackSource>();
        input.add(new FnosPlaybackSource("Original", "http://host/a.mp4"));
        input.add(new FnosPlaybackSource("1080P", "http://host/b.mp4"));

        assertEquals("1/2 Original · MP4", PlaybackSourceSelector.displayLabel(input, 0));
        assertEquals("2/2 1080P · MP4", PlaybackSourceSelector.displayLabel(input, 1));
        assertTrue(PlaybackSourceSelector.displayLabel(new ArrayList<FnosPlaybackSource>(), 0).length() > 0);
    }

    private static void appendsUsefulProtocolAndFormatHints() {
        List<FnosPlaybackSource> input = new ArrayList<FnosPlaybackSource>();
        input.add(new FnosPlaybackSource("LAN", "http://host/movie.m3u8?token=1"));
        input.add(new FnosPlaybackSource("Direct", "/volume1/movie.mkv"));

        assertEquals("1/2 LAN · HLS", PlaybackSourceSelector.displayLabel(input, 0));
        assertEquals("2/2 Direct · MKV · local", PlaybackSourceSelector.displayLabel(input, 1));
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
}
