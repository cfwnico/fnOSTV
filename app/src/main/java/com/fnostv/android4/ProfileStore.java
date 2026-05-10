package com.fnostv.android4;

import android.content.Context;
import android.content.SharedPreferences;

import com.fnostv.android4.util.Constants;
import com.fnostv.android4.util.Logger;

import org.json.JSONException;

final class ProfileStore {
    private final SharedPreferences preferences;

    ProfileStore(Context context) {
        preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    Profile load() {
        return Profile.fromJson(preferences.getString(Constants.PROFILE_KEY, ""));
    }

    void save(Profile profile) {
        try {
            preferences.edit().putString(Constants.PROFILE_KEY, profile.toJson().toString()).commit();
        } catch (JSONException exception) {
            Logger.w("Failed to serialize server profile", exception);
        }
    }
}
