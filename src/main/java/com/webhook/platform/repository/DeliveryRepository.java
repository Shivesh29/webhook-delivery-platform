package com.webhook.platform.repository;

import com.webhook.platform.model.Delivery;
import com.webhook.platform.model.DeliveryStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, String> {

    List<Delivery> findByEventId(String eventId);

    List<Delivery> findByEndpointId(String endpointId);

    List<Delivery> findByStatus(DeliveryStatus status);

    @Query("SELECT d FROM Delivery d JOIN FETCH d.endpoint JOIN FETCH d.event " +
           "WHERE d.status IN :statuses AND (d.nextRetryAt IS NULL OR d.nextRetryAt <= :now) " +
           "ORDER BY d.createdAt ASC")
    List<Delivery> findDueDeliveries(@Param("statuses") Collection<DeliveryStatus> statuses,
                                    @Param("now") Instant now,
                                    Pageable pageable);
}

