package com.fnostv.android4.ui;

import com.fnostv.android4.net.FnosFileEntry;

public final class FileBrowserLabelsTest {
    public static void main(String[] args) {
        mediaFallbackTitleDoesNotMentionFileMode();
        mediaFallbackSubtitleDoesNotMentionFileMode();
        placeholderUsesNameAndTypeInsteadOfGenericFileModeText();
        truncatesLongTitlesForCards();
        givesUnnamedEntriesVisibleFallbackText();
    }

    private static void mediaFallbackTitleDoesNotMentionFileMode() {
        assertEquals("影视入口", FileBrowserLabels.mediaFallbackTitle());
    }

    private static void mediaFallbackSubtitleDoesNotMentionFileMode() {
        assertEquals("/vol2/1000/测试", FileBrowserLabels.mediaFallbackSubtitle("/vol2/1000/测试"));
        assertEquals("影视入口", FileBrowserLabels.mediaFallbackSubtitle(""));
    }

    private static void placeholderUsesNameAndTypeInsteadOfGenericFileModeText() {
        FnosFileEntry directory = new FnosFileEntry("文化观光", "/vol2/1000/测试/文化观光", true, 0L, "Directory", "");
        FnosFileEntry video = new FnosFileEntry("1.wmv", "/vol2/1000/测试/1.wmv", false, 0L, "video/x-ms-wmv", "");

        assertEquals("文化观光\n目录", FileBrowserLabels.posterPlaceholder(directory, false));
        assertEquals("1.wmv\nWMV", FileBrowserLabels.posterPlaceholder(video, false));
    }

    private static void truncatesLongTitlesForCards() {
        FnosFileEntry entry = new FnosFileEntry("非常非常长的影视标题.wmv", "/video", false, 0L, "video/x-ms-wmv", "");

        assertEquals("非常非常长的影视...", FileBrowserLabels.cardTitle(entry));
    }

    private static void givesUnnamedEntriesVisibleFallbackText() {
        FnosFileEntry entry = new FnosFileEntry("", "/video", false, 0L, "video/x-ms-wmv", "");

        assertEquals("未命名", FileBrowserLabels.cardTitle(entry));
        assertEquals("未命名\nWMV", FileBrowserLabels.posterPlaceholder(entry, false));
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
