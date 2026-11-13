package com.webhook.platform.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HmacSignerTest {

    private HmacSigner hmacSigner;

    @BeforeEach
    void setUp() {
        hmacSigner = new HmacSigner();
    }

    @Test
    void shouldComputeConsistentHmacSignature() {
        String secret = "whsec_test_secret_1234567890abcdef";
        long timestamp = 1700000000L;
        String payload = "{\"orderId\":\"101\",\"amount\":49.99}";

        String sig1 = hmacSigner.computeSignature(secret, timestamp, payload);
        String sig2 = hmacSigner.computeSignature(secret, timestamp, payload);

        assertNotNull(sig1);
        assertEquals(64, sig1.length()); // SHA-256 produces 64 hex characters
        assertEquals(sig1, sig2);
    }

    @Test
    void shouldProduceDifferentSignatureForDifferentPayloadOrTimestamp() {
        String secret = "whsec_test_secret_1234567890abcdef";
        String payload1 = "{\"orderId\":\"101\"}";
        String payload2 = "{\"orderId\":\"102\"}";

        String sig1 = hmacSigner.computeSignature(secret, 1700000000L, payload1);
        String sig2 = hmacSigner.computeSignature(secret, 1700000000L, payload2);
        String sig3 = hmacSigner.computeSignature(secret, 1700000001L, payload1);

        assertNotEquals(sig1, sig2);
        assertNotEquals(sig1, sig3);
    }

    @Test
    void shouldFormatHeaderCorrectly() {
        String secret = "whsec_test_secret_1234567890abcdef";
        long timestamp = 1700000000L;
        String payload = "{\"test\":true}";

        String header = hmacSigner.buildSignatureHeader(secret, timestamp, payload);

        assertTrue(header.startsWith("t=1700000000,v1="));
        assertEquals(16 + 64, header.length()); // "t=1700000000,v1=" is 16 chars + 64 hex chars
    }
}

