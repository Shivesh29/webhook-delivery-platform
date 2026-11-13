package com.webhook.platform.controller;

import com.webhook.platform.dto.EventIngestRequest;
import com.webhook.platform.dto.EventIngestResponse;
import com.webhook.platform.service.EventIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Ingest events with idempotency deduplication and trigger deliveries")
public class EventController {

    private final EventIngestionService eventIngestionService;

    @PostMapping
    @Operation(summary = "Publish an event for webhook distribution",
            description = "Requires an Idempotency-Key header. Duplicate keys return cached response without re-triggering deliveries.")
    public ResponseEntity<EventIngestResponse> ingestEvent(
            @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Unique deduplication key for this event")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody EventIngestRequest request) {

        EventIngestResponse response = eventIngestionService.ingestEvent(idempotencyKey, request);
        HttpStatus status = response.isDuplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return new ResponseEntity<>(response, status);
    }
}

