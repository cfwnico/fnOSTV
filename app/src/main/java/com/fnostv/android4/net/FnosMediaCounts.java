package com.fnostv.android4.net;

public final class FnosMediaCounts {
    public final int favoriteCount;
    public final int libraryCount;
    public final int totalCount;
    public final int movieCount;
    public final int tvCount;
    public final int otherCount;

    public FnosMediaCounts(int favoriteCount, int libraryCount, int totalCount, int movieCount, int tvCount, int otherCount) {
        this.favoriteCount = Math.max(0, favoriteCount);
        this.libraryCount = Math.max(0, libraryCount);
        this.totalCount = Math.max(0, totalCount);
        this.movieCount = Math.max(0, movieCount);
        this.tvCount = Math.max(0, tvCount);
        this.otherCount = Math.max(0, otherCount);
    }
}
