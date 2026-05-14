package com.fnostv.android4.config;

public final class ProfileValidation {
    public static final int OK = 0;
    public static final int EMPTY_BASE_URL = 1;
    public static final int UNSUPPORTED_SCHEME = 2;

    private final int code;
    private final String message;

    ProfileValidation(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public boolean isValid() {
        return code == OK;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
