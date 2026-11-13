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
public class EventIngestRequest {

    @NotBlank(message = "event_type is required")
    private String eventType;

    // JSON payload string or structured JSON node
    @NotBlank(message = "payload is required")
    private String payload;
}

