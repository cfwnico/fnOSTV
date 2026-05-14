package com.fnostv.android4.media;

import java.util.Locale;

public final class MediaLibraryClassifier {
    private static final String[] VIDEO_EXTENSIONS = {
            ".mp4", ".m4v", ".mov", ".3gp", ".3gpp", ".webm", ".mkv", ".avi",
            ".wmv", ".asf", ".ts", ".m2ts", ".flv", ".mpeg", ".mpg", ".vob",
            ".m3u8", ".rm", ".rmvb"
    };

    private MediaLibraryClassifier() {
    }

    public static String normalizePath(String path) {
        String value = path == null ? "" : path.trim().replace('\\', '/');
        while (value.indexOf("//") >= 0) {
            value = value.replace("//", "/");
        }
        while (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return "/".equals(value) ? "" : value;
    }

    public static boolean isSupportedVideo(String name) {
        String value = lower(name);
        int queryIndex = value.indexOf('?');
        if (queryIndex >= 0) {
            value = value.substring(0, queryIndex);
        }
        for (int i = 0; i < VIDEO_EXTENSIONS.length; i++) {
            if (value.endsWith(VIDEO_EXTENSIONS[i])) {
                return true;
            }
        }
        return false;
    }

    public static String inferCategory(String name, String path) {
        String value = lower(name) + " " + lower(path);
        if (containsAny(value, "s01", "e01", "season", "episode", "series", "tv", "电视剧", "电视节目", "剧集", "番剧")) {
            return MediaLibraryCategory.TV;
        }
        if (containsAny(value, "movie", "movies", "film", "films", "电影", "影片", "影院")) {
            return MediaLibraryCategory.MOVIE;
        }
        return MediaLibraryCategory.OTHER;
    }

    private static boolean containsAny(String value, String... terms) {
        for (int i = 0; i < terms.length; i++) {
            if (value.indexOf(terms[i]) >= 0) {
                return true;
            }
        }
        return false;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US);
    }
}
