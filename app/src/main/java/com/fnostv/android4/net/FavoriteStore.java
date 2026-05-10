package com.fnostv.android4.net;

import android.content.Context;
import android.content.SharedPreferences;

import com.fnostv.android4.util.Constants;
import com.fnostv.android4.util.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FavoriteStore {
    private static final String KEY_FAVORITES = "favorite_media";
    private static final int MAX_ITEMS = 200;

    private final SharedPreferences preferences;

    public FavoriteStore(Context context) {
        preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<FnosFileEntry> list() {
        List<FnosFileEntry> entries = new ArrayList<FnosFileEntry>();
        String raw = preferences.getString(KEY_FAVORITES, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object != null) {
                    entries.add(fromJson(object));
                }
            }
        } catch (JSONException ex) {
            Logger.w("Favorite parse failed: " + ex.getMessage());
        }
        return entries;
    }

    public Set<String> paths() {
        Set<String> paths = new HashSet<String>();
        List<FnosFileEntry> entries = list();
        for (int i = 0; i < entries.size(); i++) {
            paths.add(entries.get(i).path);
        }
        return paths;
    }

    public boolean isFavorite(FnosFileEntry entry) {
        return entry != null && paths().contains(entry.path);
    }

    public boolean toggle(FnosFileEntry entry) {
        if (entry == null || entry.directory || entry.path.length() == 0) {
            return false;
        }
        List<FnosFileEntry> entries = list();
        JSONArray array = new JSONArray();
        boolean removed = false;
        int count = 0;
        for (int i = 0; i < entries.size(); i++) {
            FnosFileEntry value = entries.get(i);
            if (value.path.equals(entry.path)) {
                removed = true;
                continue;
            }
            if (count++ < MAX_ITEMS) {
                array.put(toJson(value));
            }
        }
        if (!removed) {
            JSONArray updated = new JSONArray();
            updated.put(toJson(entry));
            for (int i = 0; i < array.length() && i < MAX_ITEMS - 1; i++) {
                updated.put(array.opt(i));
            }
            array = updated;
        }
        preferences.edit().putString(KEY_FAVORITES, array.toString()).apply();
        return !removed;
    }

    private FnosFileEntry fromJson(JSONObject object) {
        return new FnosFileEntry(
                object.optString("name"),
                object.optString("path"),
                false,
                object.optLong("size", 0L),
                object.optString("type"),
                object.optString("mediaUrl"));
    }

    private JSONObject toJson(FnosFileEntry entry) {
        JSONObject object = new JSONObject();
        try {
            object.put("name", entry.name);
            object.put("path", entry.path);
            object.put("size", entry.size);
            object.put("type", entry.type);
            object.put("mediaUrl", entry.mediaUrl);
            object.put("favoritedAt", System.currentTimeMillis());
        } catch (JSONException ignored) {
        }
        return object;
    }
}
