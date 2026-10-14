package dev.prepstudio.catalog.domain.events;

import dev.prepstudio.shared.domain.DomainEvent;

import java.time.Instant;

public record KeyMoved(String keyId, String fromSubcategoryId, String toSubcategoryId, Instant occurredAt) implements DomainEvent {

    @Override
    public String aggregateId() {
        return keyId;
    }

    @Override
    public String context() {
        return "catalog";
    }
}
