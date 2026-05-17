package com.fnostv.android4.ui;

import com.fnostv.android4.net.FnosFileEntry;

import java.util.ArrayList;
import java.util.List;

public final class HomePosterSectionsTest {
    public static void main(String[] args) {
        buildsRecentFavoritesCategoryAndMediaSectionsInOrder();
        limitsVisibleEntriesAndMarksHasMore();
        removesDuplicatePathsInsideEachSection();
        keepsMediaSectionWhenAllListsAreEmpty();
    }

    private static void buildsRecentFavoritesCategoryAndMediaSectionsInOrder() {
        List<HomePosterSection> sections = HomePosterSections.from(
                list(
                        entry("Movie One", "/video/Movies/one.mp4", "/poster/one.webp"),
                        entry("Show S01E01", "/video/TV/show.s01e01.mkv", "/poster/show.webp"),
                        entry("Family", "/video/Home/family.mov", "/poster/family.webp")),
                list(entry("Recent One", "/recent/one.mp4", "/poster/recent.webp")),
                list(entry("Favorite One", "/favorite/one.mp4", "/poster/favorite.webp")));

        assertEquals("Continue Watching", sections.get(0).analyticsName);
        assertEquals(NativeHomeView.ACTION_RECENT, sections.get(0).action);
        assertEquals("Favorites", sections.get(1).analyticsName);
        assertEquals(NativeHomeView.ACTION_FAVORITES, sections.get(1).action);
        assertEquals("Movies", sections.get(2).analyticsName);
        assertEquals(NativeHomeView.ACTION_MOVIES, sections.get(2).action);
        assertEquals("Series", sections.get(3).analyticsName);
        assertEquals(NativeHomeView.ACTION_TV, sections.get(3).action);
        assertEquals("Other", sections.get(4).analyticsName);
        assertEquals(NativeHomeView.ACTION_OTHER, sections.get(4).action);
        assertEquals("All Media", sections.get(5).analyticsName);
        assertEquals(NativeHomeView.ACTION_MEDIA, sections.get(5).action);
    }

    private static void limitsVisibleEntriesAndMarksHasMore() {
        List<FnosFileEntry> recent = new ArrayList<FnosFileEntry>();
        for (int i = 0; i < 8; i++) {
            recent.add(entry("Recent " + i, "/recent/" + i + ".mp4", "/poster/" + i + ".webp"));
        }

        HomePosterSection section = HomePosterSections.from(null, recent, null).get(0);

        assertEquals(6, section.visibleEntries().size());
        assertTrue(section.hasMore);
    }

    private static void removesDuplicatePathsInsideEachSection() {
        List<FnosFileEntry> media = list(
                entry("First", "/same/path.mp4", "/poster/first.webp"),
                entry("Duplicate", "/same/path.mp4", "/poster/duplicate.webp"));

        HomePosterSection section = HomePosterSections.from(media, null, null).get(0);

        assertEquals(1, section.entries.size());
        assertEquals("First", section.entries.get(0).name);
    }

    private static void keepsMediaSectionWhenAllListsAreEmpty() {
        List<HomePosterSection> sections = HomePosterSections.from(null, null, null);

        assertEquals(1, sections.size());
        assertEquals("All Media", sections.get(0).analyticsName);
        assertEquals(0, sections.get(0).entries.size());
    }

    private static List<FnosFileEntry> list(FnosFileEntry... entries) {
        List<FnosFileEntry> values = new ArrayList<FnosFileEntry>();
        for (int i = 0; i < entries.length; i++) {
            values.add(entries[i]);
        }
        return values;
    }

    private static FnosFileEntry entry(String name, String path, String posterPath) {
        return new FnosFileEntry(name, path, false, 0L, "Video", "", posterPath);
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
