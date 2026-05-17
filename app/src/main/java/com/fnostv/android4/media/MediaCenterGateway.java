package com.fnostv.android4.media;

import com.fnostv.android4.net.FnosFileEntry;
import com.fnostv.android4.net.FnosFileList;
import com.fnostv.android4.net.FnosRpcException;
import com.fnostv.android4.ui.FileBrowserLabels;
import com.fnostv.android4.ui.NativeHomeView;

import java.util.ArrayList;
import java.util.List;

public final class MediaCenterGateway {
    public interface RestProvider {
        FnosFileList libraries() throws FnosRpcException;

        FnosFileList items(String path, String category, int pageSize) throws FnosRpcException;
    }

    public interface LocalIndexProvider {
        List<FnosFileEntry> entries();
    }

    public interface FileProvider {
        FnosFileList files(String path) throws FnosRpcException;
    }

    private final RestProvider restProvider;
    private final LocalIndexProvider localIndexProvider;
    private final FileProvider fileProvider;

    public MediaCenterGateway(RestProvider restProvider, LocalIndexProvider localIndexProvider, FileProvider fileProvider) {
        this.restProvider = restProvider;
        this.localIndexProvider = localIndexProvider;
        this.fileProvider = fileProvider;
    }

    public MediaCenterLoad load(String path) {
        String value = path == null ? "" : path;
        List<String> trace = new ArrayList<String>();
        if (value.length() == 0) {
            MediaCenterLoad rest = loadRestRoot(trace);
            if (rest.success) {
                return rest;
            }
            MediaCenterLoad local = loadLocalIndex();
            if (local.success) {
                return local;
            }
        } else if (isRestMediaPath(value)) {
            MediaCenterLoad child = loadRestChild(value, trace);
            if (child.success) {
                return child;
            }
        }
        MediaCenterLoad files = loadFiles(value, trace);
        if (files.success) {
            return files;
        }
        return MediaCenterLoad.failure(traceMessage(trace));
    }

    private MediaCenterLoad loadRestRoot(List<String> trace) {
        if (restProvider == null) {
            trace.add("REST: provider missing");
            return MediaCenterLoad.failure("");
        }
        try {
            FnosFileList libraries = restProvider.libraries();
            if (hasEntries(libraries)) {
                MediaCenterLoad libraryLoad = loadFirstRestLibraryWithEntries(libraries);
                if (libraryLoad.success) {
                    return libraryLoad;
                }
            }
            FnosFileList all = restProvider.items("", NativeHomeView.ACTION_ALL, 50);
            if (hasEntries(all)) {
                return MediaCenterLoad.success("\u5f71\u89c6\u5927\u5168", "fnOS \u5f71\u89c6\u6761\u76ee", all, false, MediaCenterLoad.SOURCE_REST_ITEMS);
            }
            trace.add("REST: empty");
        } catch (FnosRpcException ex) {
            trace.add("REST: " + ex.getMessage());
        } catch (RuntimeException ex) {
            trace.add("REST: " + ex.getMessage());
        }
        return MediaCenterLoad.failure("");
    }

    private MediaCenterLoad loadFirstRestLibraryWithEntries(FnosFileList libraries) throws FnosRpcException {
        for (int i = 0; i < libraries.entries.size(); i++) {
            FnosFileEntry library = libraries.entries.get(i);
            if (library == null || library.path.length() == 0) {
                continue;
            }
            FnosFileList list = restProvider.items(library.path, NativeHomeView.ACTION_ALL, 50);
            if (hasEntries(list)) {
                return MediaCenterLoad.success(
                        library.name.length() == 0 ? "\u5f71\u89c6\u5927\u5168" : library.name,
                        "fnOS \u5f71\u89c6\u5a92\u4f53\u5e93",
                        list,
                        false,
                        MediaCenterLoad.SOURCE_REST_LIBRARY);
            }
        }
        return MediaCenterLoad.failure("");
    }

    private MediaCenterLoad loadRestChild(String path, List<String> trace) {
        if (restProvider == null) {
            trace.add("REST: provider missing");
            return MediaCenterLoad.failure("");
        }
        try {
            FnosFileList list = restProvider.items(path, NativeHomeView.ACTION_ALL, 50);
            if (list != null) {
                return MediaCenterLoad.success("\u5f71\u89c6\u5927\u5168", path, list, false, MediaCenterLoad.SOURCE_REST_ITEMS);
            }
            trace.add("REST: empty child");
        } catch (FnosRpcException ex) {
            trace.add("REST: " + ex.getMessage());
        } catch (RuntimeException ex) {
            trace.add("REST: " + ex.getMessage());
        }
        return MediaCenterLoad.failure("");
    }

    private MediaCenterLoad loadLocalIndex() {
        if (localIndexProvider == null) {
            return MediaCenterLoad.failure("");
        }
        List<FnosFileEntry> indexed = localIndexProvider.entries();
        if (indexed != null && indexed.size() > 0) {
            return MediaCenterLoad.success(
                    "\u5f71\u89c6\u5927\u5168",
                    "\u672c\u5730\u5a92\u4f53\u5e93\u7d22\u5f15",
                    new FnosFileList("mediaIndex", indexed),
                    true,
                    MediaCenterLoad.SOURCE_LOCAL_INDEX);
        }
        return MediaCenterLoad.failure("");
    }

    private MediaCenterLoad loadFiles(String path, List<String> trace) {
        if (fileProvider == null) {
            trace.add("\u6587\u4ef6: provider missing");
            return MediaCenterLoad.failure("");
        }
        try {
            FnosFileList list = fileProvider.files(path);
            return MediaCenterLoad.success(
                    FileBrowserLabels.mediaFallbackTitle(),
                    FileBrowserLabels.mediaFallbackSubtitle(path),
                    list,
                    true,
                    MediaCenterLoad.SOURCE_FILE_FALLBACK);
        } catch (FnosRpcException ex) {
            trace.add("\u6587\u4ef6: " + ex.getMessage());
        } catch (RuntimeException ex) {
            trace.add("\u6587\u4ef6: " + ex.getMessage());
        }
        return MediaCenterLoad.failure("");
    }

    private static boolean hasEntries(FnosFileList list) {
        return list != null && list.entries != null && list.entries.size() > 0;
    }

    private static boolean isRestMediaPath(String path) {
        return path != null && path.length() > 0 && path.indexOf('/') < 0 && !path.startsWith("vol");
    }

    private static String traceMessage(List<String> trace) {
        if (trace == null || trace.size() == 0) {
            return "\u6682\u65e0\u53ef\u7528\u5f71\u89c6\u5185\u5bb9";
        }
        StringBuilder builder = new StringBuilder("\u6682\u65e0\u53ef\u7528\u5f71\u89c6\u5185\u5bb9: ");
        for (int i = 0; i < trace.size(); i++) {
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(trace.get(i));
        }
        return builder.toString();
    }
}
