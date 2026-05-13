package com.fnostv.android4.net;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class FnosSettingsSummary {
    public final String username;
    public final String userInitial;
    public final boolean admin;
    public final String serverName;
    public final String languageLabel;
    public final String version;
    public final String serviceVersion;
    public final boolean gpuAccelerationEnabled;
    public final boolean cpuFallbackEnabled;
    public final boolean directLinkEnabled;
    public final boolean fileMonitorEnabled;
    public final String preferredGpuLabel;
    public final String directLinkAllowedLabel;
    public final List<UserRow> userRows;
    public final List<TaskRow> taskRows;

    private FnosSettingsSummary(
            String username,
            boolean admin,
            String serverName,
            String languageLabel,
            String version,
            String serviceVersion,
            boolean gpuAccelerationEnabled,
            boolean cpuFallbackEnabled,
            boolean directLinkEnabled,
            boolean fileMonitorEnabled,
            String preferredGpuLabel,
            String directLinkAllowedLabel,
            List<UserRow> userRows,
            List<TaskRow> taskRows) {
        this.username = emptyToDefault(username, "--");
        this.userInitial = initial(this.username);
        this.admin = admin;
        this.serverName = emptyToDefault(serverName, "--");
        this.languageLabel = emptyToDefault(languageLabel, "中文简体");
        this.version = emptyToDefault(version, "--");
        this.serviceVersion = emptyToDefault(serviceVersion, "--");
        this.gpuAccelerationEnabled = gpuAccelerationEnabled;
        this.cpuFallbackEnabled = cpuFallbackEnabled;
        this.directLinkEnabled = directLinkEnabled;
        this.fileMonitorEnabled = fileMonitorEnabled;
        this.preferredGpuLabel = emptyToDefault(preferredGpuLabel, "--");
        this.directLinkAllowedLabel = emptyToDefault(directLinkAllowedLabel, "仅管理员");
        this.userRows = userRows == null ? new ArrayList<UserRow>() : userRows;
        this.taskRows = taskRows == null ? new ArrayList<TaskRow>() : taskRows;
    }

    public static FnosSettingsSummary empty() {
        return new FnosSettingsSummary(
                "--",
                false,
                "--",
                "中文简体",
                "--",
                "--",
                false,
                false,
                false,
                false,
                "--",
                "仅管理员",
                new ArrayList<UserRow>(),
                new ArrayList<TaskRow>());
    }

    public static FnosSettingsSummary fromResponses(
            JSONObject userInfoResponse,
            JSONObject systemConfigResponse,
            JSONObject versionResponse,
            JSONObject serverInfoResponse,
            JSONArray managerUsers,
            JSONArray taskSchedules,
            JSONArray gpuList) {
        JSONObject user = dataObject(userInfoResponse);
        JSONObject config = dataObject(systemConfigResponse);
        JSONObject version = dataObject(versionResponse);
        JSONObject server = dataObject(serverInfoResponse);

        return new FnosSettingsSummary(
                firstNonEmpty(user.optString("username"), firstSourceName(user.optJSONArray("sources"))),
                user.optInt("is_admin", 0) == 1,
                firstNonEmpty(server.optString("name"), config.optString("server_name")),
                languageLabel(firstNonEmpty(server.optString("lan"), user.optString("lan"))),
                version.optString("version"),
                version.optString("mediasrvVersion"),
                server.optInt("gpu_acc", 0) == 1,
                server.optInt("cpu_allow_decoding", 0) == 1,
                server.optInt("direct_link_enable", 0) == 1,
                server.optInt("file_monitor", 0) == 1,
                gpuLabel(server.optInt("gpu_prefer", 0), gpuList),
                directLinkAllowedLabel(server.optInt("direct_link_allowed_level", 0)),
                parseUsers(managerUsers),
                parseTasks(taskSchedules));
    }

    public static List<UserRow> parseUsers(JSONArray users) {
        List<UserRow> rows = new ArrayList<UserRow>();
        if (users == null) {
            return rows;
        }
        for (int i = 0; i < users.length(); i++) {
            JSONObject item = users.optJSONObject(i);
            if (item == null) {
                continue;
            }
            rows.add(new UserRow(
                    item.optString("username"),
                    item.optInt("is_admin", 0) == 1 ? "管理员" : "普通用户",
                    item.optInt("media_permission", 0) == 2 ? "全部" : "部分",
                    firstSourceName(item.optJSONArray("sources")),
                    formatDate(item.optLong("last_login_time", 0L))));
        }
        return rows;
    }

    public static List<TaskRow> parseTasks(JSONArray tasks) {
        List<TaskRow> rows = new ArrayList<TaskRow>();
        if (tasks == null) {
            return rows;
        }
        for (int i = 0; i < tasks.length(); i++) {
            JSONObject item = tasks.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String type = item.optString("type");
            rows.add(new TaskRow(type, taskLabel(type), item.optInt("status", 0) == 1 ? "已启用" : "已关闭"));
        }
        return rows;
    }

    private static JSONObject dataObject(JSONObject response) {
        if (response == null) {
            return new JSONObject();
        }
        JSONObject data = response.optJSONObject("data");
        return data == null ? new JSONObject() : data;
    }

    private static String firstSourceName(JSONArray sources) {
        if (sources == null || sources.length() == 0) {
            return "";
        }
        JSONObject source = sources.optJSONObject(0);
        return source == null ? "" : firstNonEmpty(source.optString("source_name"), source.optString("source"));
    }

    private static String gpuLabel(int sequence, JSONArray gpuList) {
        if (gpuList == null) {
            return "--";
        }
        for (int i = 0; i < gpuList.length(); i++) {
            JSONObject gpu = gpuList.optJSONObject(i);
            if (gpu != null && gpu.optInt("sequence", -1) == sequence) {
                return "GPU" + sequence + " " + gpu.optString("model");
            }
        }
        return sequence > 0 ? "GPU" + sequence : "--";
    }

    private static String directLinkAllowedLabel(int level) {
        return level == 1 ? "全部用户" : "仅管理员";
    }

    private static String languageLabel(String value) {
        if ("zh-CN".equalsIgnoreCase(value) || value.length() == 0) {
            return "中文简体";
        }
        return value;
    }

    private static String taskLabel(String type) {
        if ("TaskItemScrap".equals(type)) {
            return "扫描媒体库文件";
        }
        if ("TaskSubtitleExtra".equals(type)) {
            return "提取内置字幕";
        }
        return type == null || type.length() == 0 ? "定时任务" : type;
    }

    private static String formatDate(long seconds) {
        if (seconds <= 0L) {
            return "--";
        }
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(seconds * 1000L));
    }

    private static String initial(String value) {
        String text = value == null ? "" : value.trim();
        return text.length() == 0 ? "?" : text.substring(0, 1).toUpperCase(Locale.US);
    }

    private static String emptyToDefault(String value, String fallback) {
        String text = value == null ? "" : value.trim();
        return text.length() == 0 ? fallback : text;
    }

    private static String firstNonEmpty(String first, String second) {
        return first != null && first.length() > 0 ? first : (second == null ? "" : second);
    }

    public static final class UserRow {
        public final String username;
        public final String roleLabel;
        public final String permissionLabel;
        public final String sourceName;
        public final String lastLoginDate;

        private UserRow(String username, String roleLabel, String permissionLabel, String sourceName, String lastLoginDate) {
            this.username = emptyToDefault(username, "--");
            this.roleLabel = emptyToDefault(roleLabel, "--");
            this.permissionLabel = emptyToDefault(permissionLabel, "--");
            this.sourceName = emptyToDefault(sourceName, "--");
            this.lastLoginDate = emptyToDefault(lastLoginDate, "--");
        }
    }

    public static final class TaskRow {
        public final String type;
        public final String label;
        public final String statusLabel;

        private TaskRow(String type, String label, String statusLabel) {
            this.type = emptyToDefault(type, "--");
            this.label = emptyToDefault(label, "--");
            this.statusLabel = emptyToDefault(statusLabel, "--");
        }
    }
}
