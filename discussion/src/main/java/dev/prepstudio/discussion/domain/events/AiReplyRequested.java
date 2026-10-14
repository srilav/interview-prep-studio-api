package dev.prepstudio.discussion.domain.events;

import dev.prepstudio.shared.domain.DomainEvent;

import java.time.Instant;

public record AiReplyRequested(String threadId, String keyId, String triggeringMessageId, String requestedBy, Instant occurredAt) implements DomainEvent {

    @Override
    public String aggregateId() {
        return threadId;
    }

    @Override
    public String context() {
        return "discussion";
    }
}
