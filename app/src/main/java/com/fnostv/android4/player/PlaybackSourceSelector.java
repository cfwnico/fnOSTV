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
            return "无可用播放源";
        }
        int selected = clampIndex(sources, index);
        FnosPlaybackSource source = sources.get(selected);
        return (selected + 1) + "/" + sources.size() + " " + source.label;
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
