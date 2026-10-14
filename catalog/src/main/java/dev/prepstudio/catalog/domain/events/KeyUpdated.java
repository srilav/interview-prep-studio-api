package dev.prepstudio.catalog.domain.events;

import dev.prepstudio.shared.domain.DomainEvent;

import java.time.Instant;

public record KeyUpdated(String keyId, String subcategoryId, String term, Instant occurredAt) implements DomainEvent {

    @Override
    public String aggregateId() {
        return keyId;
    }

    @Override
    public String context() {
        return "catalog";
    }
}
