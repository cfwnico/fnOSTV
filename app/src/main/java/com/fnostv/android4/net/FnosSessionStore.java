package com.fnostv.android4.net;

import android.content.Context;
import android.content.SharedPreferences;

import com.fnostv.android4.util.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public final class FnosSessionStore {
    private static final String SESSION_KEY = "native_rpc_session";
    private static final String DEVICE_ID_KEY = "native_rpc_device_id";

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
        if (value != null && value.length() > 0) {
            return value;
        }
        value = UUID.randomUUID().toString();
        preferences.edit().putString(DEVICE_ID_KEY, value).apply();
        return value;
    }
}
