package com.fnostv.android4.net;

import android.content.Context;
import android.content.SharedPreferences;

import com.fnostv.android4.util.Constants;
import com.fnostv.android4.util.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class RecentPlaybackStore {
    private static final String KEY_RECENT_PLAYBACK = "recent_playback";
    private static final int MAX_ITEMS = 50;

    private final SharedPreferences preferences;

    public RecentPlaybackStore(Context context) {
        preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<FnosFileEntry> list() {
        List<FnosFileEntry> entries = new ArrayList<FnosFileEntry>();
        String raw = preferences.getString(KEY_RECENT_PLAYBACK, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                entries.add(new FnosFileEntry(
                        object.optString("name"),
                        object.optString("path"),
                        false,
                        object.optLong("size", 0L),
                        object.optString("type"),
                        object.optString("mediaUrl")));
            }
        } catch (JSONException ex) {
            Logger.w("Recent playback parse failed: " + ex.getMessage());
        }
        return entries;
    }

    public void remember(FnosFileEntry entry) {
        if (entry == null || !entry.isVideo()) {
            return;
        }
        List<FnosFileEntry> entries = list();
        List<FnosFileEntry> updated = new ArrayList<FnosFileEntry>();
        updated.add(entry);
        for (int i = 0; i < entries.size() && updated.size() < MAX_ITEMS; i++) {
            FnosFileEntry existing = entries.get(i);
            if (!sameFile(existing, entry)) {
                updated.add(existing);
            }
        }
        JSONArray array = new JSONArray();
        for (int i = 0; i < updated.size(); i++) {
            FnosFileEntry value = updated.get(i);
            JSONObject object = new JSONObject();
            try {
                object.put("name", value.name);
                object.put("path", value.path);
                object.put("size", value.size);
                object.put("type", value.type);
                object.put("mediaUrl", value.mediaUrl);
                object.put("playedAt", System.currentTimeMillis());
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        preferences.edit().putString(KEY_RECENT_PLAYBACK, array.toString()).apply();
    }

    private boolean sameFile(FnosFileEntry left, FnosFileEntry right) {
        return left != null
                && right != null
                && left.path.equals(right.path)
                && left.name.equals(right.name);
    }
}
