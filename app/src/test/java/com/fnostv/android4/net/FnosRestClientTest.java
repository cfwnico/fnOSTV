package com.fnostv.android4.net;

import org.json.JSONArray;
import org.json.JSONObject;

public final class FnosRestClientTest {
    public static void main(String[] args) throws Exception {
        mediaItemPayloadMatchesWebContract();
        parsesMediaCountsFromServerSummary();
        parsesRestItemsIntoNativeBrowserEntries();
        parsesPosterMetadataFromRestItems();
        buildsPosterImageUrlFromServerPath();
    }

    private static void mediaItemPayloadMatchesWebContract() throws Exception {
        JSONObject payload = new JSONObject(FnosRestClient.mediaItemPayload("library-guid", "all", 30));

        assertEquals("library-guid", payload.optString("ancestor_guid"));
        assertEquals("DESC", payload.optString("sort_type"));
        assertEquals("create_time", payload.optString("sort_column"));
        assertEquals(1, payload.optInt("exclude_grouped_video"));
        assertEquals(1, payload.optInt("page"));
        assertEquals(30, payload.optInt("page_size"));

        JSONArray types = payload.getJSONObject("tags").getJSONArray("type");
        assertEquals(4, types.length());
        assertEquals("Movie", types.getString(0));
        assertEquals("TV", types.getString(1));
        assertEquals("Directory", types.getString(2));
        assertEquals("Video", types.getString(3));
    }

    private static void parsesMediaCountsFromServerSummary() throws Exception {
        FnosMediaCounts counts = FnosRestClient.parseMediaCounts(new JSONObject(
                "{\"code\":0,\"data\":{\"f044a109c71c427491cb0135371bb263\":2,"
                        + "\"favorite\":1,\"movie\":3,\"total\":7,\"tv\":2,\"video\":4}}"));

        assertEquals(1, counts.favoriteCount);
        assertEquals(1, counts.libraryCount);
        assertEquals(7, counts.totalCount);
        assertEquals(3, counts.movieCount);
        assertEquals(2, counts.tvCount);
        assertEquals(4, counts.otherCount);
    }

    private static void parsesRestItemsIntoNativeBrowserEntries() throws Exception {
        JSONObject response = new JSONObject(
                "{\"code\":0,\"data\":{\"total\":3,\"list\":["
                        + "{\"guid\":\"tv-guid\",\"title\":\"文化大观园\",\"type\":\"TV\","
                        + "\"single_child_guid\":\"child-guid\"},"
                        + "{\"guid\":\"dir-guid\",\"title\":\"测试\",\"type\":\"Directory\","
                        + "\"path\":\"/vol2/1000/测试\"},"
                        + "{\"guid\":\"video-guid\",\"title\":\"clip.mp4\",\"type\":\"Video\","
                        + "\"path\":\"/vol2/1000/clip.mp4\",\"duration\":12}]}}");

        FnosFileList list = FnosRestClient.parseMediaItems("library-guid", response);

        assertEquals("library-guid", list.path);
        assertEquals(3, list.entries.size());
        assertEquals("文化大观园", list.entries.get(0).name);
        assertEquals("child-guid", list.entries.get(0).path);
        assertTrue(list.entries.get(0).directory);
        assertEquals("/vol2/1000/测试", list.entries.get(1).path);
        assertTrue(list.entries.get(1).directory);
        assertTrue(list.entries.get(2).isVideo());
        assertFalse(list.entries.get(2).directory);
    }

    private static void parsesPosterMetadataFromRestItems() throws Exception {
        JSONObject response = new JSONObject(
                "{\"code\":0,\"data\":{\"total\":2,\"list\":["
                        + "{\"guid\":\"tv-guid\",\"title\":\"Show\",\"type\":\"TV\","
                        + "\"poster\":\"/ad/18/show.webp\"},"
                        + "{\"guid\":\"dir-guid\",\"title\":\"Dir\",\"type\":\"Directory\","
                        + "\"poster_list\":[\"/6f/04/one.webp\",\"/f1/12/two.webp\"]}]}}");

        FnosFileList list = FnosRestClient.parseMediaItems("library-guid", response);

        assertEquals("/ad/18/show.webp", list.entries.get(0).posterPath);
        assertEquals("/6f/04/one.webp", list.entries.get(1).posterPath);
    }

    private static void buildsPosterImageUrlFromServerPath() {
        assertEquals(
                "http://192.168.0.198:5666/v/api/v1/sys/img/ad/18/show.webp?w=400",
                FnosRestClient.posterImageUrl("http://192.168.0.198:5666", "/ad/18/show.webp", 400));
        assertEquals(
                "http://192.168.0.198:5666/v/api/v1/sys/img/ad/18/show.webp",
                FnosRestClient.posterImageUrl("http://192.168.0.198:5666/", "ad/18/show.webp", 0));
        assertEquals(
                "https://cdn.example/poster.webp",
                FnosRestClient.posterImageUrl("http://192.168.0.198:5666", "https://cdn.example/poster.webp", 400));
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }

    private static void assertTrue(boolean value) {
        if (!value) {
            throw new AssertionError("Expected true");
        }
    }

    private static void assertFalse(boolean value) {
        if (value) {
            throw new AssertionError("Expected false");
        }
    }
}
