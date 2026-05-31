package com.fnostv.android4.net;

public final class FnosApiException extends Exception {
    public FnosApiException(String message) {
        super(message);
    }

    public FnosApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
