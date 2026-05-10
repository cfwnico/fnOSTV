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

    static String aesEncryptHex(String value, byte[] key, byte[] iv) throws FnosRpcException {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            return toHex(cipher.doFinal(value.getBytes("UTF-8")));
        } catch (Exception ex) {
            throw new FnosRpcException("AES 加密失败", ex);
        }
    }

    static String aesDecryptUtf8(String hexValue, byte[] key, byte[] iv) throws FnosRpcException {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            return new String(cipher.doFinal(fromHex(hexValue)), "UTF-8");
        } catch (Exception ex) {
            throw new FnosRpcException("AES 解密失败", ex);
        }
    }

    static String hmacSha256Hex(String value, String keyHex) throws FnosRpcException {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(fromHex(keyHex), "HmacSHA256"));
            return toHex(mac.doFinal(value.getBytes("UTF-8")));
        } catch (Exception ex) {
            throw new FnosRpcException("请求签名失败", ex);
        }
    }

    static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            if (value < 16) {
                builder.append('0');
            }
            builder.append(Integer.toHexString(value));
        }
        return builder.toString();
    }

    static byte[] fromHex(String hex) {
        int length = hex.length();
        byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
