package dev.prepstudio.catalog.domain.events;

import dev.prepstudio.catalog.domain.PlatformKind;
import dev.prepstudio.shared.domain.DomainEvent;

import java.time.Instant;

public record PlatformAdded(String platformId, String name, PlatformKind kind, Instant occurredAt) implements DomainEvent {

    @Override
    public String aggregateId() {
        return platformId;
    }

    @Override
    public String context() {
        return "catalog";
    }
}
