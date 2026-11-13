package com.webhook.platform.security;

import com.webhook.platform.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class SsrfValidatorTest {

    private SsrfValidator ssrfValidator;

    @BeforeEach
    void setUp() {
        ssrfValidator = new SsrfValidator();
        ReflectionTestUtils.setField(ssrfValidator, "allowLoopback", false);
    }

    @Test
    void shouldRejectBlankOrInvalidUrl() {
        assertThrows(ApiException.class, () -> ssrfValidator.validateUrl(""));
        assertThrows(ApiException.class, () -> ssrfValidator.validateUrl("not-a-valid-url"));
        assertThrows(ApiException.class, () -> ssrfValidator.validateUrl("ftp://example.com/webhook"));
    }

    @Test
    void shouldRejectLoopbackWhenDisallowed() {
        ApiException ex = assertThrows(ApiException.class, () ->
                ssrfValidator.validateUrl("http://127.0.0.1:8080/hook"));
        assertTrue(ex.getMessage().contains("Loopback destinations"));
    }

    @Test
    void shouldAllowValidPublicUrl() {
        assertDoesNotThrow(() -> ssrfValidator.validateUrl("https://httpbin.org/post"));
    }
}

