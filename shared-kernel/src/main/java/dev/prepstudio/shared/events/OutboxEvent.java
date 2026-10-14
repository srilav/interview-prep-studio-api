package dev.prepstudio.shared.events;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** A domain event stored in the same transaction as the aggregate change, waiting to be relayed to Kafka. */
@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    private String id;
    private String topic;
    private String eventType;
    private String aggregateId;
    @Column(columnDefinition = "text")
    private String payload;
    private Instant occurredAt;
    private Instant publishedAt;

    protected OutboxEvent() {
    }

    OutboxEvent(String id, String topic, String eventType, String aggregateId, String payload, Instant occurredAt) {
        this.id = id;
        this.topic = topic;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.occurredAt = occurredAt;
    }

    void markPublished() {
        this.publishedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getTopic() { return topic; }
    public String getEventType() { return eventType; }
    public String getAggregateId() { return aggregateId; }
    public String getPayload() { return payload; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getPublishedAt() { return publishedAt; }
}
