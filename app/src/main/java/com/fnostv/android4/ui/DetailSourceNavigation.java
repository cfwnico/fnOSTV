package com.fnostv.android4.ui;

public final class DetailSourceNavigation {
    private DetailSourceNavigation() {
    }

    public static int move(int currentIndex, int sourceCount, int direction) {
        if (sourceCount <= 1) {
            return 0;
        }
        int next = currentIndex + direction;
        while (next < 0) {
            next += sourceCount;
        }
        return next % sourceCount;
    }
}
