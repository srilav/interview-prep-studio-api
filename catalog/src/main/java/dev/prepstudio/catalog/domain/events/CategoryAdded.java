package dev.prepstudio.catalog.domain.events;

import dev.prepstudio.shared.domain.DomainEvent;

import java.time.Instant;

public record CategoryAdded(String platformId, String categoryId, String name, Instant occurredAt) implements DomainEvent {

    @Override
    public String aggregateId() {
        return platformId;
    }

    @Override
    public String context() {
        return "catalog";
    }
}
