package com.fnostv.android4.ui;

final class DetailActionLayout {
    static final int MAX_ACTIONS_PER_ROW = 2;

    private DetailActionLayout() {
    }

    static int rowCount(int actionCount) {
        if (actionCount <= 0) {
            return 0;
        }
        return (actionCount + MAX_ACTIONS_PER_ROW - 1) / MAX_ACTIONS_PER_ROW;
    }

    static int actionsInRow(int actionCount, int rowIndex) {
        if (actionCount <= 0 || rowIndex < 0 || rowIndex >= rowCount(actionCount)) {
            return 0;
        }
        int remaining = actionCount - rowIndex * MAX_ACTIONS_PER_ROW;
        return Math.min(MAX_ACTIONS_PER_ROW, remaining);
    }
}
