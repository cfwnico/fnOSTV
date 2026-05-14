package com.fnostv.android4.ui;

import com.fnostv.android4.net.FnosFileEntry;

public final class FileBrowserLabels {
    private static final int CARD_TITLE_LIMIT = 8;

    private FileBrowserLabels() {
    }

    public static String mediaFallbackTitle() {
        return "影视入口";
    }

    public static String mediaFallbackSubtitle(String path) {
        String value = path == null ? "" : path.trim();
        return value.length() == 0 ? mediaFallbackTitle() : value;
    }

    public static String posterPlaceholder(FnosFileEntry entry, boolean favorite) {
        if (entry == null) {
            return "";
        }
        String name = cardTitle(entry);
        String badge = entry.directory ? "目录" : mediaFormat(entry);
        if (favorite) {
            badge = "* " + badge;
        }
        return name + "\n" + badge;
    }

    public static String cardTitle(FnosFileEntry entry) {
        String name = entry == null ? "" : entry.name.trim();
        if (name.length() == 0) {
            return "未命名";
        }
        if (name.length() <= CARD_TITLE_LIMIT) {
            return name;
        }
        return name.substring(0, CARD_TITLE_LIMIT) + "...";
    }

    private static String mediaFormat(FnosFileEntry entry) {
        String label = entry.formatLabel();
        if (!"video".equals(label)) {
            return label;
        }
        String type = entry.type == null ? "" : entry.type.trim();
        int slash = type.lastIndexOf('/');
        if (slash >= 0 && slash < type.length() - 1) {
            type = type.substring(slash + 1);
        }
        if (type.startsWith("x-ms-")) {
            type = type.substring(5);
        }
        if (type.length() == 0) {
            return label;
        }
        return type.toUpperCase();
    }
}
