package dev.prepstudio.shared.domain;

import java.time.Instant;

/** Something that happened in a bounded context that other parts of the system may react to. */
public interface DomainEvent {

    /** Id of the aggregate that raised the event; used as the Kafka partition key. */
    String aggregateId();

    Instant occurredAt();

    /** Owning bounded context, e.g. "catalog". Events are published to the topic "{context}.events". */
    String context();
}
