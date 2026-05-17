package com.fnostv.android4.tv;

public enum BackNavigationState {
    PLAYER,
    DETAIL,
    FULLSCREEN,
    BROWSER,
    WEB,
    SYSTEM;

    public static BackNavigationState choose(
            boolean playerVisible,
            boolean detailVisible,
            boolean fullscreenVisible,
            boolean browserVisible,
            boolean webCanGoBack) {
        if (playerVisible) {
            return PLAYER;
        }
        if (detailVisible) {
            return DETAIL;
        }
        if (fullscreenVisible) {
            return FULLSCREEN;
        }
        if (browserVisible) {
            return BROWSER;
        }
        if (webCanGoBack) {
            return WEB;
        }
        return SYSTEM;
    }
}
