package com.fnostv.android4.media;

public final class MediaLibraryCategory {
    public static final String ALL = "all";
    public static final String MOVIE = "movie";
    public static final String TV = "tv";
    public static final String OTHER = "other";

    private MediaLibraryCategory() {
    }

    public static String normalize(String category) {
        if (MOVIE.equals(category) || TV.equals(category) || OTHER.equals(category)) {
            return category;
        }
        return ALL;
    }

    public static String label(String category) {
        String value = normalize(category);
        if (MOVIE.equals(value)) {
            return "电影";
        }
        if (TV.equals(value)) {
            return "电视节目";
        }
        if (OTHER.equals(value)) {
            return "其他";
        }
        return "全部";
    }
}
