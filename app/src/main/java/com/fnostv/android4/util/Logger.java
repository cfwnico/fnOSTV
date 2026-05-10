package com.fnostv.android4.util;

import android.util.Log;

public final class Logger {
    public static final String TAG = "FnOSTV";

    private Logger() {
    }

    public static void d(String message) {
        Log.d(TAG, message);
    }

    public static void w(String message) {
        Log.w(TAG, message);
    }

    public static void w(String message, Throwable throwable) {
        Log.w(TAG, message, throwable);
    }

    public static void e(String message, Throwable throwable) {
        Log.e(TAG, message, throwable);
    }
}
