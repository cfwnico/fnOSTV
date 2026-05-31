package com.fnostv.android4.config;

import org.json.JSONException;
import org.json.JSONObject;

public final class ServerProfile {
    public final int schemaVersion;
    public final String baseUrl;
    public final String username;
    public final String password;
    public final boolean trustSslErrors;
    public final boolean useHttps;

    public ServerProfile(String baseUrl, String username, String password, boolean trustSslErrors) {
        this(1, baseUrl, username, password, trustSslErrors, false);
    }

    public ServerProfile(String baseUrl, String username, String password, boolean trustSslErrors, boolean useHttps) {
        this(1, baseUrl, username, password, trustSslErrors, useHttps);
    }

    private ServerProfile(int schemaVersion, String baseUrl, String username, String password, boolean trustSslErrors, boolean useHttps) {
        this.schemaVersion = schemaVersion;
        this.useHttps = useHttps;
        this.baseUrl = normalizeBaseUrl(baseUrl, useHttps);
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password;
        this.trustSslErrors = trustSslErrors;
    }

    public boolean isReady() {
        return ProfileValidator.validate(this).isValid();
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("schemaVersion", schemaVersion);
        object.put("baseUrl", baseUrl);
        object.put("username", username);
        object.put("password", password);
        object.put("trustSslErrors", trustSslErrors);
        object.put("useHttps", useHttps);
        return object;
    }

    public static ServerProfile fromJson(String value) {
        if (value == null || value.length() == 0) {
            return empty();
        }
        try {
            JSONObject object = new JSONObject(value);
            return new ServerProfile(
                    object.optInt("schemaVersion", 1),
                    object.optString("baseUrl"),
                    object.optString("username"),
                    object.optString("password"),
                    object.optBoolean("trustSslErrors", false),
                    object.optBoolean("useHttps", false));
        } catch (JSONException ignored) {
            return empty();
        }
    }

    public static ServerProfile empty() {
        return new ServerProfile("", "", "", false, false);
    }

    private static String normalizeBaseUrl(String raw, boolean useHttps) {
        if (raw == null) {
            return "";
        }
        String url = raw.trim();
        if (url.length() == 0) {
            return "";
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = (useHttps ? "https://" : "http://") + url;
        }
        int authorityEnd = firstIndexOf(url, new char[]{'/', '?', '#'}, url.indexOf("://") + 3);
        if (authorityEnd > 0) {
            url = url.substring(0, authorityEnd);
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static int firstIndexOf(String value, char[] chars, int start) {
        int first = -1;
        for (int i = 0; i < chars.length; i++) {
            int index = value.indexOf(chars[i], start);
            if (index >= 0 && (first < 0 || index < first)) {
                first = index;
            }
        }
        return first;
    }
}
