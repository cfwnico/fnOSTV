package com.fnostv.android4.ui;

import com.fnostv.android4.net.FnosFileEntry;

import java.util.ArrayList;
import java.util.List;

public final class HomePosterSections {
    private static final int RECENT_LIMIT = 6;
    private static final int FAVORITE_LIMIT = 6;
    private static final int MEDIA_LIMIT = 8;

    private HomePosterSections() {
    }

    public static List<HomePosterSection> from(List<FnosFileEntry> media, List<FnosFileEntry> recent, List<FnosFileEntry> favorite) {
        List<HomePosterSection> sections = new ArrayList<HomePosterSection>();
        List<FnosFileEntry> recentEntries = clean(recent);
        if (recentEntries.size() > 0) {
            sections.add(new HomePosterSection("继续观看", NativeHomeView.ACTION_RECENT, recentEntries, RECENT_LIMIT));
        }
        List<FnosFileEntry> favoriteEntries = clean(favorite);
        if (favoriteEntries.size() > 0) {
            sections.add(new HomePosterSection("我的收藏", NativeHomeView.ACTION_FAVORITES, favoriteEntries, FAVORITE_LIMIT));
        }
        sections.add(new HomePosterSection("影视大全", NativeHomeView.ACTION_MEDIA, clean(media), MEDIA_LIMIT));
        return sections;
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
