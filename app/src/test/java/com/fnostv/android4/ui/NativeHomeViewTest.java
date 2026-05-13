package com.fnostv.android4.ui;

public final class NativeHomeViewTest {
    public static void main(String[] args) {
        userMenuActionsMatchSettingsRoutes();
    }

    private static void userMenuActionsMatchSettingsRoutes() {
        assertEquals("settings-password", NativeHomeView.ACTION_USER_PASSWORD);
        assertEquals("settings-preference", NativeHomeView.ACTION_USER_PREFERENCE);
        assertEquals("settings-appearance", NativeHomeView.ACTION_USER_APPEARANCE);
        assertEquals("help", NativeHomeView.ACTION_HELP);
        assertEquals("about", NativeHomeView.ACTION_ABOUT);
        assertEquals("logout", NativeHomeView.ACTION_LOGOUT);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
