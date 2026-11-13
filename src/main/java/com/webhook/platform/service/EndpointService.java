package com.webhook.platform.service;

import com.webhook.platform.common.ApiException;
import com.webhook.platform.dto.EndpointCreateRequest;
import com.webhook.platform.dto.EndpointResponse;
import com.webhook.platform.model.Endpoint;
import com.webhook.platform.repository.EndpointRepository;
import com.webhook.platform.security.SecretGenerator;
import com.webhook.platform.security.SsrfValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EndpointService {

    private final EndpointRepository endpointRepository;
    private final SecretGenerator secretGenerator;
    private final SsrfValidator ssrfValidator;

    @Transactional
    public EndpointResponse createEndpoint(EndpointCreateRequest request) {
        ssrfValidator.validateUrl(request.getUrl());

        String secret = secretGenerator.generateSecret();
        String id = UUID.randomUUID().toString();

        Endpoint endpoint = Endpoint.builder()
                .id(id)
                .url(request.getUrl())
                .description(request.getDescription())
                .secret(secret)
                .subscribedEvents(request.getSubscribedEvents() != null && !request.getSubscribedEvents().isBlank()
                        ? request.getSubscribedEvents()
                        : "*")
                .enabled(true)
                .build();

        Endpoint saved = endpointRepository.save(endpoint);
        log.info("Registered new webhook endpoint: id={}, url={}", saved.getId(), saved.getUrl());
        return EndpointResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<EndpointResponse> listEndpoints() {
        return endpointRepository.findAll().stream()
                .map(EndpointResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public EndpointResponse getEndpoint(String id) {
        Endpoint endpoint = endpointRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Endpoint not found with id: " + id));
        return EndpointResponse.fromEntity(endpoint);
    }

    @Transactional
    public EndpointResponse toggleEndpoint(String id, boolean enabled) {
        Endpoint endpoint = endpointRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Endpoint not found with id: " + id));
        endpoint.setEnabled(enabled);
        Endpoint saved = endpointRepository.save(endpoint);
        log.info("Toggled endpoint status: id={}, enabled={}", id, enabled);
        return EndpointResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteEndpoint(String id) {
        if (!endpointRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Endpoint not found with id: " + id);
        }
        endpointRepository.deleteById(id);
        log.info("Deleted endpoint: id={}", id);
    }
}

