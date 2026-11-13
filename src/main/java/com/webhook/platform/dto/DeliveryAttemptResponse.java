package com.webhook.platform.dto;

import com.webhook.platform.model.DeliveryAttempt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAttemptResponse {

    private String id;
    private String deliveryId;
    private int attemptNumber;
    private Integer responseStatusCode;
    private String responseBody;
    private long durationMs;
    private String errorMessage;
    private Instant attemptedAt;

    public static DeliveryAttemptResponse fromEntity(DeliveryAttempt attempt) {
        return DeliveryAttemptResponse.builder()
                .id(attempt.getId())
                .deliveryId(attempt.getDelivery().getId())
                .attemptNumber(attempt.getAttemptNumber())
                .responseStatusCode(attempt.getResponseStatusCode())
                .responseBody(attempt.getResponseBody())
                .durationMs(attempt.getDurationMs())
                .errorMessage(attempt.getErrorMessage())
                .attemptedAt(attempt.getAttemptedAt())
                .build();
    }
}

