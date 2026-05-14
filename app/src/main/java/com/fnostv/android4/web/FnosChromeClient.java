package com.fnostv.android4.web;

import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public final class FnosChromeClient extends WebChromeClient {
    private final WebViewEvents events;
    private final FullscreenVideoController fullscreenVideoController;

    public FnosChromeClient(WebViewEvents events, FullscreenVideoController fullscreenVideoController) {
        this.events = events;
        this.fullscreenVideoController = fullscreenVideoController;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        events.onProgressChanged(newProgress);
    }

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        fullscreenVideoController.show(view, callback);
    }

    @Override
    public void onHideCustomView() {
        fullscreenVideoController.hide();
    }
}
