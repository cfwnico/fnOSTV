package com.fnostv.android4;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;

final class ProfileStore {
    private static final String PREFS = "fnos_tv_prefs";
    private static final String KEY_PROFILE = "active_profile";

    private final SharedPreferences preferences;

    ProfileStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    Profile load() {
        return Profile.fromJson(preferences.getString(KEY_PROFILE, ""));
    }

    void save(Profile profile) {
        try {
            preferences.edit().putString(KEY_PROFILE, profile.toJson().toString()).commit();
        } catch (JSONException ignored) {
        }
    }
}
