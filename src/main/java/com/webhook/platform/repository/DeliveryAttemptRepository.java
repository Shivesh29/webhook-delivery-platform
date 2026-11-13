package com.webhook.platform.repository;

import com.webhook.platform.model.DeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, String> {

    List<DeliveryAttempt> findByDeliveryIdOrderByAttemptNumberAsc(String deliveryId);
}

