package com.fnostv.android4.net;

public final class FnosPlaybackSource {
    public final String label;
    public final String url;

    public FnosPlaybackSource(String label, String url) {
        this.label = label == null || label.length() == 0 ? "原画" : label;
        this.url = url == null ? "" : url;
    }

    public boolean isValid() {
        return url.length() > 0;
    }
}
