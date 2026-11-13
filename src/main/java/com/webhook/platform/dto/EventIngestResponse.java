package com.webhook.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventIngestResponse {

    private String eventId;
    private String eventType;
    private String idempotencyKey;
    private boolean duplicate;
    private int matchingEndpointsCount;
    private List<String> deliveryIds;
    private Instant createdAt;
}

