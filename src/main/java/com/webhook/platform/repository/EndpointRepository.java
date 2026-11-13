package com.webhook.platform.repository;

import com.webhook.platform.model.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EndpointRepository extends JpaRepository<Endpoint, String> {

    List<Endpoint> findByEnabledTrue();

    long countByEnabledTrue();
}

