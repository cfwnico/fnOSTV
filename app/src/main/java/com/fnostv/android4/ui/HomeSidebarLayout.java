package com.fnostv.android4.ui;

final class HomeSidebarLayout {
    static final int MAX_CONTENT_HEIGHT_DP = 520;
    static final int PADDING_TOP = 14;
    static final int PADDING_BOTTOM = 8;
    static final int ROW_HEIGHT = 30;
    static final int SECTION_HEIGHT = 24;
    static final int BRAND_BOTTOM = 10;
    static final int GROUP_BOTTOM = 12;
    static final int ROW_BOTTOM = 2;

    private HomeSidebarLayout() {
    }

    static int estimatedContentHeightDp() {
        int brand = ROW_HEIGHT + BRAND_BOTTOM;
        int topRows = ROW_HEIGHT + ROW_BOTTOM + ROW_HEIGHT + GROUP_BOTTOM;
        int media = SECTION_HEIGHT + ROW_BOTTOM + ROW_HEIGHT + GROUP_BOTTOM;
        int categories = SECTION_HEIGHT + ROW_BOTTOM + 4 * (ROW_HEIGHT + ROW_BOTTOM);
        return PADDING_TOP + brand + topRows + media + categories + PADDING_BOTTOM;
    }
}
