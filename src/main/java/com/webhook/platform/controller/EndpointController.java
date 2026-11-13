package com.webhook.platform.controller;

import com.webhook.platform.dto.EndpointCreateRequest;
import com.webhook.platform.dto.EndpointResponse;
import com.webhook.platform.service.EndpointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/endpoints")
@RequiredArgsConstructor
@Tag(name = "Endpoints", description = "Register and manage webhook subscriber endpoints")
public class EndpointController {

    private final EndpointService endpointService;

    @PostMapping
    @Operation(summary = "Register a new webhook endpoint", description = "Validates URL for SSRF protection and auto-generates HMAC signing secret")
    public ResponseEntity<EndpointResponse> createEndpoint(@Valid @RequestBody EndpointCreateRequest request) {
        EndpointResponse response = endpointService.createEndpoint(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List all webhook endpoints")
    public ResponseEntity<List<EndpointResponse>> listEndpoints() {
        return ResponseEntity.ok(endpointService.listEndpoints());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get endpoint details by ID")
    public ResponseEntity<EndpointResponse> getEndpoint(@PathVariable String id) {
        return ResponseEntity.ok(endpointService.getEndpoint(id));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Enable or disable an endpoint")
    public ResponseEntity<EndpointResponse> toggleEndpoint(@PathVariable String id, @RequestParam boolean enabled) {
        return ResponseEntity.ok(endpointService.toggleEndpoint(id, enabled));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an endpoint")
    public ResponseEntity<Void> deleteEndpoint(@PathVariable String id) {
        endpointService.deleteEndpoint(id);
        return ResponseEntity.noContent().build();
    }
}

