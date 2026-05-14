package com.fnostv.android4.net;

public final class FnosSession {
    public final String token;
    public final String longToken;
    public final String secretHex;
    public final String user;
    public final String machineId;
    public final String uid;

    public FnosSession(String token, String longToken, String secretHex, String user, String machineId, String uid) {
        this.token = token == null ? "" : token;
        this.longToken = longToken == null ? "" : longToken;
        this.secretHex = secretHex == null ? "" : secretHex;
        this.user = user == null ? "" : user;
        this.machineId = machineId == null ? "" : machineId;
        this.uid = uid == null ? "" : uid;
    }

    public boolean hasToken() {
        return token.length() > 0 && secretHex.length() > 0;
    }
}
