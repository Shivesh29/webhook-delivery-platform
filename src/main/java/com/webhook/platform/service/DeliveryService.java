package com.webhook.platform.service;

import com.webhook.platform.common.ApiException;
import com.webhook.platform.dto.DeliveryAttemptResponse;
import com.webhook.platform.dto.DeliveryResponse;
import com.webhook.platform.model.Delivery;
import com.webhook.platform.model.DeliveryStatus;
import com.webhook.platform.repository.DeliveryAttemptRepository;
import com.webhook.platform.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryAttemptRepository attemptRepository;
    private final DeliveryDispatcher deliveryDispatcher;

    @Transactional(readOnly = true)
    public List<DeliveryResponse> listDeliveries(DeliveryStatus status, String eventId, String endpointId) {
        List<Delivery> list;
        if (eventId != null && !eventId.isBlank()) {
            list = deliveryRepository.findByEventId(eventId);
        } else if (endpointId != null && !endpointId.isBlank()) {
            list = deliveryRepository.findByEndpointId(endpointId);
        } else if (status != null) {
            list = deliveryRepository.findByStatus(status);
        } else {
            list = deliveryRepository.findAll();
        }

        return list.stream().map(DeliveryResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getDelivery(String id) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Delivery not found with id: " + id));
        return DeliveryResponse.fromEntity(delivery);
    }

    @Transactional(readOnly = true)
    public List<DeliveryAttemptResponse> getAttempts(String deliveryId) {
        if (!deliveryRepository.existsById(deliveryId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Delivery not found with id: " + deliveryId);
        }
        return attemptRepository.findByDeliveryIdOrderByAttemptNumberAsc(deliveryId).stream()
                .map(DeliveryAttemptResponse::fromEntity)
                .toList();
    }

    @Transactional
    public DeliveryResponse replayDelivery(String deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Delivery not found with id: " + deliveryId));

        log.info("Manual replay requested for delivery id={}, previous status={}", deliveryId, delivery.getStatus());

        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setNextRetryAt(Instant.now());
        deliveryRepository.save(delivery);

        // Dispatch async
        deliveryDispatcher.dispatchAsync(delivery.getId());

        return DeliveryResponse.fromEntity(delivery);
    }
}

