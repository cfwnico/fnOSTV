package com.fnostv.android4.ui;

public final class DetailActionLayoutTest {
    public static void main(String[] args) {
        splitsFourActionsIntoTwoRows();
        neverCreatesRowsWiderThanTwoActions();
    }

    private static void splitsFourActionsIntoTwoRows() {
        assertEquals(2, DetailActionLayout.rowCount(4));
        assertEquals(2, DetailActionLayout.actionsInRow(4, 0));
        assertEquals(2, DetailActionLayout.actionsInRow(4, 1));
    }

    private static void neverCreatesRowsWiderThanTwoActions() {
        assertEquals(1, DetailActionLayout.rowCount(1));
        assertEquals(1, DetailActionLayout.actionsInRow(1, 0));
        assertEquals(2, DetailActionLayout.rowCount(3));
        assertEquals(2, DetailActionLayout.actionsInRow(3, 0));
        assertEquals(1, DetailActionLayout.actionsInRow(3, 1));
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
