package com.fnostv.android4.config;

import android.content.Context;
import android.content.SharedPreferences;

import com.fnostv.android4.util.Constants;
import com.fnostv.android4.util.Logger;

import org.json.JSONException;

public final class ProfileStore {
    private final SharedPreferences preferences;

    public ProfileStore(Context context) {
        preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public ServerProfile load() {
        return ServerProfile.fromJson(preferences.getString(Constants.PROFILE_KEY, ""));
    }

    public void save(ServerProfile profile) {
        try {
            boolean saved = preferences.edit().putString(Constants.PROFILE_KEY, profile.toJson().toString()).commit();
            if (!saved) {
                Logger.w("Server profile commit returned false");
            }
        } catch (JSONException exception) {
            Logger.w("Failed to serialize server profile", exception);
        }
    }
}
