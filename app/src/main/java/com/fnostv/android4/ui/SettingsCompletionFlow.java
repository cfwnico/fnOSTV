package com.fnostv.android4.ui;

public final class SettingsCompletionFlow {
    public static final int ACTION_KEEP_ACCOUNT_EDITOR = 0;
    public static final int ACTION_SHOW_NATIVE_SETTINGS = 1;
    public static final int ACTION_FINISH = 2;

    private SettingsCompletionFlow() {
    }

    public static int afterAccountSave(boolean firstConfiguration, boolean accountEditorOpen, boolean profileReady) {
        if (!accountEditorOpen || !profileReady) {
            return ACTION_KEEP_ACCOUNT_EDITOR;
        }
        return firstConfiguration ? ACTION_FINISH : ACTION_SHOW_NATIVE_SETTINGS;
    }
}
