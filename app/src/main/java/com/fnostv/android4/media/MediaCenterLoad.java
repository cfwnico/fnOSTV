package com.fnostv.android4.media;

import com.fnostv.android4.net.FnosFileList;

public final class MediaCenterLoad {
    public static final String SOURCE_REST_LIBRARY = "rest-library";
    public static final String SOURCE_REST_ITEMS = "rest-items";
    public static final String SOURCE_LOCAL_INDEX = "local-index";
    public static final String SOURCE_RPC_MEDIACENTER = "rpc-mediacenter";
    public static final String SOURCE_FILE_FALLBACK = "file-fallback";

    public final boolean success;
    public final String title;
    public final String subtitle;
    public final FnosFileList list;
    public final boolean sortEntries;
    public final String message;
    public final String source;

    private MediaCenterLoad(boolean success, String title, String subtitle, FnosFileList list, boolean sortEntries, String message, String source) {
        this.success = success;
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.list = list;
        this.sortEntries = sortEntries;
        this.message = message == null ? "" : message;
        this.source = source == null ? "" : source;
    }

    public static MediaCenterLoad success(String title, String subtitle, FnosFileList list, boolean sortEntries, String source) {
        return new MediaCenterLoad(true, title, subtitle, list, sortEntries, "", source);
    }

    public static MediaCenterLoad failure(String message) {
        return new MediaCenterLoad(false, "", "", null, true, message, "");
    }
}
