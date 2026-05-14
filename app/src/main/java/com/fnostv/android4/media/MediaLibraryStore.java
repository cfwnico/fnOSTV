package com.fnostv.android4.media;

import android.content.Context;
import android.content.SharedPreferences;

import com.fnostv.android4.util.Constants;
import com.fnostv.android4.util.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class MediaLibraryStore {
    private static final String KEY_LIBRARIES = "native_media_libraries";
    private static final int MAX_LIBRARIES = 200;

    private final SharedPreferences preferences;

    public MediaLibraryStore(Context context) {
        preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<MediaLibrary> list() {
        List<MediaLibrary> libraries = new ArrayList<MediaLibrary>();
        String raw = preferences.getString(KEY_LIBRARIES, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object != null) {
                    libraries.add(fromJson(object));
                }
            }
        } catch (JSONException ex) {
            Logger.w("Media library parse failed: " + ex.getMessage());
        }
        Collections.sort(libraries, new Comparator<MediaLibrary>() {
            @Override
            public int compare(MediaLibrary left, MediaLibrary right) {
                return left.sortOrder - right.sortOrder;
            }
        });
        return libraries;
    }

    public List<MediaLibrary> listOrSeedDefault() {
        List<MediaLibrary> libraries = list();
        if (libraries.size() > 0) {
            return libraries;
        }
        List<String> paths = new ArrayList<String>();
        MediaLibrary library = new MediaLibrary(newId(), "影视大全", MediaLibraryCategory.ALL, paths, true, 0, System.currentTimeMillis());
        libraries.add(library);
        saveAll(libraries);
        return libraries;
    }

    public void upsert(MediaLibrary library) {
        if (library == null) {
            return;
        }
        List<MediaLibrary> libraries = list();
        boolean replaced = false;
        for (int i = 0; i < libraries.size(); i++) {
            if (libraries.get(i).id.equals(library.id)) {
                libraries.set(i, library.withUpdatedAt(System.currentTimeMillis()));
                replaced = true;
                break;
            }
        }
        if (!replaced && libraries.size() < MAX_LIBRARIES) {
            String id = library.id.length() == 0 ? newId() : library.id;
            libraries.add(new MediaLibrary(id, library.name, library.category, library.paths, library.enabled, libraries.size(), System.currentTimeMillis()));
        }
        saveAll(libraries);
    }

    public void delete(String id) {
        if (id == null || id.length() == 0) {
            return;
        }
        List<MediaLibrary> libraries = list();
        List<MediaLibrary> updated = new ArrayList<MediaLibrary>();
        for (int i = 0; i < libraries.size(); i++) {
            MediaLibrary library = libraries.get(i);
            if (!id.equals(library.id)) {
                updated.add(new MediaLibrary(library.id, library.name, library.category, library.paths, library.enabled, updated.size(), library.updatedAt));
            }
        }
        saveAll(updated);
    }

    public void saveAll(List<MediaLibrary> libraries) {
        JSONArray array = new JSONArray();
        if (libraries != null) {
            for (int i = 0; i < libraries.size() && i < MAX_LIBRARIES; i++) {
                array.put(toJson(libraries.get(i), i));
            }
        }
        preferences.edit().putString(KEY_LIBRARIES, array.toString()).apply();
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    private MediaLibrary fromJson(JSONObject object) {
        JSONArray rawPaths = object.optJSONArray("paths");
        List<String> paths = new ArrayList<String>();
        if (rawPaths != null) {
            for (int i = 0; i < rawPaths.length(); i++) {
                paths.add(rawPaths.optString(i));
            }
        }
        return new MediaLibrary(
                object.optString("id"),
                object.optString("name"),
                object.optString("category", MediaLibraryCategory.ALL),
                paths,
                object.optBoolean("enabled", true),
                object.optInt("sortOrder", 0),
                object.optLong("updatedAt", 0L));
    }

    private JSONObject toJson(MediaLibrary library, int order) {
        JSONObject object = new JSONObject();
        try {
            object.put("id", library.id);
            object.put("name", library.name);
            object.put("category", library.category);
            object.put("enabled", library.enabled);
            object.put("sortOrder", order);
            object.put("updatedAt", library.updatedAt);
            JSONArray paths = new JSONArray();
            for (int i = 0; i < library.paths.size(); i++) {
                paths.put(library.paths.get(i));
            }
            object.put("paths", paths);
        } catch (JSONException ignored) {
        }
        return object;
    }
}
