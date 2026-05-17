package com.fnostv.android4.ui;

final class BitmapSampleSize {
    private BitmapSampleSize() {
    }

    static int forBounds(int width, int height, int maxWidth, int maxHeight) {
        if (width <= 0 || height <= 0 || maxWidth <= 0 || maxHeight <= 0) {
            return 1;
        }
        int sample = 1;
        while ((width / (sample * 2)) >= maxWidth || (height / (sample * 2)) >= maxHeight) {
            sample *= 2;
        }
        return sample;
    }
}
