package dev.prepstudio.learning.domain.events;

import dev.prepstudio.catalog.domain.Difficulty;
import dev.prepstudio.shared.domain.DomainEvent;

import java.time.Instant;

public record ExplanationViewed(String explanationId, String keyId, Difficulty level, String learner, Instant occurredAt) implements DomainEvent {

    @Override
    public String aggregateId() {
        return explanationId;
    }

    @Override
    public String context() {
        return "learning";
    }
}
