package com.fnostv.android4.tv;

public final class BackNavigationStateTest {
    public static void main(String[] args) {
        choosesPlayerBeforeOtherSurfaces();
        choosesDetailBeforeBrowser();
        choosesFullscreenBeforeBrowser();
        choosesBrowserBeforeWebHistory();
        fallsBackToSystemWhenNothingHandlesBack();
    }

    private static void choosesPlayerBeforeOtherSurfaces() {
        assertEquals(BackNavigationState.PLAYER, BackNavigationState.choose(true, true, true, true, true));
    }

    private static void choosesDetailBeforeBrowser() {
        assertEquals(BackNavigationState.DETAIL, BackNavigationState.choose(false, true, true, true, true));
    }

    private static void choosesFullscreenBeforeBrowser() {
        assertEquals(BackNavigationState.FULLSCREEN, BackNavigationState.choose(false, false, true, true, true));
    }

    private static void choosesBrowserBeforeWebHistory() {
        assertEquals(BackNavigationState.BROWSER, BackNavigationState.choose(false, false, false, true, true));
    }

    private static void fallsBackToSystemWhenNothingHandlesBack() {
        assertEquals(BackNavigationState.SYSTEM, BackNavigationState.choose(false, false, false, false, false));
    }

    private static void assertEquals(BackNavigationState expected, BackNavigationState actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
