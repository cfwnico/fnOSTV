package com.fnostv.android4.net;

import android.content.Context;
import android.content.SharedPreferences;

import com.fnostv.android4.util.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;

public final class FnosSessionStore {
    private static final String SESSION_KEY = "native_rpc_session";
    private static final String DEVICE_ID_KEY = "native_rpc_device_id";
    private static final String DEVICE_ID_CHARS = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SharedPreferences preferences;

    public FnosSessionStore(Context context) {
        preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public FnosSession load() {
        String raw = preferences.getString(SESSION_KEY, "");
        if (raw == null || raw.length() == 0) {
            return new FnosSession("", "", "", "", "", "");
        }
        try {
            JSONObject object = new JSONObject(raw);
            return new FnosSession(
                    object.optString("token"),
                    object.optString("longToken"),
                    object.optString("secretHex"),
                    object.optString("user"),
                    object.optString("machineId"),
                    object.optString("uid"));
        } catch (JSONException ignored) {
            return new FnosSession("", "", "", "", "", "");
        }
    }

    public void save(FnosSession session) {
        JSONObject object = new JSONObject();
        try {
            object.put("token", session.token);
            object.put("longToken", session.longToken);
            object.put("secretHex", session.secretHex);
            object.put("user", session.user);
            object.put("machineId", session.machineId);
            object.put("uid", session.uid);
            preferences.edit().putString(SESSION_KEY, object.toString()).apply();
        } catch (JSONException ignored) {
            clear();
        }
    }

    public void clear() {
        preferences.edit().remove(SESSION_KEY).apply();
    }

    public String getOrCreateDeviceId() {
        String value = preferences.getString(DEVICE_ID_KEY, "");
        if (isWebDeviceId(value)) {
            return value;
        }
        value = createWebDeviceId();
        preferences.edit().putString(DEVICE_ID_KEY, value).apply();
        return value;
    }

    private boolean isWebDeviceId(String value) {
        if (value == null) {
            return false;
        }
        String[] parts = value.split("-");
        return parts.length == 3
                && parts[0].length() > 0
                && parts[1].length() > 0
                && parts[2].length() > 0;
    }

    private String createWebDeviceId() {
        return Long.toString(System.currentTimeMillis(), 36)
                + "-" + randomBase36(13)
                + "-" + randomBase36(13);
    }

    private String randomBase36(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(DEVICE_ID_CHARS.charAt(RANDOM.nextInt(DEVICE_ID_CHARS.length())));
        }
        return builder.toString();
    }
}
