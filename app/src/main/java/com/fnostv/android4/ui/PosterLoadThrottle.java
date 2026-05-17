package com.fnostv.android4.ui;

import java.util.HashSet;
import java.util.Set;

final class PosterLoadThrottle {
    private final int maxActive;
    private final Set<String> activeUrls = new HashSet<String>();

    PosterLoadThrottle(int maxActive) {
        this.maxActive = Math.max(1, maxActive);
    }

    synchronized boolean tryStart(String url) {
        if (url == null || url.length() == 0) {
            return false;
        }
        if (activeUrls.contains(url) || activeUrls.size() >= maxActive) {
            return false;
        }
        activeUrls.add(url);
        return true;
    }

    synchronized void finish(String url) {
        activeUrls.remove(url);
    }
}
