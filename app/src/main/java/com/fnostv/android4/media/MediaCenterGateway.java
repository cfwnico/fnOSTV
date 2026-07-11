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

        FnosFileList items(String path, String category, int pageSize, boolean isAncestor) throws FnosRpcException;
    }

    public interface LocalIndexProvider {
        List<FnosFileEntry> entries();
    }

    public interface RpcProvider {
        FnosFileList entries() throws FnosRpcException;
    }

    public interface FileProvider {
        FnosFileList files(String path) throws FnosRpcException;
    }

    private final RestProvider restProvider;
    private final LocalIndexProvider localIndexProvider;
    private final RpcProvider rpcProvider;
    private final FileProvider fileProvider;

    public MediaCenterGateway(RestProvider restProvider, LocalIndexProvider localIndexProvider, RpcProvider rpcProvider, FileProvider fileProvider) {
        this.restProvider = restProvider;
        this.localIndexProvider = localIndexProvider;
        this.rpcProvider = rpcProvider;
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
            MediaCenterLoad rpc = loadRpc(trace);
            if (rpc.success) {
                return rpc;
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
                return MediaCenterLoad.success(
                        "媒体库",
                        "fnOS 影视媒体库",
                        libraries,
                        false,
                        MediaCenterLoad.SOURCE_REST_LIBRARY);
            }
            FnosFileList all = restProvider.items("", NativeHomeView.ACTION_ALL, 50, true);
            if (hasEntries(all)) {
                return MediaCenterLoad.success("影视大全", "fnOS 影视条目", all, false, MediaCenterLoad.SOURCE_REST_ITEMS);
            }
            trace.add("REST: empty");
        } catch (FnosRpcException ex) {
            trace.add("REST: " + ex.getMessage());
        } catch (RuntimeException ex) {
            trace.add("REST: " + ex.getMessage());
        }
        return MediaCenterLoad.failure("");
    }

    private MediaCenterLoad loadRestChild(String path, List<String> trace) {
        if (restProvider == null) {
            trace.add("REST: provider missing");
            return MediaCenterLoad.failure("");
        }
        try {
            FnosFileList list = restProvider.items(path, NativeHomeView.ACTION_ALL, 50, false);
            if (list != null) {
                return MediaCenterLoad.success("影视大全", path, list, false, MediaCenterLoad.SOURCE_REST_ITEMS);
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
                    "影视大全",
                    "本地媒体库索引",
                    new FnosFileList("mediaIndex", indexed),
                    true,
                    MediaCenterLoad.SOURCE_LOCAL_INDEX);
        }
        return MediaCenterLoad.failure("");
    }

    private MediaCenterLoad loadRpc(List<String> trace) {
        if (rpcProvider == null) {
            trace.add("RPC: provider missing");
            return MediaCenterLoad.failure("");
        }
        try {
            FnosFileList list = rpcProvider.entries();
            if (hasEntries(list)) {
                return MediaCenterLoad.success("影视中心", "fnOS mediaCenter 回退", list, false, MediaCenterLoad.SOURCE_RPC_MEDIACENTER);
            }
            trace.add("RPC: empty");
        } catch (FnosRpcException ex) {
            trace.add("RPC: " + ex.getMessage());
        } catch (RuntimeException ex) {
            trace.add("RPC: " + ex.getMessage());
        }
        return MediaCenterLoad.failure("");
    }

    private MediaCenterLoad loadFiles(String path, List<String> trace) {
        if (fileProvider == null) {
            trace.add("文件: provider missing");
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
            trace.add("文件: " + ex.getMessage());
        } catch (RuntimeException ex) {
            trace.add("文件: " + ex.getMessage());
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
            return "暂无可用影视内容";
        }
        StringBuilder builder = new StringBuilder("暂无可用影视内容：");
        for (int i = 0; i < trace.size(); i++) {
            if (i > 0) {
                builder.append("；");
            }
            builder.append(trace.get(i));
        }
        return builder.toString();
    }
}
