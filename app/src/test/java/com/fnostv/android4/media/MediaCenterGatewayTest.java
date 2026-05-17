package com.fnostv.android4.media;

import com.fnostv.android4.net.FnosFileEntry;
import com.fnostv.android4.net.FnosFileList;
import com.fnostv.android4.net.FnosRpcException;
import com.fnostv.android4.ui.NativeHomeView;

import java.util.ArrayList;
import java.util.List;

public final class MediaCenterGatewayTest {
    public static void main(String[] args) throws Exception {
        restLibraryItemsWin();
        emptyRestFallsBackToLocalIndex();
        localEmptyFallsBackToRpc();
        restChildFailureFallsBackToFileList();
        allFailuresReturnTraceMessage();
    }

    private static void restLibraryItemsWin() {
        FakeRest rest = new FakeRest();
        rest.libraries = list("libraries", dir("Library A", "guid-a"));
        rest.items = list("guid-a", video("Movie A", "guid-video"));
        FakeLocal local = new FakeLocal(video("Local", "/local"));
        FakeRpc rpc = new FakeRpc(video("RPC", "/rpc"));
        FakeFile file = new FakeFile(list("", video("File", "/file")));

        MediaCenterLoad result = new MediaCenterGateway(rest, local, rpc, file).load("");

        assertEquals(true, result.success);
        assertEquals("Library A", result.title);
        assertEquals("fnOS 影视媒体库", result.subtitle);
        assertEquals("Movie A", result.list.entries.get(0).name);
        assertEquals(MediaCenterLoad.SOURCE_REST_LIBRARY, result.source);
    }

    private static void emptyRestFallsBackToLocalIndex() {
        FakeRest rest = new FakeRest();
        rest.libraries = list("libraries");
        rest.items = list("");
        FakeLocal local = new FakeLocal(video("Local", "/local"));
        FakeRpc rpc = new FakeRpc(video("RPC", "/rpc"));
        FakeFile file = new FakeFile(list("", video("File", "/file")));

        MediaCenterLoad result = new MediaCenterGateway(rest, local, rpc, file).load("");

        assertEquals(true, result.success);
        assertEquals("影视大全", result.title);
        assertEquals("本地媒体库索引", result.subtitle);
        assertEquals("Local", result.list.entries.get(0).name);
        assertEquals(MediaCenterLoad.SOURCE_LOCAL_INDEX, result.source);
    }

    private static void localEmptyFallsBackToRpc() {
        FakeRest rest = new FakeRest();
        rest.failMessage = "HTTP 500";
        FakeLocal local = new FakeLocal();
        FakeRpc rpc = new FakeRpc(video("RPC", "/rpc"));
        FakeFile file = new FakeFile(list("", video("File", "/file")));

        MediaCenterLoad result = new MediaCenterGateway(rest, local, rpc, file).load("");

        assertEquals(true, result.success);
        assertEquals("影视中心", result.title);
        assertEquals("fnOS mediaCenter 回退", result.subtitle);
        assertEquals("RPC", result.list.entries.get(0).name);
        assertEquals(MediaCenterLoad.SOURCE_RPC_MEDIACENTER, result.source);
    }

    private static void restChildFailureFallsBackToFileList() {
        FakeRest rest = new FakeRest();
        rest.failMessage = "REST child failed";
        FakeLocal local = new FakeLocal(video("Local", "/local"));
        FakeRpc rpc = new FakeRpc(video("RPC", "/rpc"));
        FakeFile file = new FakeFile(list("guid-a", video("File", "/file")));

        MediaCenterLoad result = new MediaCenterGateway(rest, local, rpc, file).load("guid-a");

        assertEquals(true, result.success);
        assertEquals("影视目录", result.title);
        assertEquals("文件模式回退：guid-a", result.subtitle);
        assertEquals("File", result.list.entries.get(0).name);
        assertEquals(MediaCenterLoad.SOURCE_FILE_FALLBACK, result.source);
    }

    private static void allFailuresReturnTraceMessage() {
        FakeRest rest = new FakeRest();
        rest.failMessage = "REST down";
        FakeLocal local = new FakeLocal();
        FakeRpc rpc = new FakeRpc();
        rpc.failMessage = "errno=10000002";
        FakeFile file = new FakeFile(null);
        file.failMessage = "file unavailable";

        MediaCenterLoad result = new MediaCenterGateway(rest, local, rpc, file).load("");

        assertEquals(false, result.success);
        assertContains(result.message, "REST: REST down");
        assertContains(result.message, "RPC: errno=10000002");
        assertContains(result.message, "文件: file unavailable");
    }

    private static FnosFileEntry video(String name, String path) {
        return new FnosFileEntry(name, path, false, 0L, "video/mp4", "");
    }

    private static FnosFileEntry dir(String name, String path) {
        return new FnosFileEntry(name, path, true, 0L, "MediaDB", "");
    }

    private static FnosFileList list(String path, FnosFileEntry... entries) {
        List<FnosFileEntry> values = new ArrayList<FnosFileEntry>();
        for (int i = 0; i < entries.length; i++) {
            values.add(entries[i]);
        }
        return new FnosFileList(path, values);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }

    private static void assertContains(String value, String needle) {
        if (value == null || value.indexOf(needle) < 0) {
            throw new AssertionError("Expected <" + value + "> to contain <" + needle + ">");
        }
    }

    private static final class FakeRest implements MediaCenterGateway.RestProvider {
        FnosFileList libraries = list("libraries");
        FnosFileList items = list("");
        String failMessage = "";

        @Override
        public FnosFileList libraries() throws FnosRpcException {
            if (failMessage.length() > 0) {
                throw new FnosRpcException(failMessage);
            }
            return libraries;
        }

        @Override
        public FnosFileList items(String path, String category, int pageSize) throws FnosRpcException {
            if (failMessage.length() > 0) {
                throw new FnosRpcException(failMessage);
            }
            assertEquals(NativeHomeView.ACTION_ALL, category);
            assertEquals(50, pageSize);
            return items;
        }
    }

    private static final class FakeLocal implements MediaCenterGateway.LocalIndexProvider {
        private final List<FnosFileEntry> entries = new ArrayList<FnosFileEntry>();

        FakeLocal(FnosFileEntry... values) {
            for (int i = 0; i < values.length; i++) {
                entries.add(values[i]);
            }
        }

        @Override
        public List<FnosFileEntry> entries() {
            return entries;
        }
    }

    private static final class FakeRpc implements MediaCenterGateway.RpcProvider {
        private final FnosFileList entries;
        String failMessage = "";

        FakeRpc(FnosFileEntry... values) {
            entries = list("mediaCenter", values);
        }

        @Override
        public FnosFileList entries() throws FnosRpcException {
            if (failMessage.length() > 0) {
                throw new FnosRpcException(failMessage);
            }
            return entries;
        }
    }

    private static final class FakeFile implements MediaCenterGateway.FileProvider {
        private final FnosFileList files;
        String failMessage = "";

        FakeFile(FnosFileList files) {
            this.files = files;
        }

        @Override
        public FnosFileList files(String path) throws FnosRpcException {
            if (failMessage.length() > 0) {
                throw new FnosRpcException(failMessage);
            }
            if (files == null) {
                throw new FnosRpcException("no files");
            }
            return files;
        }
    }
}
