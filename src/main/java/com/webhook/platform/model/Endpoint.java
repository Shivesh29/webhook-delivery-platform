package com.webhook.platform.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "endpoints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Endpoint {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 1024)
    private String url;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 128)
    private String secret;

    @Column(name = "subscribed_events", nullable = false, length = 1000)
    @Builder.Default
    private String subscribedEvents = "*";

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean matchesEvent(String eventType) {
        if (!enabled || eventType == null) {
            return false;
        }
        if ("*".equals(subscribedEvents.trim())) {
            return true;
        }
        return Arrays.stream(subscribedEvents.split(","))
                .map(String::trim)
                .anyMatch(pattern -> {
                    if ("*".equals(pattern)) {
                        return true;
                    }
                    if (pattern.endsWith(".*")) {
                        String prefix = pattern.substring(0, pattern.length() - 2);
                        return eventType.startsWith(prefix + ".");
                    }
                    return pattern.equalsIgnoreCase(eventType);
                });
    }
}
