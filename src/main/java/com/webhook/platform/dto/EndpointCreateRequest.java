package com.webhook.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointCreateRequest {

    @NotBlank(message = "Endpoint URL is required")
    private String url;

    private String description;

    // Optional: comma-separated list of event types, defaults to "*"
    private String subscribedEvents;
}

