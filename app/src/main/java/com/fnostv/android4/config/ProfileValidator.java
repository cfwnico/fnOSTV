package com.fnostv.android4.config;

public final class ProfileValidator {
    private ProfileValidator() {
    }

    public static ProfileValidation validate(ServerProfile profile) {
        if (profile == null || profile.baseUrl.length() == 0) {
            return new ProfileValidation(ProfileValidation.EMPTY_BASE_URL, "请填写服务器地址");
        }
        if (!profile.baseUrl.startsWith("http://") && !profile.baseUrl.startsWith("https://")) {
            return new ProfileValidation(ProfileValidation.UNSUPPORTED_SCHEME, "服务器地址仅支持 HTTP 或 HTTPS");
        }
        return new ProfileValidation(ProfileValidation.OK, "");
    }
}
