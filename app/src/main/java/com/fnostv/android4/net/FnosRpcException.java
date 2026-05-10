package com.fnostv.android4.net;

public final class FnosRpcException extends Exception {
    public FnosRpcException(String message) {
        super(message);
    }

    public FnosRpcException(String message, Throwable cause) {
        super(message, cause);
    }
}
