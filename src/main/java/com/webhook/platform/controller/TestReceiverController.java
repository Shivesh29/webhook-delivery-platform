package com.webhook.platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/v1/test-receiver")
@Slf4j
@Tag(name = "Test Receiver", description = "Built-in mock customer endpoints for testing deliveries, signatures, and retries")
public class TestReceiverController {

    private final List<Map<String, Object>> receivedRequests = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> flakyAttemptCounter = new ConcurrentHashMap<>();

    @PostMapping("/webhook")
    @Operation(summary = "Always returns 200 OK", description = "Inspects and records received webhook headers and payload")
    public ResponseEntity<Map<String, Object>> receiveWebhook(
            @RequestHeader(value = "X-Webhook-Id", required = false) String webhookId,
            @RequestHeader(value = "X-Webhook-Event", required = false) String webhookEvent,
            @RequestHeader(value = "X-Webhook-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestBody(required = false) String payload) {

        log.info("TestReceiver [/webhook] received webhook: id={}, event={}, timestamp={}, sig={}",
                webhookId, webhookEvent, timestamp, signature);

        Map<String, Object> record = new HashMap<>();
        record.put("type", "normal_200");
        record.put("webhookId", webhookId);
        record.put("event", webhookEvent);
        record.put("timestamp", timestamp);
        record.put("signature", signature);
        record.put("payload", payload);
        record.put("receivedAt", Instant.now().toString());

        receivedRequests.add(0, record);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "received");
        response.put("webhookId", webhookId);
        response.put("recordedAt", Instant.now().toString());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/flaky")
    @Operation(summary = "Simulates a flaky server", description = "Responds with 500 twice, then succeeds with 200 on the 3rd attempt to demonstrate retries")
    public ResponseEntity<Map<String, Object>> receiveFlaky(
            @RequestHeader(value = "X-Webhook-Id", required = false) String webhookId,
            @RequestHeader(value = "X-Webhook-Event", required = false) String webhookEvent,
            @RequestBody(required = false) String payload) {

        String key = webhookId != null ? webhookId : "anonymous";
        int currentAttempt = flakyAttemptCounter.merge(key, 1, Integer::sum);

        log.info("TestReceiver [/flaky] attempt #{} for delivery {}", currentAttempt, key);

        Map<String, Object> record = new HashMap<>();
        record.put("type", "flaky");
        record.put("webhookId", webhookId);
        record.put("attempt", currentAttempt);
        record.put("receivedAt", Instant.now().toString());
        receivedRequests.add(0, record);

        if (currentAttempt < 3) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Simulated downstream temporary outage");
            error.put("attempt", currentAttempt);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } else {
            Map<String, Object> success = new HashMap<>();
            success.put("status", "success_after_retries");
            success.put("totalAttempts", currentAttempt);
            return ResponseEntity.ok(success);
        }
    }

    @PostMapping("/always-fail")
    @Operation(summary = "Always returns 500 error", description = "Used to demonstrate transition to DEAD_LETTER after max retries")
    public ResponseEntity<Map<String, Object>> receiveAlwaysFail(
            @RequestHeader(value = "X-Webhook-Id", required = false) String webhookId,
            @RequestBody(required = false) String payload) {

        log.warn("TestReceiver [/always-fail] rejecting webhook: {}", webhookId);

        Map<String, Object> record = new HashMap<>();
        record.put("type", "always_fail_500");
        record.put("webhookId", webhookId);
        record.put("receivedAt", Instant.now().toString());
        receivedRequests.add(0, record);

        Map<String, Object> error = new HashMap<>();
        error.put("error", "Receiver permanently failing");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @GetMapping("/requests")
    @Operation(summary = "Inspect all captured requests received by the mock endpoints")
    public ResponseEntity<List<Map<String, Object>>> getCapturedRequests() {
        return ResponseEntity.ok(receivedRequests);
    }

    @DeleteMapping("/requests")
    @Operation(summary = "Clear captured requests history")
    public ResponseEntity<Void> clearCapturedRequests() {
        receivedRequests.clear();
        flakyAttemptCounter.clear();
        return ResponseEntity.noContent().build();
    }
}

