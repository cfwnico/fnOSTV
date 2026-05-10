package com.fnostv.android4.tv;

import android.view.KeyEvent;

public final class RemoteKeyHandler {
    private final RemoteActions actions;

    public RemoteKeyHandler(RemoteActions actions) {
        this.actions = actions;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            return actions.openSettings();
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return actions.goBack();
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_SPACE) {
            return actions.togglePlayback();
        }
        return false;
    }
}
