package com.fnostv.android4.net;

import org.json.JSONObject;

public final class FnosFileEntry {
    public final String name;
    public final String path;
    public final boolean directory;
    public final long size;
    public final String type;
    public final String mediaUrl;

    public FnosFileEntry(String name, String path, boolean directory, long size, String type, String mediaUrl) {
        this.name = name == null ? "" : name;
        this.path = path == null ? "" : path;
        this.directory = directory;
        this.size = size;
        this.type = type == null ? "" : type;
        this.mediaUrl = mediaUrl == null ? "" : mediaUrl;
    }

    static FnosFileEntry fromJson(JSONObject object, String parentPath, String uid) {
        String name = object.optString("name");
        String path = object.optString("path");
        if (path.length() == 0) {
            path = rootPath(object, parentPath, uid, name);
        }
        boolean directory = object.optInt("dir", 0) == 1 || object.optBoolean("dir");
        return new FnosFileEntry(
                name,
                path,
                directory,
                object.optLong("size", 0L),
                object.optString("type"),
                mediaUrl(object, path));
    }

    public boolean isVideo() {
        String value = lower(type);
        if (value.indexOf("video") >= 0) {
            return true;
        }
        value = lower(name);
        return value.endsWith(".mp4")
                || value.endsWith(".m4v")
                || value.endsWith(".mov")
                || value.endsWith(".3gp")
                || value.endsWith(".webm")
                || value.endsWith(".mkv")
                || value.endsWith(".avi")
                || value.endsWith(".ts")
                || value.endsWith(".m2ts")
                || value.endsWith(".flv");
    }

    public String playbackUrl() {
        if (mediaUrl.length() > 0) {
            return mediaUrl;
        }
        return isHttpUrl(path) ? path : "";
    }

    public String mimeType() {
        if (type.length() > 0 && type.indexOf('/') > 0) {
            return type;
        }
        String value = lower(name);
        if (value.endsWith(".mp4") || value.endsWith(".m4v")) {
            return "video/mp4";
        }
        if (value.endsWith(".3gp")) {
            return "video/3gpp";
        }
        if (value.endsWith(".webm")) {
            return "video/webm";
        }
        return "video/*";
    }

    private static String rootPath(JSONObject object, String parentPath, String uid, String name) {
        if (parentPath != null && parentPath.length() > 0) {
            return join(parentPath, name);
        }
        String volume = object.optString("v");
        if (volume.length() == 0) {
            volume = object.optString("vid");
        }
        if (volume.length() > 0 && uid != null && uid.length() > 0) {
            return "vol" + volume + "/" + uid + "/" + name;
        }
        return name;
    }

    private static String join(String parentPath, String name) {
        if (parentPath == null || parentPath.length() == 0) {
            return name;
        }
        if (name == null || name.length() == 0) {
            return parentPath;
        }
        return parentPath.endsWith("/") ? parentPath + name : parentPath + "/" + name;
    }

    private static String mediaUrl(JSONObject object, String fallbackPath) {
        String[] keys = {"url", "playUrl", "downloadUrl", "fileUrl", "mediaUrl", "realUrl", "link", "src"};
        for (int i = 0; i < keys.length; i++) {
            String value = object.optString(keys[i]);
            if (isHttpUrl(value)) {
                return value;
            }
        }
        return isHttpUrl(fallbackPath) ? fallbackPath : "";
    }

    private static boolean isHttpUrl(String value) {
        return value != null && (value.startsWith("http://") || value.startsWith("https://"));
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
