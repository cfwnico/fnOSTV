package com.fnostv.android4.net;

import org.json.JSONObject;

public final class FnosFileEntry {
    public final String name;
    public final String path;
    public final boolean directory;
    public final long size;
    public final String type;

    public FnosFileEntry(String name, String path, boolean directory, long size, String type) {
        this.name = name == null ? "" : name;
        this.path = path == null ? "" : path;
        this.directory = directory;
        this.size = size;
        this.type = type == null ? "" : type;
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
                object.optString("type"));
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
}
