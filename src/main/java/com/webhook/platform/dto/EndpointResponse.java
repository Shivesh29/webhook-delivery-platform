package com.webhook.platform.dto;

import com.webhook.platform.model.Endpoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointResponse {

    private String id;
    private String url;
    private String description;
    private String secret;
    private String subscribedEvents;
    private boolean enabled;
    private Instant createdAt;

    public static EndpointResponse fromEntity(Endpoint endpoint) {
        return EndpointResponse.builder()
                .id(endpoint.getId())
                .url(endpoint.getUrl())
                .description(endpoint.getDescription())
                .secret(endpoint.getSecret())
                .subscribedEvents(endpoint.getSubscribedEvents())
                .enabled(endpoint.isEnabled())
                .createdAt(endpoint.getCreatedAt())
                .build();
    }
}

