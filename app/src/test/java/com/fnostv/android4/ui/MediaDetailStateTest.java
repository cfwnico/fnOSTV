package com.fnostv.android4.ui;

import com.fnostv.android4.net.FnosFileEntry;
import com.fnostv.android4.net.FnosPlaybackSource;
import com.fnostv.android4.net.MediaDetailInfo;

import java.util.ArrayList;
import java.util.List;

public final class MediaDetailStateTest {
    public static void main(String[] args) {
        emptySourcesAreSafe();
        selectsCurrentSource();
        storesLoadingAndErrorState();
        selectedSourceCanBeMovedFirstForPlayback();
        storesDetailInfoAndChildren();
        detailErrorDoesNotClearPlaybackSources();
    }

    private static void emptySourcesAreSafe() {
        MediaDetailState state = new MediaDetailState(entry(), false);
        assertEquals("无可用播放源", state.sourceLabel());
        assertEquals(null, state.currentSource());
    }

    private static void selectsCurrentSource() {
        MediaDetailState state = new MediaDetailState(entry(), true);
        List<FnosPlaybackSource> sources = new ArrayList<FnosPlaybackSource>();
        sources.add(new FnosPlaybackSource("原画", "http://host/a.mp4"));
        sources.add(new FnosPlaybackSource("1080P", "http://host/b.mp4"));

        state.setSources(sources);
        state.selectSource(8);

        assertEquals("2/2 1080P", state.sourceLabel());
        assertEquals("http://host/b.mp4", state.currentSource().url);
        assertEquals(true, state.favorite);
    }

    private static void storesLoadingAndErrorState() {
        MediaDetailState state = new MediaDetailState(entry(), false);
        state.setLoadingSources(true);
        assertEquals(true, state.loadingSources);
        state.setError("播放源准备失败");
        assertEquals(false, state.loadingSources);
        assertEquals("播放源准备失败", state.errorMessage);
    }

    private static void selectedSourceCanBeMovedFirstForPlayback() {
        MediaDetailState state = new MediaDetailState(entry(), false);
        List<FnosPlaybackSource> sources = new ArrayList<FnosPlaybackSource>();
        sources.add(new FnosPlaybackSource("原画", "http://host/a.mp4"));
        sources.add(new FnosPlaybackSource("1080P", "http://host/b.mp4"));
        state.setSources(sources);
        state.selectSource(1);

        List<FnosPlaybackSource> ordered = state.sourcesForPlayback();

        assertEquals("1080P", ordered.get(0).label);
        assertEquals("原画", ordered.get(1).label);
    }

    private static void storesDetailInfoAndChildren() {
        MediaDetailState state = new MediaDetailState(entry(), false);
        List<FnosFileEntry> children = new ArrayList<FnosFileEntry>();
        children.add(new FnosFileEntry("第一集", "ep-1", false, 0L, "Video", ""));
        children.add(new FnosFileEntry("第二集", "ep-2", false, 0L, "Video", ""));
        MediaDetailInfo info = new MediaDetailInfo(
                "文化大观园",
                "",
                "一档文化纪录片",
                "2024",
                "8.6",
                "纪录片",
                "45:30",
                "TV",
                children);

        state.setLoadingDetail(true);
        assertEquals(true, state.loadingDetail);
        state.setDetailInfo(info);

        assertEquals(false, state.loadingDetail);
        assertEquals("文化大观园", state.detailInfo.title);
        assertEquals(2, state.detailChildren().size());
        assertEquals("第一集", state.detailChildren().get(0).name);
    }

    private static void detailErrorDoesNotClearPlaybackSources() {
        MediaDetailState state = new MediaDetailState(entry(), false);
        List<FnosPlaybackSource> sources = new ArrayList<FnosPlaybackSource>();
        sources.add(new FnosPlaybackSource("原画", "http://host/a.mp4"));
        state.setSources(sources);

        state.setDetailError("详情信息暂不可用");

        assertEquals("详情信息暂不可用", state.detailError);
        assertEquals(1, state.sources().size());
        assertEquals("原画", state.sources().get(0).label);
    }

    private static FnosFileEntry entry() {
        return new FnosFileEntry("Movie.mkv", "/video/Movie.mkv", false, 1024L, "video/x-matroska", "");
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
