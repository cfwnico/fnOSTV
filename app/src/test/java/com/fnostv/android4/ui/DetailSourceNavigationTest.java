package com.fnostv.android4.ui;

public final class DetailSourceNavigationTest {
    public static void main(String[] args) {
        movesForwardAndBackwardWithWraparound();
        keepsSingleOrEmptySourceAtZero();
    }

    private static void movesForwardAndBackwardWithWraparound() {
        assertEquals(1, DetailSourceNavigation.move(0, 3, 1));
        assertEquals(2, DetailSourceNavigation.move(0, 3, -1));
        assertEquals(0, DetailSourceNavigation.move(2, 3, 1));
    }

    private static void keepsSingleOrEmptySourceAtZero() {
        assertEquals(0, DetailSourceNavigation.move(0, 1, 1));
        assertEquals(0, DetailSourceNavigation.move(0, 0, -1));
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
