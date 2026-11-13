package com.webhook.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryStatsResponse {

    private long totalDeliveries;
    private long successCount;
    private long retryingCount;
    private long deadLetterCount;
    private long pendingCount;
    private double successRatePercent;
    private long activeEndpointsCount;
}

