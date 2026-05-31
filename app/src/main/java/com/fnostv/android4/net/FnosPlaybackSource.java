package com.fnostv.android4.net;

public final class FnosPlaybackSource {
    public final String label;
    public final String url;
    public final String authorizationToken;

    public FnosPlaybackSource(String label, String url) {
        this(label, url, "");
    }

    public FnosPlaybackSource(String label, String url, String authorizationToken) {
        this.label = label == null || label.length() == 0 ? "原画" : label;
        this.url = url == null ? "" : url;
        this.authorizationToken = authorizationToken == null ? "" : authorizationToken;
    }

    public boolean isValid() {
        return url.length() > 0;
    }
}
