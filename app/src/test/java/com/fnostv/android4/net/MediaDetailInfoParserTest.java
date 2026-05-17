package com.fnostv.android4.net;

import org.json.JSONObject;

public final class MediaDetailInfoParserTest {
    public static void main(String[] args) throws Exception {
        parsesDataItemMetadata();
        parsesChildrenFromEpisodes();
        formatsDurationFromSeconds();
    }

    private static void parsesDataItemMetadata() throws Exception {
        MediaDetailInfo info = MediaDetailInfoParser.parse(new JSONObject(
                "{\"code\":0,\"data\":{\"item\":{\"title\":\"文化大观园\","
                        + "\"description\":\"一档文化纪录片\","
                        + "\"release_date\":\"2024-05-01\","
                        + "\"score\":8.6,"
                        + "\"category\":\"纪录片\","
                        + "\"duration\":2730,"
                        + "\"type\":\"TV\"}}}"));

        assertEquals("文化大观园", info.title);
        assertEquals("一档文化纪录片", info.overview);
        assertEquals("2024", info.year);
        assertEquals("8.6", info.rating);
        assertEquals("纪录片", info.category);
        assertEquals("45:30", info.durationLabel);
        assertEquals("TV", info.sourceLabel);
    }

    private static void parsesChildrenFromEpisodes() throws Exception {
        MediaDetailInfo info = MediaDetailInfoParser.parse(new JSONObject(
                "{\"code\":0,\"data\":{\"detail\":{\"name\":\"剧集\","
                        + "\"episodes\":["
                        + "{\"title\":\"第一集\",\"guid\":\"ep-1\",\"poster\":\"/poster/ep1.webp\",\"type\":\"Video\"},"
                        + "{\"title\":\"第二集\",\"path\":\"/video/ep2.mp4\",\"type\":\"Video\"}]}}}"));

        assertEquals(2, info.children.size());
        assertEquals("第一集", info.children.get(0).name);
        assertEquals("ep-1", info.children.get(0).path);
        assertEquals("/poster/ep1.webp", info.children.get(0).posterPath);
        assertEquals("第二集", info.children.get(1).name);
        assertEquals("/video/ep2.mp4", info.children.get(1).path);
    }

    private static void formatsDurationFromSeconds() throws Exception {
        MediaDetailInfo shortInfo = MediaDetailInfoParser.parse(new JSONObject(
                "{\"data\":{\"title\":\"短片\",\"duration\":75}}"));
        MediaDetailInfo longInfo = MediaDetailInfoParser.parse(new JSONObject(
                "{\"data\":{\"title\":\"长片\",\"duration\":3670}}"));
        MediaDetailInfo millisInfo = MediaDetailInfoParser.parse(new JSONObject(
                "{\"data\":{\"title\":\"毫秒\",\"duration\":2730000}}"));

        assertEquals("1:15", shortInfo.durationLabel);
        assertEquals("1:01:10", longInfo.durationLabel);
        assertEquals("45:30", millisInfo.durationLabel);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
