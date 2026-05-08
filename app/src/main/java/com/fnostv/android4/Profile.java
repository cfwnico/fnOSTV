package com.fnostv.android4;

import org.json.JSONException;
import org.json.JSONObject;

final class Profile {
    final String baseUrl;
    final String username;
    final String password;
    final boolean autoLogin;
    final boolean trustSslErrors;

    Profile(String baseUrl, String username, String password, boolean autoLogin, boolean trustSslErrors) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password;
        this.autoLogin = autoLogin;
        this.trustSslErrors = trustSslErrors;
    }

    boolean isReady() {
        return baseUrl.length() > 0;
    }

    JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("baseUrl", baseUrl);
        object.put("username", username);
        object.put("password", password);
        object.put("autoLogin", autoLogin);
        object.put("trustSslErrors", trustSslErrors);
        return object;
    }

    static Profile fromJson(String value) {
        if (value == null || value.length() == 0) {
            return empty();
        }
        try {
            JSONObject object = new JSONObject(value);
            return new Profile(
                    object.optString("baseUrl"),
                    object.optString("username"),
                    object.optString("password"),
                    object.optBoolean("autoLogin", true),
                    object.optBoolean("trustSslErrors", false));
        } catch (JSONException ignored) {
            return empty();
        }
    }

    static Profile empty() {
        return new Profile("", "", "", true, false);
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
