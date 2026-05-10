package com.fnostv.android4.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FnosFileList {
    public final String path;
    public final List<FnosFileEntry> entries;

    public FnosFileList(String path, List<FnosFileEntry> entries) {
        this.path = path == null ? "" : path;
        this.entries = Collections.unmodifiableList(new ArrayList<FnosFileEntry>(entries));
    }
}
