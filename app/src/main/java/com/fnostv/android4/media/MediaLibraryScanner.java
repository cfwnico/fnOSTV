package com.fnostv.android4.media;

import com.fnostv.android4.net.FnosFileEntry;
import com.fnostv.android4.net.FnosFileList;
import com.fnostv.android4.net.FnosRpcClient;
import com.fnostv.android4.net.FnosRpcException;
import com.fnostv.android4.net.FnosSession;

import java.util.ArrayList;
import java.util.List;

public final class MediaLibraryScanner {
    private static final int MAX_DEPTH = 4;
    private static final int MAX_DIRS = 200;
    private static final int MAX_ITEMS = 1000;

    private final FnosRpcClient client;
    private final FnosSession session;

    public MediaLibraryScanner(FnosRpcClient client, FnosSession session) {
        this.client = client;
        this.session = session;
    }

    public List<FnosFileEntry> scan(List<MediaLibrary> libraries) throws FnosRpcException {
        List<FnosFileEntry> entries = new ArrayList<FnosFileEntry>();
        if (libraries == null) {
            return entries;
        }
        for (int i = 0; i < libraries.size() && entries.size() < MAX_ITEMS; i++) {
            MediaLibrary library = libraries.get(i);
            if (library != null && library.enabled) {
                scanLibrary(library, entries);
            }
        }
        return entries;
    }

    private void scanLibrary(MediaLibrary library, List<FnosFileEntry> entries) throws FnosRpcException {
        for (int i = 0; i < library.paths.size() && entries.size() < MAX_ITEMS; i++) {
            scanRoot(library.paths.get(i), entries);
        }
    }

    private void scanRoot(String rootPath, List<FnosFileEntry> entries) throws FnosRpcException {
        List<DirNode> queue = new ArrayList<DirNode>();
        List<String> visited = new ArrayList<String>();
        queue.add(new DirNode(rootPath, 0));
        int index = 0;
        while (index < queue.size() && visited.size() < MAX_DIRS && entries.size() < MAX_ITEMS) {
            DirNode node = queue.get(index++);
            String path = MediaLibraryClassifier.normalizePath(node.path);
            if (visited.contains(path)) {
                continue;
            }
            visited.add(path);
            FnosFileList list = client.listDir(session, path);
            for (int i = 0; i < list.entries.size() && entries.size() < MAX_ITEMS; i++) {
                FnosFileEntry entry = list.entries.get(i);
                if (entry.directory && node.depth < MAX_DEPTH) {
                    String childPath = MediaLibraryClassifier.normalizePath(entry.path);
                    if (childPath.length() > 0 && !visited.contains(childPath)) {
                        queue.add(new DirNode(childPath, node.depth + 1));
                    }
                } else if (entry.isVideo() && !containsPath(entries, entry.path)) {
                    entries.add(entry);
                }
            }
        }
    }

    private boolean containsPath(List<FnosFileEntry> entries, String path) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).path.equals(path)) {
                return true;
            }
        }
        return false;
    }

    private static final class DirNode {
        final String path;
        final int depth;

        DirNode(String path, int depth) {
            this.path = path == null ? "" : path;
            this.depth = depth;
        }
    }
}
