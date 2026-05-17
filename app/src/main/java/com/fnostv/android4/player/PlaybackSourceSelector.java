package com.fnostv.android4.player;

import com.fnostv.android4.net.FnosPlaybackSource;

import java.util.ArrayList;
import java.util.List;

public final class PlaybackSourceSelector {
    private PlaybackSourceSelector() {
    }

    public static List<FnosPlaybackSource> normalize(List<FnosPlaybackSource> sources) {
        List<FnosPlaybackSource> result = new ArrayList<FnosPlaybackSource>();
        if (sources == null) {
            return result;
        }
        for (int i = 0; i < sources.size(); i++) {
            FnosPlaybackSource source = sources.get(i);
            if (source != null && source.isValid() && !containsUrl(result, source.url)) {
                result.add(source);
            }
        }
        return result;
    }

    public static int clampIndex(List<FnosPlaybackSource> sources, int index) {
        int size = sources == null ? 0 : sources.size();
        if (size <= 0 || index < 0) {
            return 0;
        }
        return index >= size ? size - 1 : index;
    }

    public static FnosPlaybackSource selectedSource(List<FnosPlaybackSource> sources, int index) {
        if (sources == null || sources.size() == 0) {
            return null;
        }
        return sources.get(clampIndex(sources, index));
    }

    public static String displayLabel(List<FnosPlaybackSource> sources, int index) {
        if (sources == null || sources.size() == 0) {
            return "\u65e0\u53ef\u7528\u64ad\u653e\u6e90";
        }
        int selected = clampIndex(sources, index);
        FnosPlaybackSource source = sources.get(selected);
        String hint = sourceHint(source.url);
        return (selected + 1) + "/" + sources.size() + " " + source.label
                + (hint.length() == 0 ? "" : " \u00b7 " + hint);
    }

    private static String sourceHint(String url) {
        String cleanUrl = url == null ? "" : url;
        int queryIndex = cleanUrl.indexOf('?');
        if (queryIndex >= 0) {
            cleanUrl = cleanUrl.substring(0, queryIndex);
        }
        String lower = cleanUrl.toLowerCase();
        String format = "";
        int extensionIndex = lower.lastIndexOf('.');
        if (extensionIndex >= 0 && extensionIndex < lower.length() - 1) {
            format = lower.substring(extensionIndex + 1).toUpperCase();
        }
        if ("M3U8".equals(format)) {
            format = "HLS";
        }
        boolean local = !(lower.startsWith("http://") || lower.startsWith("https://"));
        if (format.length() == 0) {
            return local ? "local" : "";
        }
        return local ? format + " \u00b7 local" : format;
    }

    private static boolean containsUrl(List<FnosPlaybackSource> sources, String url) {
        for (int i = 0; i < sources.size(); i++) {
            if (sources.get(i).url.equals(url)) {
                return true;
            }
        }
        return false;
    }
}
