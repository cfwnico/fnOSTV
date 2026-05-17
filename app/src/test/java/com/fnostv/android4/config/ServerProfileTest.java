package com.fnostv.android4.config;

public final class ServerProfileTest {
    public static void main(String[] args) {
        normalizesVideoLoginUrlToServerOrigin();
        normalizesVideoAppUrlToServerOrigin();
        preservesPlainServerOrigin();
    }

    private static void normalizesVideoLoginUrlToServerOrigin() {
        ServerProfile profile = new ServerProfile(
                "https://xdorghub.fnos.net/v/login?redirect_uri=https%3A%2F%2Fxdorghub.fnos.net%2Fv",
                "XDORG",
                "password",
                true,
                false);

        assertEquals("https://xdorghub.fnos.net", profile.baseUrl);
    }

    private static void normalizesVideoAppUrlToServerOrigin() {
        ServerProfile profile = new ServerProfile(
                "http://192.168.0.198:5666/v",
                "XDORG",
                "password",
                true,
                false);

        assertEquals("http://192.168.0.198:5666", profile.baseUrl);
    }

    private static void preservesPlainServerOrigin() {
        ServerProfile profile = new ServerProfile(
                "192.168.0.198:5666",
                "XDORG",
                "password",
                true,
                false);

        assertEquals("http://192.168.0.198:5666", profile.baseUrl);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
