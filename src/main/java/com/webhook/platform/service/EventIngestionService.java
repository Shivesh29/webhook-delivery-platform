package com.webhook.platform.service;

import com.webhook.platform.dto.EventIngestRequest;
import com.webhook.platform.dto.EventIngestResponse;
import com.webhook.platform.model.Delivery;
import com.webhook.platform.model.DeliveryStatus;
import com.webhook.platform.model.Endpoint;
import com.webhook.platform.model.WebhookEvent;
import com.webhook.platform.repository.DeliveryRepository;
import com.webhook.platform.repository.EndpointRepository;
import com.webhook.platform.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventIngestionService {

    private final WebhookEventRepository eventRepository;
    private final EndpointRepository endpointRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryDispatcher deliveryDispatcher;

    @Value("${webhook.delivery.max-attempts:5}")
    private int defaultMaxAttempts;

    @Transactional
    public EventIngestResponse ingestEvent(String idempotencyKey, EventIngestRequest request) {
        // 1. Idempotency Check
        Optional<WebhookEvent> existing = eventRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            WebhookEvent event = existing.get();
            log.info("Idempotent event duplicate detected: key={}, eventId={}", idempotencyKey, event.getId());
            List<Delivery> existingDeliveries = deliveryRepository.findByEventId(event.getId());
            List<String> deliveryIds = existingDeliveries.stream().map(Delivery::getId).toList();

            return EventIngestResponse.builder()
                    .eventId(event.getId())
                    .eventType(event.getEventType())
                    .idempotencyKey(event.getIdempotencyKey())
                    .duplicate(true)
                    .matchingEndpointsCount(existingDeliveries.size())
                    .deliveryIds(deliveryIds)
                    .createdAt(event.getCreatedAt())
                    .build();
        }

        // 2. Persist Event
        String eventId = UUID.randomUUID().toString();
        WebhookEvent event = WebhookEvent.builder()
                .id(eventId)
                .eventType(request.getEventType())
                .idempotencyKey(idempotencyKey)
                .payload(request.getPayload())
                .createdAt(Instant.now())
                .build();
        eventRepository.save(event);

        // 3. Match active endpoints
        List<Endpoint> activeEndpoints = endpointRepository.findByEnabledTrue();
        List<Endpoint> matchingEndpoints = activeEndpoints.stream()
                .filter(ep -> ep.matchesEvent(request.getEventType()))
                .toList();

        List<String> createdDeliveryIds = new ArrayList<>();
        List<Delivery> deliveriesToCreate = new ArrayList<>();

        for (Endpoint ep : matchingEndpoints) {
            String deliveryId = UUID.randomUUID().toString();
            Delivery delivery = Delivery.builder()
                    .id(deliveryId)
                    .event(event)
                    .endpoint(ep)
                    .status(DeliveryStatus.PENDING)
                    .attemptCount(0)
                    .maxAttempts(defaultMaxAttempts)
                    .nextRetryAt(Instant.now()) // eligible immediately
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            deliveriesToCreate.add(delivery);
            createdDeliveryIds.add(deliveryId);
        }

        deliveryRepository.saveAll(deliveriesToCreate);
        log.info("Ingested event id={}, type={}, created {} deliveries",
                eventId, request.getEventType(), deliveriesToCreate.size());

        // 4. Trigger immediate async dispatch for each delivery
        for (Delivery d : deliveriesToCreate) {
            try {
                deliveryDispatcher.dispatchAsync(d.getId());
            } catch (Exception e) {
                log.error("Failed to trigger immediate dispatch for delivery {}: {}", d.getId(), e.getMessage());
            }
        }

        return EventIngestResponse.builder()
                .eventId(event.getId())
                .eventType(event.getEventType())
                .idempotencyKey(idempotencyKey)
                .duplicate(false)
                .matchingEndpointsCount(deliveriesToCreate.size())
                .deliveryIds(createdDeliveryIds)
                .createdAt(event.getCreatedAt())
                .build();
    }
}

