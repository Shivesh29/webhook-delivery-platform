package com.webhook.platform.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class HmacSigner {

    private static final String HMAC_SHA256 = "HmacSHA256";

    public String computeSignature(String secret, long timestamp, String payload) {
        String dataToSign = timestamp + "." + (payload != null ? payload : "");
        return sign(secret, dataToSign);
    }

    public String sign(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256 signature", e);
        }
    }

    public String buildSignatureHeader(String secret, long timestamp, String payload) {
        String signature = computeSignature(secret, timestamp, payload);
        return "t=" + timestamp + ",v1=" + signature;
    }
}

