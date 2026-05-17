package com.fnostv.android4.player;

import com.fnostv.android4.net.FnosPlaybackSource;

import java.util.ArrayList;
import java.util.List;

public final class PlaybackSourceSelectorTest {
    public static void main(String[] args) {
        filtersInvalidAndDuplicateSources();
        clampsSourceIndex();
        formatsDisplayLabels();
    }

    private static void filtersInvalidAndDuplicateSources() {
        List<FnosPlaybackSource> input = new ArrayList<FnosPlaybackSource>();
        input.add(new FnosPlaybackSource("原画", "http://host/a.mp4"));
        input.add(new FnosPlaybackSource("重复", "http://host/a.mp4"));
        input.add(new FnosPlaybackSource("空", ""));
        input.add(new FnosPlaybackSource("高清", "http://host/b.mp4"));

        List<FnosPlaybackSource> result = PlaybackSourceSelector.normalize(input);

        assertEquals(2, result.size());
        assertEquals("原画", result.get(0).label);
        assertEquals("http://host/a.mp4", result.get(0).url);
        assertEquals("高清", result.get(1).label);
        assertEquals("http://host/b.mp4", result.get(1).url);
    }

    private static void clampsSourceIndex() {
        List<FnosPlaybackSource> input = new ArrayList<FnosPlaybackSource>();
        input.add(new FnosPlaybackSource("原画", "http://host/a.mp4"));
        input.add(new FnosPlaybackSource("高清", "http://host/b.mp4"));

        assertEquals(0, PlaybackSourceSelector.clampIndex(input, -1));
        assertEquals(1, PlaybackSourceSelector.clampIndex(input, 9));
        assertEquals(0, PlaybackSourceSelector.clampIndex(new ArrayList<FnosPlaybackSource>(), 9));
    }

    private static void formatsDisplayLabels() {
        List<FnosPlaybackSource> input = new ArrayList<FnosPlaybackSource>();
        input.add(new FnosPlaybackSource("原画", "http://host/a.mp4"));
        input.add(new FnosPlaybackSource("1080P", "http://host/b.mp4"));

        assertEquals("1/2 原画", PlaybackSourceSelector.displayLabel(input, 0));
        assertEquals("2/2 1080P", PlaybackSourceSelector.displayLabel(input, 1));
        assertEquals("无可用播放源", PlaybackSourceSelector.displayLabel(new ArrayList<FnosPlaybackSource>(), 0));
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
