package com.webhook.platform.service;

import com.webhook.platform.model.Delivery;
import com.webhook.platform.model.DeliveryStatus;
import com.webhook.platform.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryScheduler {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryDispatcher deliveryDispatcher;

    @Value("${webhook.worker.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${webhook.worker.polling-interval-ms:5000}")
    public void pollAndDispatchDueDeliveries() {
        Instant now = Instant.now();
        List<DeliveryStatus> eligibleStatuses = List.of(DeliveryStatus.PENDING, DeliveryStatus.RETRYING);

        List<Delivery> dueDeliveries = deliveryRepository.findDueDeliveries(
                eligibleStatuses, now, PageRequest.of(0, batchSize));

        if (!dueDeliveries.isEmpty()) {
            log.debug("Found {} due webhook deliveries to process", dueDeliveries.size());
            for (Delivery delivery : dueDeliveries) {
                try {
                    deliveryDispatcher.dispatchAsync(delivery.getId());
                } catch (Exception e) {
                    log.error("Failed to enqueue delivery {}: {}", delivery.getId(), e.getMessage(), e);
                }
            }
        }
    }
}

