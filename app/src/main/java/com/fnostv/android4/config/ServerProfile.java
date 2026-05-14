package com.fnostv.android4.config;

import org.json.JSONException;
import org.json.JSONObject;

public final class ServerProfile {
    public final int schemaVersion;
    public final String baseUrl;
    public final String username;
    public final String password;
    public final boolean autoLogin;
    public final boolean trustSslErrors;

    public ServerProfile(String baseUrl, String username, String password, boolean autoLogin, boolean trustSslErrors) {
        this(1, baseUrl, username, password, autoLogin, trustSslErrors);
    }

    private ServerProfile(int schemaVersion, String baseUrl, String username, String password, boolean autoLogin, boolean trustSslErrors) {
        this.schemaVersion = schemaVersion;
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password;
        this.autoLogin = autoLogin;
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
        object.put("autoLogin", autoLogin);
        object.put("trustSslErrors", trustSslErrors);
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
                    object.optBoolean("autoLogin", true),
                    object.optBoolean("trustSslErrors", false));
        } catch (JSONException ignored) {
            return empty();
        }
    }

    public static ServerProfile empty() {
        return new ServerProfile("", "", "", true, false);
    }

    private static String normalizeBaseUrl(String raw) {
        if (raw == null) {
            return "";
        }
        String url = raw.trim();
        if (url.length() == 0) {
            return "";
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
