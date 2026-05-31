package com.fnostv.android4.media;

import com.fnostv.android4.net.FnosFileEntry;
import com.fnostv.android4.net.FnosFileList;
import com.fnostv.android4.net.FnosApiException;
import com.fnostv.android4.ui.FileBrowserLabels;
import com.fnostv.android4.ui.NativeHomeView;

import java.util.ArrayList;
import java.util.List;

public final class MediaCenterGatewayTest {
    public static void main(String[] args) throws Exception {
        restLibraryItemsWin();
        triesNextRestLibraryWhenFirstLibraryIsEmpty();
        emptyRestFallsBackToLocalIndex();
        restChildFailureFallsBackToFileList();
        rootRestAndLocalFailureFallsBackToFileList();
        allFailuresReturnTraceMessage();
    }

    private static void restLibraryItemsWin() {
        FakeRest rest = new FakeRest();
        rest.libraries = list("libraries", dir("Library A", "guid-a"));
        rest.items = list("guid-a", video("Movie A", "guid-video"));
        FakeLocal local = new FakeLocal(video("Local", "/local"));
        FakeFile file = new FakeFile(list("", video("File", "/file")));

        MediaCenterLoad result = new MediaCenterGateway(rest, local, file).load("");

        assertEquals(true, result.success);
        assertEquals("Library A", result.title);
        assertEquals("Movie A", result.list.entries.get(0).name);
        assertEquals(MediaCenterLoad.SOURCE_REST_LIBRARY, result.source);
    }

    private static void triesNextRestLibraryWhenFirstLibraryIsEmpty() {
        FakeRest rest = new FakeRest();
        rest.libraries = list("libraries", dir("Empty Library", "guid-empty"), dir("Library B", "guid-b"));
        rest.items = list("guid-empty");
        rest.secondPath = "guid-b";
        rest.secondItems = list("guid-b", video("Movie B", "guid-video-b"));
        FakeLocal local = new FakeLocal(video("Local", "/local"));
        FakeFile file = new FakeFile(list("", video("File", "/file")));

        MediaCenterLoad result = new MediaCenterGateway(rest, local, file).load("");

        assertEquals(true, result.success);
        assertEquals("Library B", result.title);
        assertEquals("Movie B", result.list.entries.get(0).name);
        assertEquals(MediaCenterLoad.SOURCE_REST_LIBRARY, result.source);
    }

    private static void emptyRestFallsBackToLocalIndex() {
        FakeRest rest = new FakeRest();
        rest.libraries = list("libraries");
        rest.items = list("");
        FakeLocal local = new FakeLocal(video("Local", "/local"));
        FakeFile file = new FakeFile(list("", video("File", "/file")));

        MediaCenterLoad result = new MediaCenterGateway(rest, local, file).load("");

        assertEquals(true, result.success);
        assertEquals("Local", result.list.entries.get(0).name);
        assertEquals(MediaCenterLoad.SOURCE_LOCAL_INDEX, result.source);
    }

    private static void restChildFailureFallsBackToFileList() {
        FakeRest rest = new FakeRest();
        rest.failMessage = "REST child failed";
        FakeLocal local = new FakeLocal(video("Local", "/local"));
        FakeFile file = new FakeFile(list("guid-a", video("File", "/file")));

        MediaCenterLoad result = new MediaCenterGateway(rest, local, file).load("guid-a");

        assertEquals(true, result.success);
        assertEquals(FileBrowserLabels.mediaFallbackTitle(), result.title);
        assertEquals(FileBrowserLabels.mediaFallbackSubtitle("guid-a"), result.subtitle);
        assertEquals("File", result.list.entries.get(0).name);
        assertEquals(MediaCenterLoad.SOURCE_FILE_FALLBACK, result.source);
    }

    private static void rootRestAndLocalFailureFallsBackToFileList() {
        FakeRest rest = new FakeRest();
        rest.failMessage = "REST down";
        FakeLocal local = new FakeLocal();
        FakeFile file = new FakeFile(list("", video("File", "/file")));

        MediaCenterLoad result = new MediaCenterGateway(rest, local, file).load("");

        assertEquals(true, result.success);
        assertEquals("File", result.list.entries.get(0).name);
        assertEquals(MediaCenterLoad.SOURCE_FILE_FALLBACK, result.source);
    }

    private static void allFailuresReturnTraceMessage() {
        FakeRest rest = new FakeRest();
        rest.failMessage = "REST down";
        FakeLocal local = new FakeLocal();
        FakeFile file = new FakeFile(null);
        file.failMessage = "file unavailable";

        MediaCenterLoad result = new MediaCenterGateway(rest, local, file).load("");

        assertEquals(false, result.success);
        assertContains(result.message, "REST: REST down");
        assertContains(result.message, "file unavailable");
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
        String secondPath = "";
        FnosFileList secondItems = list("");
        String failMessage = "";

        @Override
        public FnosFileList libraries() throws FnosApiException {
            if (failMessage.length() > 0) {
                throw new FnosApiException(failMessage);
            }
            return libraries;
        }

        @Override
        public FnosFileList items(String path, String category, int pageSize) throws FnosApiException {
            if (failMessage.length() > 0) {
                throw new FnosApiException(failMessage);
            }
            assertEquals(NativeHomeView.ACTION_ALL, category);
            assertEquals(50, pageSize);
            if (path.equals(secondPath)) {
                return secondItems;
            }
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

    private static final class FakeFile implements MediaCenterGateway.FileProvider {
        private final FnosFileList files;
        String failMessage = "";

        FakeFile(FnosFileList files) {
            this.files = files;
        }

        @Override
        public FnosFileList files(String path) throws FnosApiException {
            if (failMessage.length() > 0) {
                throw new FnosApiException(failMessage);
            }
            if (files == null) {
                throw new FnosApiException("no files");
            }
            return files;
        }
    }
}
