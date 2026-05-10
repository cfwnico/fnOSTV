package com.fnostv.android4.web;

import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;

public interface WebViewEvents {
    void onPageLoadStarted();

    void onPageLoadFinished(WebView view, String url);

    void onProgressChanged(int progress);

    void onMainFrameError(String description, String failingUrl);

    void onSslError(SslErrorHandler handler, SslError error);
}
