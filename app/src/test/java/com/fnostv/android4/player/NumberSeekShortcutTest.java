package com.fnostv.android4.player;

import android.view.KeyEvent;

public final class NumberSeekShortcutTest {
    public static void main(String[] args) {
        mapsNumberKeysToPercent();
        ignoresUnsupportedKeys();
        calculatesSeekTargets();
        rejectsUnknownDuration();
        clampsTargetsToDuration();
        formatsHints();
    }

    private static void mapsNumberKeysToPercent() {
        assertEquals(0, NumberSeekShortcut.percentForKey(KeyEvent.KEYCODE_0));
        assertEquals(10, NumberSeekShortcut.percentForKey(KeyEvent.KEYCODE_1));
        assertEquals(50, NumberSeekShortcut.percentForKey(KeyEvent.KEYCODE_5));
        assertEquals(90, NumberSeekShortcut.percentForKey(KeyEvent.KEYCODE_9));
    }

    private static void ignoresUnsupportedKeys() {
        assertEquals(-1, NumberSeekShortcut.percentForKey(KeyEvent.KEYCODE_DPAD_CENTER));
        assertEquals(-1, NumberSeekShortcut.percentForKey(KeyEvent.KEYCODE_MENU));
    }

    private static void calculatesSeekTargets() {
        assertEquals(0, NumberSeekShortcut.targetMs(100000, 0));
        assertEquals(30000, NumberSeekShortcut.targetMs(100000, 30));
        assertEquals(90000, NumberSeekShortcut.targetMs(100000, 90));
    }

    private static void rejectsUnknownDuration() {
        assertEquals(-1, NumberSeekShortcut.targetMs(0, 50));
        assertEquals(-1, NumberSeekShortcut.targetMs(-1000, 50));
    }

    private static void clampsTargetsToDuration() {
        assertEquals(1000, NumberSeekShortcut.targetMs(1000, 150));
        assertEquals(0, NumberSeekShortcut.targetMs(1000, -10));
    }

    private static void formatsHints() {
        assertEquals("回到开头", NumberSeekShortcut.hintForPercent(0));
        assertEquals("跳转 30%", NumberSeekShortcut.hintForPercent(30));
        assertEquals("跳转 90%", NumberSeekShortcut.hintForPercent(90));
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
