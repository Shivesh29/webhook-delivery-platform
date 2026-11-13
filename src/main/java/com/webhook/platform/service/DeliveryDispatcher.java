package com.webhook.platform.service;

import com.webhook.platform.model.Delivery;
import com.webhook.platform.model.DeliveryAttempt;
import com.webhook.platform.model.DeliveryStatus;
import com.webhook.platform.model.Endpoint;
import com.webhook.platform.repository.DeliveryAttemptRepository;
import com.webhook.platform.repository.DeliveryRepository;
import com.webhook.platform.security.HmacSigner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryDispatcher {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final HmacSigner hmacSigner;

    @Value("${webhook.delivery.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    @Value("${webhook.delivery.read-timeout-ms:5000}")
    private int readTimeoutMs;

    @Value("${webhook.delivery.backoff-multipliers-seconds:60,300,1800,7200}")
    private List<Integer> backoffMultipliersSeconds;

    private final Random random = new Random();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(3000))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Async
    @Transactional
    public void dispatchAsync(String deliveryId) {
        dispatch(deliveryId);
    }

    @Transactional
    public void dispatch(String deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElse(null);
        if (delivery == null) {
            log.warn("Delivery not found for id: {}", deliveryId);
            return;
        }

        // Avoid re-processing already delivered items
        if (delivery.getStatus() == DeliveryStatus.SUCCESS) {
            return;
        }

        Endpoint endpoint = delivery.getEndpoint();
        if (!endpoint.isEnabled()) {
            log.info("Skipping delivery {} because endpoint {} is disabled", delivery.getId(), endpoint.getId());
            return;
        }

        int attemptNumber = delivery.getAttemptCount() + 1;
        long timestamp = Instant.now().getEpochSecond();
        String payload = delivery.getEvent().getPayload();
        String signatureHeader = hmacSigner.buildSignatureHeader(endpoint.getSecret(), timestamp, payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint.getUrl()))
                .timeout(Duration.ofMillis(readTimeoutMs))
                .header("Content-Type", "application/json")
                .header("User-Agent", "Webhook-Delivery-Platform/1.0")
                .header("X-Webhook-Id", delivery.getId())
                .header("X-Webhook-Event", delivery.getEvent().getEventType())
                .header("X-Webhook-Timestamp", String.valueOf(timestamp))
                .header("X-Webhook-Signature", signatureHeader)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        long startTime = System.currentTimeMillis();
        Integer statusCode = null;
        String responseBody = null;
        String errorMessage = null;

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            statusCode = response.statusCode();
            responseBody = response.body();
            if (responseBody != null && responseBody.length() > 2000) {
                responseBody = responseBody.substring(0, 2000) + "... [truncated]";
            }
        } catch (HttpTimeoutException e) {
            errorMessage = "Request timed out after " + readTimeoutMs + "ms";
        } catch (IOException e) {
            errorMessage = "I/O error connecting to receiver: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorMessage = "Delivery attempt interrupted";
        } catch (Exception e) {
            errorMessage = "Unexpected error: " + e.getMessage();
        }

        long durationMs = System.currentTimeMillis() - startTime;
        Instant now = Instant.now();

        // Record attempt
        DeliveryAttempt attempt = DeliveryAttempt.builder()
                .id(UUID.randomUUID().toString())
                .delivery(delivery)
                .attemptNumber(attemptNumber)
                .responseStatusCode(statusCode)
                .responseBody(responseBody)
                .durationMs(durationMs)
                .errorMessage(errorMessage)
                .attemptedAt(now)
                .build();
        deliveryAttemptRepository.save(attempt);

        delivery.setAttemptCount(attemptNumber);
        delivery.setLastAttemptAt(now);

        boolean isSuccess = statusCode != null && statusCode >= 200 && statusCode < 300;

        if (isSuccess) {
            delivery.setStatus(DeliveryStatus.SUCCESS);
            delivery.setDeliveredAt(now);
            delivery.setNextRetryAt(null);
            log.info("Delivery {} successfully delivered to {} on attempt {} (status={}, duration={}ms)",
                    delivery.getId(), endpoint.getUrl(), attemptNumber, statusCode, durationMs);
        } else {
            if (attemptNumber >= delivery.getMaxAttempts()) {
                delivery.setStatus(DeliveryStatus.DEAD_LETTER);
                delivery.setNextRetryAt(null);
                log.warn("Delivery {} reached max attempts ({}) and moved to DEAD_LETTER. Last error: {}, HTTP: {}",
                        delivery.getId(), delivery.getMaxAttempts(), errorMessage, statusCode);
            } else {
                delivery.setStatus(DeliveryStatus.RETRYING);
                long delaySeconds = computeBackoffSeconds(attemptNumber);
                Instant nextRetry = now.plusSeconds(delaySeconds);
                delivery.setNextRetryAt(nextRetry);
                log.info("Delivery {} failed (HTTP: {}, err: {}). Scheduled retry #{} at {}",
                        delivery.getId(), statusCode, errorMessage, attemptNumber + 1, nextRetry);
            }
        }

        deliveryRepository.save(delivery);
    }

    public long computeBackoffSeconds(int attemptNumber) {
        int index = Math.min(attemptNumber - 1, backoffMultipliersSeconds.size() - 1);
        int baseSeconds = backoffMultipliersSeconds.get(Math.max(0, index));
        // Add full jitter: random value between [0.8 * base, 1.2 * base]
        double jitterMultiplier = 0.8 + (random.nextDouble() * 0.4);
        return Math.max(1, (long) (baseSeconds * jitterMultiplier));
    }
}

