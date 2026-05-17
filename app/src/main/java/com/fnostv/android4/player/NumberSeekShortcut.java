package com.fnostv.android4.player;

import android.view.KeyEvent;

public final class NumberSeekShortcut {
    private NumberSeekShortcut() {
    }

    public static int percentForKey(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_0) {
            return 0;
        }
        if (keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_9) {
            return (keyCode - KeyEvent.KEYCODE_0) * 10;
        }
        return -1;
    }

    public static int targetMs(int durationMs, int percent) {
        if (durationMs <= 0) {
            return -1;
        }
        int clampedPercent = Math.max(0, Math.min(100, percent));
        long target = ((long) durationMs * clampedPercent) / 100L;
        return (int) Math.max(0L, Math.min((long) durationMs, target));
    }

    public static String hintForPercent(int percent) {
        return percent == 0 ? "回到开头" : "跳转 " + percent + "%";
    }
}
