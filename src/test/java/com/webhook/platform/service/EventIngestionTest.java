package com.webhook.platform.service;

import com.webhook.platform.dto.EventIngestRequest;
import com.webhook.platform.dto.EventIngestResponse;
import com.webhook.platform.model.Endpoint;
import com.webhook.platform.repository.DeliveryRepository;
import com.webhook.platform.repository.EndpointRepository;
import com.webhook.platform.repository.WebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class EventIngestionTest {

    @Autowired
    private EventIngestionService eventIngestionService;

    @Autowired
    private EndpointRepository endpointRepository;

    @Autowired
    private WebhookEventRepository eventRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @MockBean
    private DeliveryDispatcher deliveryDispatcher;

    @BeforeEach
    void setUp() {
        deliveryRepository.deleteAll();
        eventRepository.deleteAll();
        endpointRepository.deleteAll();

        // Register a test subscriber endpoint
        Endpoint endpoint = Endpoint.builder()
                .id(UUID.randomUUID().toString())
                .url("https://example.com/webhook")
                .secret("whsec_test123")
                .subscribedEvents("order.*,payment.completed")
                .enabled(true)
                .build();
        endpointRepository.save(endpoint);
    }

    @Test
    void shouldIngestEventAndCreateDelivery() {
        String idempotencyKey = "key_" + UUID.randomUUID();
        EventIngestRequest request = EventIngestRequest.builder()
                .eventType("order.created")
                .payload("{\"order_id\":\"ord_999\",\"amount\":120}")
                .build();

        EventIngestResponse response = eventIngestionService.ingestEvent(idempotencyKey, request);

        assertNotNull(response);
        assertFalse(response.isDuplicate());
        assertEquals(1, response.getMatchingEndpointsCount());
        assertEquals(1, response.getDeliveryIds().size());
    }

    @Test
    void shouldDetectDuplicateIdempotencyKey() {
        String idempotencyKey = "key_dup_" + UUID.randomUUID();
        EventIngestRequest request = EventIngestRequest.builder()
                .eventType("payment.completed")
                .payload("{\"payment_id\":\"pay_123\"}")
                .build();

        EventIngestResponse first = eventIngestionService.ingestEvent(idempotencyKey, request);
        assertFalse(first.isDuplicate());

        EventIngestResponse second = eventIngestionService.ingestEvent(idempotencyKey, request);
        assertTrue(second.isDuplicate());
        assertEquals(first.getEventId(), second.getEventId());
        assertEquals(first.getDeliveryIds(), second.getDeliveryIds());
    }
}

