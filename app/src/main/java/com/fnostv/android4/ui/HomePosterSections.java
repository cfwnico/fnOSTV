package com.fnostv.android4.ui;

import com.fnostv.android4.media.MediaLibraryCategory;
import com.fnostv.android4.media.MediaLibraryClassifier;
import com.fnostv.android4.net.FnosFileEntry;

import java.util.ArrayList;
import java.util.List;

public final class HomePosterSections {
    private static final int RECENT_LIMIT = 6;
    private static final int FAVORITE_LIMIT = 6;
    private static final int CATEGORY_LIMIT = 6;
    private static final int MEDIA_LIMIT = 8;

    private HomePosterSections() {
    }

    public static List<HomePosterSection> from(List<FnosFileEntry> media, List<FnosFileEntry> recent, List<FnosFileEntry> favorite) {
        List<HomePosterSection> sections = new ArrayList<HomePosterSection>();
        List<FnosFileEntry> recentEntries = clean(recent);
        if (recentEntries.size() > 0) {
            sections.add(section("\u7ee7\u7eed\u89c2\u770b", "Continue Watching", NativeHomeView.ACTION_RECENT, recentEntries, RECENT_LIMIT));
        }
        List<FnosFileEntry> favoriteEntries = clean(favorite);
        if (favoriteEntries.size() > 0) {
            sections.add(section("\u6211\u7684\u6536\u85cf", "Favorites", NativeHomeView.ACTION_FAVORITES, favoriteEntries, FAVORITE_LIMIT));
        }
        List<FnosFileEntry> mediaEntries = clean(media);
        addCategory(sections, mediaEntries, MediaLibraryCategory.MOVIE, "\u7535\u5f71", "Movies", NativeHomeView.ACTION_MOVIES);
        addCategory(sections, mediaEntries, MediaLibraryCategory.TV, "\u7535\u89c6\u5267", "Series", NativeHomeView.ACTION_TV);
        addCategory(sections, mediaEntries, MediaLibraryCategory.OTHER, "\u5176\u4ed6", "Other", NativeHomeView.ACTION_OTHER);
        sections.add(section("\u5f71\u89c6\u5927\u5168", "All Media", NativeHomeView.ACTION_MEDIA, mediaEntries, MEDIA_LIMIT));
        return sections;
    }

    private static void addCategory(
            List<HomePosterSection> sections,
            List<FnosFileEntry> entries,
            String category,
            String title,
            String analyticsName,
            String action) {
        List<FnosFileEntry> categoryEntries = new ArrayList<FnosFileEntry>();
        for (int i = 0; i < entries.size(); i++) {
            FnosFileEntry entry = entries.get(i);
            if (category.equals(MediaLibraryClassifier.inferCategory(entry.name, entry.path))) {
                categoryEntries.add(entry);
            }
        }
        if (categoryEntries.size() > 0) {
            sections.add(section(title, analyticsName, action, categoryEntries, CATEGORY_LIMIT));
        }
    }

    private static HomePosterSection section(String title, String analyticsName, String action, List<FnosFileEntry> entries, int limit) {
        return new HomePosterSection(title, analyticsName, action, entries, limit);
    }

    private static List<FnosFileEntry> clean(List<FnosFileEntry> entries) {
        List<FnosFileEntry> values = new ArrayList<FnosFileEntry>();
        if (entries == null) {
            return values;
        }
        for (int i = 0; i < entries.size(); i++) {
            FnosFileEntry entry = entries.get(i);
            if (entry == null || containsPath(values, entry.path)) {
                continue;
            }
            if (entry.isVideo() || (entry.directory && entry.posterPath.length() > 0)) {
                values.add(entry);
            }
        }
        return values;
    }

    private static boolean containsPath(List<FnosFileEntry> entries, String path) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).path.equals(path)) {
                return true;
            }
        }
        return false;
    }
}
