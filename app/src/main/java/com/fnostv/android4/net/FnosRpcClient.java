package com.fnostv.android4.net;

import com.fnostv.android4.config.ServerProfile;
import com.fnostv.android4.util.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public final class FnosRpcClient {
    private static final int OPEN_TIMEOUT_SECONDS = 10;
    private static final int REQUEST_TIMEOUT_SECONDS = 15;
    private static final String BACK_ID_SEED = "0000000000000000";

    private final OkHttpClient httpClient;
    private final ServerProfile profile;
    private final String deviceId;
    private final Map<String, PendingCall> pendingCalls = new HashMap<String, PendingCall>();
    private WebSocket webSocket;
    private CountDownLatch openLatch;
    private FnosRpcException openError;
    private int requestId;
    private String backId = BACK_ID_SEED;
    private String si = "";
    private String publicKey = "";
    private byte[] loginAesKey;
    private byte[] loginIv;

    public FnosRpcClient(ServerProfile profile, String deviceId) {
        this.profile = profile;
        this.deviceId = deviceId;
        httpClient = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .connectTimeout(OPEN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    public void connect() throws FnosRpcException {
        openLatch = new CountDownLatch(1);
        Request request = new Request.Builder().url(webSocketUrl(profile.baseUrl)).build();
        webSocket = httpClient.newWebSocket(request, new Listener());
        awaitOpen();
    }

    public FnosSession login() throws FnosRpcException {
        connect();
        try {
            loadPublicKey();
            JSONObject response = send(encryptForLogin(loginPayload()), REQUEST_TIMEOUT_SECONDS);
            String secretHex = response.optString("secret");
            if (secretHex.length() > 0) {
                secretHex = FnosCrypto.aesDecryptUtf8(secretHex, loginAesKey, loginIv);
            }
            FnosSession session = new FnosSession(
                    response.optString("token"),
                    response.optString("longToken"),
                    secretHex,
                    profile.username,
                    response.optString("machineId"),
                    response.optString("uid"));
            if (!session.hasToken()) {
                throw new FnosRpcException("登录成功响应缺少 token 或 secret");
            }
            return session;
        } finally {
            close();
        }
    }

    public boolean authToken(FnosSession session) throws FnosRpcException {
        if (session == null || !session.hasToken()) {
            return false;
        }
        connect();
        try {
            loadPublicKey();
            JSONObject request = new JSONObject();
            request.put("req", "user.authToken");
            request.put("token", session.token);
            request.put("main", true);
            request.put("si", si);
            send(signRequest(request, session.secretHex), REQUEST_TIMEOUT_SECONDS);
            return true;
        } catch (JSONException ex) {
            throw new FnosRpcException("构造 token 验证请求失败", ex);
        } finally {
            close();
        }
    }

    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "done");
            webSocket = null;
        }
        httpClient.dispatcher().executorService().shutdown();
    }

    private void awaitOpen() throws FnosRpcException {
        try {
            if (!openLatch.await(OPEN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new FnosRpcException("连接 fnOS RPC 超时");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FnosRpcException("连接 fnOS RPC 被中断", ex);
        }
        if (openError != null) {
            throw openError;
        }
    }

    private void loadPublicKey() throws FnosRpcException {
        try {
            JSONObject request = new JSONObject();
            request.put("req", "util.crypto.getRSAPub");
            JSONObject response = send(request, REQUEST_TIMEOUT_SECONDS);
            publicKey = response.optString("pub");
            si = response.optString("si");
            if (publicKey.length() == 0 || si.length() == 0) {
                throw new FnosRpcException("fnOS RPC 握手缺少公钥或会话标识");
            }
        } catch (JSONException ex) {
            throw new FnosRpcException("构造握手请求失败", ex);
        }
    }

    private JSONObject loginPayload() throws FnosRpcException {
        try {
            JSONObject request = new JSONObject();
            request.put("req", "user.login");
            request.put("user", profile.username);
            request.put("password", profile.password);
            request.put("stay", false);
            request.put("deviceType", "TV");
            request.put("deviceName", "Android-TV");
            request.put("did", deviceId);
            request.put("si", si);
            return request;
        } catch (JSONException ex) {
            throw new FnosRpcException("构造登录请求失败", ex);
        }
    }

    private JSONObject encryptForLogin(JSONObject payload) throws FnosRpcException {
        try {
            loginAesKey = FnosCrypto.randomAscii(32).getBytes("UTF-8");
            loginIv = FnosCrypto.randomBytes(16);
            JSONObject encrypted = new JSONObject();
            encrypted.put("req", "encrypted");
            encrypted.put("iv", FnosCrypto.toHex(loginIv));
            encrypted.put("rsa", FnosCrypto.rsaEncryptBase64(publicKey, new String(loginAesKey, "UTF-8")));
            encrypted.put("aes", FnosCrypto.aesEncryptHex(payload.toString(), loginAesKey, loginIv));
            return encrypted;
        } catch (Exception ex) {
            if (ex instanceof FnosRpcException) {
                throw (FnosRpcException) ex;
            }
            throw new FnosRpcException("构造加密登录请求失败", ex);
        }
    }

    private SignedRequest signRequest(JSONObject request, String secretHex) throws FnosRpcException {
        if (request.optString("reqid").length() == 0) {
            try {
                request.put("reqid", nextReqId());
            } catch (JSONException ex) {
                throw new FnosRpcException("构造请求编号失败", ex);
            }
        }
        String body = request.toString();
        return new SignedRequest(request.optString("reqid"), FnosCrypto.hmacSha256Hex(body, secretHex) + body);
    }

    private JSONObject send(JSONObject request, int timeoutSeconds) throws FnosRpcException {
        String reqId = request.optString("reqid");
        if (reqId.length() == 0) {
            reqId = nextReqId();
            try {
                request.put("reqid", reqId);
            } catch (JSONException ex) {
                throw new FnosRpcException("构造请求编号失败", ex);
            }
        }
        PendingCall call = new PendingCall();
        synchronized (pendingCalls) {
            pendingCalls.put(reqId, call);
        }
        webSocket.send(request.toString());
        return call.await(reqId, timeoutSeconds);
    }

    private JSONObject send(SignedRequest request, int timeoutSeconds) throws FnosRpcException {
        String reqId = request.reqId;
        PendingCall call = new PendingCall();
        synchronized (pendingCalls) {
            pendingCalls.put(reqId, call);
        }
        webSocket.send(request.signedPayload);
        return call.await(reqId, timeoutSeconds);
    }

    private String nextReqId() {
        long seconds = System.currentTimeMillis() / 1000L;
        requestId = (requestId + 1) & 0xFFFF;
        return leftPad(Long.toHexString(seconds), 8) + backId + leftPad(Integer.toHexString(requestId), 4);
    }

    private String leftPad(String value, int length) {
        StringBuilder builder = new StringBuilder(value);
        while (builder.length() < length) {
            builder.insert(0, '0');
        }
        return builder.toString();
    }

    private String webSocketUrl(String baseUrl) {
        String url = baseUrl;
        if (url.startsWith("https://")) {
            url = "wss://" + url.substring("https://".length());
        } else if (url.startsWith("http://")) {
            url = "ws://" + url.substring("http://".length());
        }
        return url + "/websocket?type=main";
    }

    private final class Listener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            openLatch.countDown();
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            try {
                JSONObject message = new JSONObject(text);
                if ("pong".equals(message.optString("res"))) {
                    return;
                }
                if (message.has("backId")) {
                    backId = message.optString("backId", backId);
                }
                String reqId = message.optString("reqid");
                if (reqId.length() == 0) {
                    return;
                }
                PendingCall call;
                synchronized (pendingCalls) {
                    call = pendingCalls.remove(reqId);
                }
                if (call != null) {
                    call.complete(message);
                }
            } catch (JSONException ex) {
                Logger.w("RPC message parse failed: " + ex.getMessage());
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable throwable, Response response) {
            String message = throwable == null ? "未知网络错误" : throwable.getMessage();
            openError = new FnosRpcException("fnOS RPC 连接失败：" + message, throwable);
            openLatch.countDown();
            synchronized (pendingCalls) {
                Iterator<Map.Entry<String, PendingCall>> iterator = pendingCalls.entrySet().iterator();
                while (iterator.hasNext()) {
                    iterator.next().getValue().fail(openError);
                    iterator.remove();
                }
            }
        }
    }

    private static final class PendingCall {
        private final CountDownLatch latch = new CountDownLatch(1);
        private JSONObject response;
        private FnosRpcException error;

        void complete(JSONObject value) {
            response = value;
            latch.countDown();
        }

        void fail(FnosRpcException value) {
            error = value;
            latch.countDown();
        }

        JSONObject await(String reqId, int timeoutSeconds) throws FnosRpcException {
            try {
                if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
                    throw new FnosRpcException("RPC 请求超时：" + reqId);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new FnosRpcException("RPC 请求被中断：" + reqId, ex);
            }
            if (error != null) {
                throw error;
            }
            if (response == null) {
                throw new FnosRpcException("RPC 响应为空：" + reqId);
            }
            if (!"success".equals(response.optString("result"))) {
                String message = response.optString("errmsg");
                if (message.length() == 0) {
                    message = "RPC 请求失败：" + response.optString("errno", response.toString());
                }
                throw new FnosRpcException(message);
            }
            return response;
        }
    }

    private static final class SignedRequest {
        final String reqId;
        final String signedPayload;

        SignedRequest(String reqId, String signedPayload) {
            this.reqId = reqId;
            this.signedPayload = signedPayload;
        }
    }
}
