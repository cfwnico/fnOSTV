package com.fnostv.android4.media;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MediaLibrary {
    public final String id;
    public final String name;
    public final String category;
    public final List<String> paths;
    public final boolean enabled;
    public final int sortOrder;
    public final long updatedAt;

    public MediaLibrary(String id, String name, String category, List<String> paths, boolean enabled, int sortOrder, long updatedAt) {
        this.id = id == null ? "" : id;
        this.name = emptyToDefault(name, "影视大全");
        this.category = MediaLibraryCategory.normalize(category);
        this.paths = Collections.unmodifiableList(normalizePaths(paths));
        this.enabled = enabled;
        this.sortOrder = sortOrder;
        this.updatedAt = updatedAt;
    }

    public MediaLibrary withUpdatedAt(long value) {
        return new MediaLibrary(id, name, category, paths, enabled, sortOrder, value);
    }

    public String categoryLabel() {
        return MediaLibraryCategory.label(category);
    }

    private static List<String> normalizePaths(List<String> paths) {
        List<String> values = new ArrayList<String>();
        if (paths == null) {
            return values;
        }
        for (int i = 0; i < paths.size(); i++) {
            String value = MediaLibraryClassifier.normalizePath(paths.get(i));
            if (value.length() > 0 && !values.contains(value)) {
                values.add(value);
            }
        }
        return values;
    }

    private static String emptyToDefault(String value, String fallback) {
        String text = value == null ? "" : value.trim();
        return text.length() == 0 ? fallback : text;
    }
}
