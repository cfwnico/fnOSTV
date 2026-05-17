package com.fnostv.android4.ui;

import com.fnostv.android4.net.FnosFileEntry;
import com.fnostv.android4.net.FnosPlaybackSource;
import com.fnostv.android4.net.MediaDetailInfo;
import com.fnostv.android4.player.PlaybackSourceSelector;

import java.util.ArrayList;
import java.util.List;

public final class MediaDetailState {
    public final FnosFileEntry entry;
    public boolean favorite;
    public boolean loadingSources;
    public boolean loadingDetail;
    public String errorMessage = "";
    public String detailError = "";
    public MediaDetailInfo detailInfo = MediaDetailInfo.empty();
    private final List<FnosPlaybackSource> sources = new ArrayList<FnosPlaybackSource>();
    private int selectedSourceIndex;

    public MediaDetailState(FnosFileEntry entry, boolean favorite) {
        this.entry = entry;
        this.favorite = favorite;
    }

    public void setLoadingSources(boolean loadingSources) {
        this.loadingSources = loadingSources;
        if (loadingSources) {
            errorMessage = "";
        }
    }

    public void setError(String errorMessage) {
        this.loadingSources = false;
        this.errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public void setLoadingDetail(boolean loadingDetail) {
        this.loadingDetail = loadingDetail;
        if (loadingDetail) {
            detailError = "";
        }
    }

    public void setDetailInfo(MediaDetailInfo detailInfo) {
        this.detailInfo = detailInfo == null ? MediaDetailInfo.empty() : detailInfo;
        loadingDetail = false;
        detailError = "";
    }

    public void setDetailError(String detailError) {
        this.loadingDetail = false;
        this.detailError = detailError == null ? "" : detailError;
    }

    public List<FnosFileEntry> detailChildren() {
        return new ArrayList<FnosFileEntry>(detailInfo.children);
    }

    public void setSources(List<FnosPlaybackSource> resolvedSources) {
        sources.clear();
        sources.addAll(PlaybackSourceSelector.normalize(resolvedSources));
        selectedSourceIndex = PlaybackSourceSelector.clampIndex(sources, selectedSourceIndex);
        loadingSources = false;
        errorMessage = "";
    }

    public List<FnosPlaybackSource> sources() {
        return new ArrayList<FnosPlaybackSource>(sources);
    }

    public List<FnosPlaybackSource> sourcesForPlayback() {
        List<FnosPlaybackSource> ordered = sources();
        if (ordered.size() <= 1) {
            return ordered;
        }
        int selected = PlaybackSourceSelector.clampIndex(ordered, selectedSourceIndex);
        FnosPlaybackSource first = ordered.remove(selected);
        ordered.add(0, first);
        return ordered;
    }

    public void selectSource(int index) {
        selectedSourceIndex = PlaybackSourceSelector.clampIndex(sources, index);
    }

    public int selectedSourceIndex() {
        return selectedSourceIndex;
    }

    public FnosPlaybackSource currentSource() {
        return PlaybackSourceSelector.selectedSource(sources, selectedSourceIndex);
    }

    public String sourceLabel() {
        return PlaybackSourceSelector.displayLabel(sources, selectedSourceIndex);
    }
}
