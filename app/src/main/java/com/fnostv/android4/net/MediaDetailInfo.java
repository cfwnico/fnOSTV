package com.fnostv.android4.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MediaDetailInfo {
    public final String title;
    public final String subtitle;
    public final String overview;
    public final String year;
    public final String rating;
    public final String category;
    public final String durationLabel;
    public final String sourceLabel;
    public final List<FnosFileEntry> children;

    public MediaDetailInfo(
            String title,
            String subtitle,
            String overview,
            String year,
            String rating,
            String category,
            String durationLabel,
            String sourceLabel,
            List<FnosFileEntry> children) {
        this.title = clean(title);
        this.subtitle = clean(subtitle);
        this.overview = clean(overview);
        this.year = clean(year);
        this.rating = clean(rating);
        this.category = clean(category);
        this.durationLabel = clean(durationLabel);
        this.sourceLabel = clean(sourceLabel);
        this.children = Collections.unmodifiableList(children == null
                ? new ArrayList<FnosFileEntry>()
                : new ArrayList<FnosFileEntry>(children));
    }

    public static MediaDetailInfo empty() {
        return new MediaDetailInfo("", "", "", "", "", "", "", "", null);
    }

    public boolean isEmpty() {
        return title.length() == 0
                && subtitle.length() == 0
                && overview.length() == 0
                && year.length() == 0
                && rating.length() == 0
                && category.length() == 0
                && durationLabel.length() == 0
                && sourceLabel.length() == 0
                && children.size() == 0;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
