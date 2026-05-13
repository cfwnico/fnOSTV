package com.fnostv.android4.ui;

import com.fnostv.android4.net.FnosFileEntry;

import java.util.ArrayList;
import java.util.List;

public final class HomePosterSlotsTest {
    public static void main(String[] args) {
        selectsPosterBackedEntriesForEachHomeSection();
        selectsDirectoryPosterForMediaLibrarySection();
        fallsBackToFirstVideoWhenPosterIsMissing();
    }

    private static void selectsDirectoryPosterForMediaLibrarySection() {
        List<FnosFileEntry> media = new ArrayList<FnosFileEntry>();
        media.add(new FnosFileEntry("文化大观园", "tv-guid", true, 0L, "TV", "", "/poster/tv.webp"));

        HomePosterSlots slots = HomePosterSlots.from(media, null, null);

        assertEquals("文化大观园", slots.media.name);
        assertEquals("/poster/tv.webp", slots.media.posterPath);
    }

    private static void selectsPosterBackedEntriesForEachHomeSection() {
        List<FnosFileEntry> media = new ArrayList<FnosFileEntry>();
        media.add(entry("Media One", "/media/one", ""));
        media.add(entry("Media Two", "/media/two", "/poster/media-two.webp"));
        List<FnosFileEntry> recent = new ArrayList<FnosFileEntry>();
        recent.add(entry("Recent One", "/recent/one", "/poster/recent-one.webp"));
        List<FnosFileEntry> favorite = new ArrayList<FnosFileEntry>();
        favorite.add(entry("Favorite One", "/favorite/one", "/poster/favorite-one.webp"));

        HomePosterSlots slots = HomePosterSlots.from(media, recent, favorite);

        assertEquals("Media Two", slots.media.name);
        assertEquals("/poster/media-two.webp", slots.media.posterPath);
        assertEquals("Recent One", slots.recent.name);
        assertEquals("/poster/recent-one.webp", slots.recent.posterPath);
        assertEquals("Favorite One", slots.favorite.name);
        assertEquals("/poster/favorite-one.webp", slots.favorite.posterPath);
    }

    private static void fallsBackToFirstVideoWhenPosterIsMissing() {
        List<FnosFileEntry> media = new ArrayList<FnosFileEntry>();
        media.add(entry("Plain Video", "/media/plain", ""));

        HomePosterSlots slots = HomePosterSlots.from(media, null, null);

        assertEquals("Plain Video", slots.media.name);
        assertNull(slots.recent);
        assertNull(slots.favorite);
    }

    private static FnosFileEntry entry(String name, String path, String posterPath) {
        return new FnosFileEntry(name, path, false, 0L, "Video", "", posterPath);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }

    private static void assertNull(Object actual) {
        if (actual != null) {
            throw new AssertionError("Expected null but was " + actual);
        }
    }
}
