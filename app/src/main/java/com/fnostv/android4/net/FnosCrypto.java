package com.fnostv.android4.net;

import android.util.Base64;

import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

final class FnosCrypto {
    private static final String RANDOM_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private FnosCrypto() {
    }

    static String randomAscii(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(RANDOM_CHARS.charAt(RANDOM.nextInt(RANDOM_CHARS.length())));
        }
        return builder.toString();
    }

    static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    static String rsaEncryptBase64(String publicKeyPem, String value) throws FnosRpcException {
        try {
            String normalized = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                    .replace("-----END RSA PUBLIC KEY-----", "")
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace(" ", "");
            byte[] keyBytes = Base64.decode(normalized, Base64.DEFAULT);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return Base64.encodeToString(cipher.doFinal(value.getBytes("UTF-8")), Base64.NO_WRAP);
        } catch (Exception ex) {
            throw new FnosRpcException("RSA 加密失败", ex);
        }
    }

    static String aesEncryptBase64(String value, byte[] key, byte[] iv) throws FnosRpcException {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            return Base64.encodeToString(cipher.doFinal(value.getBytes("UTF-8")), Base64.NO_WRAP);
        } catch (Exception ex) {
            throw new FnosRpcException("AES 加密失败", ex);
        }
    }

    static String aesDecryptBase64ToBase64(String base64Value, byte[] key, byte[] iv) throws FnosRpcException {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(Base64.decode(base64Value, Base64.DEFAULT));
            return Base64.encodeToString(decrypted, Base64.NO_WRAP);
        } catch (Exception ex) {
            throw new FnosRpcException("AES 解密失败", ex);
        }
    }

    static String hmacSha256Base64(String value, String keyBase64) throws FnosRpcException {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Base64.decode(keyBase64, Base64.DEFAULT), "HmacSHA256"));
            return Base64.encodeToString(mac.doFinal(value.getBytes("UTF-8")), Base64.NO_WRAP);
        } catch (Exception ex) {
            throw new FnosRpcException("请求签名失败", ex);
        }
    }

    static String md5Hex(String value) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(value.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    static String generateAuthx(String method, String urlPath, String body, String token) {
        String secret = "NDzZTVxnRKP8Z0jXg1VAMonaG8akvh";
        String s = String.valueOf((long)(Math.random() * 900000) + 100000);
        String c = String.valueOf(System.currentTimeMillis());
        
        boolean isGet = "GET".equalsIgnoreCase(method);
        String a = "";
        if (!isGet && body != null && body.length() > 0) {
            a = body;
        }
        
        String o = a.length() == 0 ? "" : md5Hex(a);
        String t = token == null ? "" : token;
        
        // Ensure path parsing logic matches Web UI
        // Typical pu(e.url) returns the relative path, let's assume it passes the correct urlPath
        String l = secret + "_" + urlPath + "_" + s + "_" + c + "_" + o + "_" + t;
        
        return "nonce=" + s + "&timestamp=" + c + "&sign=" + md5Hex(l);
    }
}
