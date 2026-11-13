package com.webhook.platform.dto;

import com.webhook.platform.model.Delivery;
import com.webhook.platform.model.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryResponse {

    private String id;
    private String eventId;
    private String eventType;
    private String endpointId;
    private String endpointUrl;
    private DeliveryStatus status;
    private int attemptCount;
    private int maxAttempts;
    private Instant nextRetryAt;
    private Instant lastAttemptAt;
    private Instant deliveredAt;
    private Instant createdAt;

    public static DeliveryResponse fromEntity(Delivery delivery) {
        return DeliveryResponse.builder()
                .id(delivery.getId())
                .eventId(delivery.getEvent().getId())
                .eventType(delivery.getEvent().getEventType())
                .endpointId(delivery.getEndpoint().getId())
                .endpointUrl(delivery.getEndpoint().getUrl())
                .status(delivery.getStatus())
                .attemptCount(delivery.getAttemptCount())
                .maxAttempts(delivery.getMaxAttempts())
                .nextRetryAt(delivery.getNextRetryAt())
                .lastAttemptAt(delivery.getLastAttemptAt())
                .deliveredAt(delivery.getDeliveredAt())
                .createdAt(delivery.getCreatedAt())
                .build();
    }
}

