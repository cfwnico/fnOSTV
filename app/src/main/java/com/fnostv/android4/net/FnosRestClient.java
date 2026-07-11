package com.fnostv.android4.net;

import com.fnostv.android4.config.ServerProfile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class FnosRestClient {
    private static final int TIMEOUT_SECONDS = 15;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final ServerProfile profile;
    private final OkHttpClient httpClient;
    private String token;

    public FnosRestClient(ServerProfile profile) {
        this.profile = profile;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    public FnosMediaCounts mediaCounts() throws FnosRpcException {
        try {
            return parseMediaCounts(get("/mediadb/sum"));
        } catch (JSONException ex) {
            throw new FnosRpcException("解析影视统计失败", ex);
        }
    }

    public FnosFileList mediaLibraries() throws FnosRpcException {
        try {
            return parseMediaLibraries(get("/mediadb/list"));
        } catch (JSONException ex) {
            throw new FnosRpcException("解析媒体库列表失败", ex);
        }
    }

    public FnosFileList mediaItems(String ancestorGuid, String category, int pageSize) throws FnosRpcException {
        try {
            return parseMediaItems(ancestorGuid, post("/item/list", mediaItemPayload(ancestorGuid, category, pageSize)));
        } catch (JSONException ex) {
            throw new FnosRpcException("解析影视条目失败", ex);
        }
    }

    public MediaDetailInfo mediaDetail(String guid) throws FnosRpcException {
        String path = mediaDetailPath(guid);
        if (path.length() == 0) {
            return MediaDetailInfo.empty();
        }
        return MediaDetailInfoParser.parse(get(path));
    }

    public FnosFileList favoriteItems() throws FnosRpcException {
        JSONObject body = new JSONObject();
        try {
            body.put("tags", new JSONObject());
            body.put("sort_type", "DESC");
            body.put("sort_column", "create_time");
            body.put("page", 1);
            body.put("page_size", 50);
            return parseMediaItems("favorite", post("/favorite/list", body.toString()));
        } catch (JSONException ex) {
            throw new FnosRpcException("构造收藏列表请求失败", ex);
        }
    }

    public FnosFileList recentItems() throws FnosRpcException {
        try {
            return parsePlayList(get("/play/list"));
        } catch (JSONException ex) {
            throw new FnosRpcException("解析继续观看列表失败", ex);
        }
    }

    public JSONObject serverInfo() throws FnosRpcException {
        return get("/server/info");
    }

    public JSONObject systemConfig() throws FnosRpcException {
        return get("/sys/config");
    }

    public JSONObject version() throws FnosRpcException {
        return get("/sys/version");
    }

    public JSONObject userInfo() throws FnosRpcException {
        return get("/user/info");
    }

    public String authorizationToken() {
        return token == null ? "" : token;
    }

    public JSONObject runningTasks() throws FnosRpcException {
        return get("/task/running");
    }

    public JSONArray managerUsers() throws FnosRpcException {
        return dataArray(get("/manager/user/list"));
    }

    public JSONArray taskSchedules() throws FnosRpcException {
        return dataArray(get("/task/schedule/list"));
    }

    public JSONArray gpuList() throws FnosRpcException {
        return dataArray(get("/server/gpu/list"));
    }

    public static String mediaItemPayload(String ancestorGuid, String category, int pageSize) throws JSONException {
        JSONObject body = new JSONObject();
        if (ancestorGuid != null && ancestorGuid.length() > 0) {
            body.put("ancestor_guid", ancestorGuid);
        }
        JSONObject tags = new JSONObject();
        tags.put("type", typeArray(category));
        body.put("tags", tags);
        body.put("sort_type", "DESC");
        body.put("sort_column", "create_time");
        if (shouldExcludeGrouped(category)) {
            body.put("exclude_grouped_video", 1);
        }
        body.put("page", 1);
        body.put("page_size", Math.max(1, pageSize));
        return body.toString();
    }

    public static String mediaDetailPath(String guid) {
        String value = guid == null ? "" : guid.trim();
        if (value.length() == 0) {
            return "";
        }
        return "/item/detail?guid=" + encodeQueryValue(value);
    }

    public static FnosMediaCounts parseMediaCounts(JSONObject response) throws JSONException {
        JSONObject data = requireDataObject(response);
        int libraryCount = 0;
        Iterator<String> keys = data.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!isSummaryKey(key)) {
                libraryCount++;
            }
        }
        return new FnosMediaCounts(
                data.optInt("favorite", 0),
                libraryCount,
                data.optInt("total", 0),
                data.optInt("movie", 0),
                data.optInt("tv", 0),
                data.optInt("video", 0));
    }

    public static FnosFileList parseMediaItems(String path, JSONObject response) throws JSONException {
        JSONObject data = requireDataObject(response);
        JSONArray list = data.optJSONArray("list");
        return new FnosFileList(path, parseEntries(list));
    }

    public static FnosFileList parseMediaLibraries(JSONObject response) throws JSONException {
        JSONArray data = response.optJSONArray("data");
        if (data == null) {
            JSONObject object = response.optJSONObject("data");
            data = object == null ? null : object.optJSONArray("list");
        }
        List<FnosFileEntry> entries = new ArrayList<FnosFileEntry>();
        if (data != null) {
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String title = firstNonEmpty(item.optString("title"), item.optString("name"), item.optString("guid"));
                String guid = item.optString("guid");
                if (guid.length() > 0) {
                    entries.add(new FnosFileEntry(title, guid, true, 0L, "MediaDB", ""));
                }
            }
        }
        return new FnosFileList("mediaLibraries", entries);
    }

    public static FnosFileList parsePlayList(JSONObject response) throws JSONException {
        Object data = response.opt("data");
        JSONArray list = null;
        if (data instanceof JSONArray) {
            list = (JSONArray) data;
        } else if (data instanceof JSONObject) {
            list = ((JSONObject) data).optJSONArray("list");
        }
        return new FnosFileList("recent", parseEntries(list));
    }

    private JSONObject get(String path) throws FnosRpcException {
        ensureToken();
        String urlPath = "/v/api/v1" + (path.startsWith("/") ? path : "/" + path);
        String authx = FnosCrypto.generateAuthx("GET", urlPath, "");
        Request request = new Request.Builder()
                .url(apiUrl(path))
                .header("Authorization", token)
                .header("Authx", authx)
                .header("Accept", "application/json")
                .build();
        return execute(request);
    }

    private JSONObject post(String path, String body) throws FnosRpcException {
        ensureToken();
        String urlPath = "/v/api/v1" + (path.startsWith("/") ? path : "/" + path);
        String authx = FnosCrypto.generateAuthx("POST", urlPath, body);
        Request request = new Request.Builder()
                .url(apiUrl(path))
                .header("Authorization", token)
                .header("Authx", authx)
                .header("Accept", "application/json")
                .post(RequestBody.create(JSON, body))
                .build();
        return execute(request);
    }

    private void ensureToken() throws FnosRpcException {
        if (token != null && token.length() > 0) {
            return;
        }
        try {
            JSONObject body = new JSONObject();
            body.put("username", profile.username);
            body.put("password", profile.password);
            body.put("app_name", "trimemedia-web");
            Request request = new Request.Builder()
                    .url(apiUrl("/login"))
                    .header("Accept", "application/json")
                    .post(RequestBody.create(JSON, body.toString()))
                    .build();
            JSONObject response = execute(request);
            token = extractToken(response);
            if (token.length() == 0) {
                throw new FnosRpcException("影视 REST 登录响应缺少 token");
            }
        } catch (JSONException ex) {
            throw new FnosRpcException("构造影视 REST 登录请求失败", ex);
        }
    }

    private JSONObject execute(Request request) throws FnosRpcException {
        Response response = null;
        try {
            response = httpClient.newCall(request).execute();
            String body = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw new FnosRpcException("影视 REST HTTP " + response.code());
            }
            JSONObject object = new JSONObject(body);
            int code = object.optInt("code", 0);
            if (code != 0) {
                String message = object.optString("msg");
                if (message.length() == 0) {
                    message = object.optString("message");
                }
                throw new FnosRpcException("影视 REST code=" + code + (message.length() == 0 ? "" : " " + message));
            }
            return object;
        } catch (IOException ex) {
            throw new FnosRpcException("影视 REST 请求失败", ex);
        } catch (JSONException ex) {
            throw new FnosRpcException("影视 REST 响应解析失败", ex);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private String apiUrl(String path) {
        String value = path == null ? "" : path;
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        return profile.baseUrl + "/v/api/v1" + value;
    }

    private static String extractToken(JSONObject response) {
        String value = response.optString("token");
        if (value.length() > 0) {
            return value;
        }
        JSONObject data = response.optJSONObject("data");
        if (data != null) {
            value = firstNonEmpty(
                    data.optString("token"),
                    data.optString("access_token"),
                    data.optString("authorization"),
                    data.optString("auth"));
            if (value.length() > 0) {
                return value;
            }
        }
        return response.optString("data");
    }

    private static JSONArray typeArray(String category) {
        JSONArray values = new JSONArray();
        String normalized = category == null ? "" : category.toLowerCase();
        if ("movies".equals(normalized) || "movie".equals(normalized)) {
            values.put("Movie");
        } else if ("tv".equals(normalized)) {
            values.put("TV");
        } else if ("other".equals(normalized)) {
            values.put("Directory");
            values.put("Video");
        } else {
            values.put("Movie");
            values.put("TV");
            values.put("Directory");
            values.put("Video");
        }
        return values;
    }

    private static boolean shouldExcludeGrouped(String category) {
        String normalized = category == null ? "" : category.toLowerCase();
        return normalized.length() == 0 || "all".equals(normalized) || "other".equals(normalized);
    }

    private static JSONObject requireDataObject(JSONObject response) throws JSONException {
        JSONObject data = response.optJSONObject("data");
        if (data == null) {
            throw new JSONException("missing data object");
        }
        return data;
    }

    private static JSONArray dataArray(JSONObject response) {
        JSONArray data = response == null ? null : response.optJSONArray("data");
        return data == null ? new JSONArray() : data;
    }

    private static List<FnosFileEntry> parseEntries(JSONArray list) {
        List<FnosFileEntry> entries = new ArrayList<FnosFileEntry>();
        if (list == null) {
            return entries;
        }
        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.optJSONObject(i);
            if (item != null) {
                entries.add(parseEntry(item));
            }
        }
        return entries;
    }

    private static FnosFileEntry parseEntry(JSONObject item) {
        String type = item.optString("type");
        String name = firstNonEmpty(
                item.optString("title"),
                item.optString("tv_title"),
                item.optString("file_name"),
                item.optString("name"),
                item.optString("guid"));
        String path = firstNonEmpty(
                item.optString("path"),
                item.optString("file_path"),
                item.optString("location"),
                item.optString("url"));
        boolean directory = isDirectoryType(type);
        if (path.length() == 0 && directory) {
            path = firstNonEmpty(item.optString("single_child_guid"), item.optString("guid"));
        }
        if (path.length() == 0) {
            path = firstNonEmpty(item.optString("single_child_guid"), item.optString("guid"));
        }
        String entryType = directory ? type : mediaType(type, name);
        return new FnosFileEntry(
                name,
                path,
                directory,
                item.optLong("size", 0L),
                entryType,
                mediaUrl(item),
                posterPath(item));
    }

    public static String posterImageUrl(String baseUrl, String posterPath, int width) {
        String path = posterPath == null ? "" : posterPath.trim();
        if (path.length() == 0) {
            return "";
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String base = baseUrl == null ? "" : baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        String url = base + "/v/api/v1/sys/img/" + path;
        if (width > 0) {
            url += "?w=" + width;
        }
        return url;
    }

    private static String posterPath(JSONObject item) {
        String value = item.optString("poster");
        if (value.length() > 0) {
            return value;
        }
        JSONArray posters = item.optJSONArray("poster_list");
        if (posters != null && posters.length() > 0) {
            return posters.optString(0);
        }
        return "";
    }

    private static String mediaType(String type, String name) {
        String lowerType = type == null ? "" : type.toLowerCase();
        if (lowerType.indexOf("video") >= 0 || lowerType.indexOf("movie") >= 0 || looksVideoName(name)) {
            return "video/rest/" + type;
        }
        return type;
    }

    private static boolean isDirectoryType(String type) {
        String value = type == null ? "" : type.toLowerCase();
        return "directory".equals(value)
                || "tv".equals(value)
                || "season".equals(value)
                || "mediadb".equals(value);
    }

    private static String mediaUrl(JSONObject object) {
        String[] keys = {"playUrl", "downloadUrl", "fileUrl", "mediaUrl", "realUrl", "link", "src", "url"};
        for (int i = 0; i < keys.length; i++) {
            String value = object.optString(keys[i]);
            if (value.startsWith("http://") || value.startsWith("https://")) {
                return value;
            }
        }
        return "";
    }

    private static boolean looksVideoName(String name) {
        String value = name == null ? "" : name.toLowerCase();
        return value.endsWith(".mp4")
                || value.endsWith(".m4v")
                || value.endsWith(".mov")
                || value.endsWith(".3gp")
                || value.endsWith(".3gpp")
                || value.endsWith(".webm")
                || value.endsWith(".mkv")
                || value.endsWith(".avi")
                || value.endsWith(".wmv")
                || value.endsWith(".asf")
                || value.endsWith(".ts")
                || value.endsWith(".m2ts")
                || value.endsWith(".flv")
                || value.endsWith(".mpeg")
                || value.endsWith(".mpg")
                || value.endsWith(".vob")
                || value.endsWith(".m3u8")
                || value.endsWith(".rm")
                || value.endsWith(".rmvb");
    }

    private static boolean isSummaryKey(String key) {
        return "favorite".equals(key)
                || "movie".equals(key)
                || "total".equals(key)
                || "tv".equals(key)
                || "video".equals(key);
    }

    private static String firstNonEmpty(String first, String second) {
        return first != null && first.length() > 0 ? first : (second == null ? "" : second);
    }

    private static String firstNonEmpty(String first, String second, String third) {
        return firstNonEmpty(firstNonEmpty(first, second), third);
    }

    private static String firstNonEmpty(String first, String second, String third, String fourth) {
        return firstNonEmpty(firstNonEmpty(first, second, third), fourth);
    }

    private static String firstNonEmpty(String first, String second, String third, String fourth, String fifth) {
        return firstNonEmpty(firstNonEmpty(first, second, third, fourth), fifth);
    }

    private static String encodeQueryValue(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException ex) {
            return value;
        }
    }
}
