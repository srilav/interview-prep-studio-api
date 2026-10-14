package dev.prepstudio.shared.domain;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.List;

/** Base class for aggregate roots. Collects domain events until the application service publishes them. */
@MappedSuperclass
public abstract class AggregateRoot {

    @Transient
    private final List<DomainEvent> events = new ArrayList<>();

    protected void registerEvent(DomainEvent event) {
        events.add(event);
    }

    /** Returns the pending events and clears them, so each event is published exactly once. */
    public List<DomainEvent> pullEvents() {
        var pending = List.copyOf(events);
        events.clear();
        return pending;
    }
}
