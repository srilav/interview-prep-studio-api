package dev.prepstudio.discussion.domain.events;

import dev.prepstudio.shared.domain.DomainEvent;

import java.time.Instant;

public record MessagePosted(String threadId, String keyId, String messageId, String author, boolean fromAiTutor, String content, Instant occurredAt) implements DomainEvent {

    @Override
    public String aggregateId() {
        return threadId;
    }

    @Override
    public String context() {
        return "discussion";
    }
}
