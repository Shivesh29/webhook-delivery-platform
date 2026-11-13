package com.webhook.platform.controller;

import com.webhook.platform.dto.DeliveryAttemptResponse;
import com.webhook.platform.dto.DeliveryResponse;
import com.webhook.platform.model.DeliveryStatus;
import com.webhook.platform.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
@Tag(name = "Deliveries", description = "Monitor delivery statuses, view audit logs, and replay dead-letter webhooks")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/stats")
    @Operation(summary = "Get platform metrics & delivery statistics")
    public ResponseEntity<com.webhook.platform.dto.DeliveryStatsResponse> getStats() {
        return ResponseEntity.ok(deliveryService.getStats());
    }

    @GetMapping
    @Operation(summary = "Query deliveries", description = "Filter by status, eventId, or endpointId")
    public ResponseEntity<List<DeliveryResponse>> listDeliveries(
            @RequestParam(required = false) DeliveryStatus status,
            @RequestParam(required = false) String eventId,
            @RequestParam(required = false) String endpointId) {
        return ResponseEntity.ok(deliveryService.listDeliveries(status, eventId, endpointId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get delivery status by ID")
    public ResponseEntity<DeliveryResponse> getDelivery(@PathVariable String id) {
        return ResponseEntity.ok(deliveryService.getDelivery(id));
    }

    @GetMapping("/{id}/attempts")
    @Operation(summary = "View delivery audit trail", description = "Inspect every individual HTTP attempt, latency, status code, and errors")
    public ResponseEntity<List<DeliveryAttemptResponse>> getAttempts(@PathVariable String id) {
        return ResponseEntity.ok(deliveryService.getAttempts(id));
    }

    @PostMapping("/{id}/replay")
    @Operation(summary = "Manual Dead-Letter Queue (DLQ) replay", description = "Re-triggers an immediate asynchronous delivery attempt for a failed or dead-lettered webhook")
    public ResponseEntity<DeliveryResponse> replayDelivery(@PathVariable String id) {
        return ResponseEntity.ok(deliveryService.replayDelivery(id));
    }
}

