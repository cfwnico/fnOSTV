package com.fnostv.android4.ui;

import com.fnostv.android4.net.FnosFileEntry;

import java.util.List;

public final class HomePosterSlots {
    public final FnosFileEntry media;
    public final FnosFileEntry recent;
    public final FnosFileEntry favorite;

    private HomePosterSlots(FnosFileEntry media, FnosFileEntry recent, FnosFileEntry favorite) {
        this.media = media;
        this.recent = recent;
        this.favorite = favorite;
    }

    public static HomePosterSlots from(List<FnosFileEntry> media, List<FnosFileEntry> recent, List<FnosFileEntry> favorite) {
        return new HomePosterSlots(select(media), select(recent), select(favorite));
    }

    private static FnosFileEntry select(List<FnosFileEntry> entries) {
        FnosFileEntry firstVideo = null;
        if (entries == null) {
            return null;
        }
        for (int i = 0; i < entries.size(); i++) {
            FnosFileEntry entry = entries.get(i);
            if (entry == null) {
                continue;
            }
            if (entry.posterPath.length() > 0) {
                return entry;
            }
            if (firstVideo == null && entry.isVideo()) {
                firstVideo = entry;
            }
        }
        return firstVideo;
    }
}
