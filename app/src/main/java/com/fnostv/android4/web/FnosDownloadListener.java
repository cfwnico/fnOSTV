package com.fnostv.android4.web;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.DownloadListener;
import android.widget.Toast;

public final class FnosDownloadListener implements DownloadListener {
    private final Context context;

    public FnosDownloadListener(Context context) {
        this.context = context;
    }

    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(context, "没有可用的下载应用", Toast.LENGTH_SHORT).show();
        }
    }
}
