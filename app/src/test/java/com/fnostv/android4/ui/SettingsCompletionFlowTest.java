package com.fnostv.android4.ui;

public final class SettingsCompletionFlowTest {
    public static void main(String[] args) {
        firstConfigurationFinishesAfterReadyProfile();
        existingConnectionErrorFinishesAfterSave();
        invalidProfileKeepsAccountEditorOpen();
    }

    private static void firstConfigurationFinishesAfterReadyProfile() {
        assertEquals(
                SettingsCompletionFlow.ACTION_FINISH,
                SettingsCompletionFlow.afterAccountSave(true, true, true));
    }

    private static void existingConnectionErrorFinishesAfterSave() {
        assertEquals(
                SettingsCompletionFlow.ACTION_FINISH,
                SettingsCompletionFlow.afterAccountSave(false, true, true));
    }

    private static void invalidProfileKeepsAccountEditorOpen() {
        assertEquals(
                SettingsCompletionFlow.ACTION_KEEP_ACCOUNT_EDITOR,
                SettingsCompletionFlow.afterAccountSave(true, true, false));
        assertEquals(
                SettingsCompletionFlow.ACTION_KEEP_ACCOUNT_EDITOR,
                SettingsCompletionFlow.afterAccountSave(false, false, true));
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
