package dev.prepstudio.shared.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.prepstudio.shared.domain.AggregateRoot;
import dev.prepstudio.shared.domain.DomainEvent;
import dev.prepstudio.shared.domain.Ids;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publishes an aggregate's pending events two ways, inside the caller's transaction:
 * <ul>
 *   <li>writes each to the outbox table, so the relay can deliver it to Kafka for other services;</li>
 *   <li>publishes it as a Spring event, so in-process modules can react with
 *       {@code @TransactionalEventListener} once the transaction commits.</li>
 * </ul>
 */
@Component
public class DomainEventPublisher {

    private final OutboxEventRepository outbox;
    private final ApplicationEventPublisher spring;
    private final ObjectMapper json;

    public DomainEventPublisher(OutboxEventRepository outbox, ApplicationEventPublisher spring, ObjectMapper json) {
        this.outbox = outbox;
        this.spring = spring;
        this.json = json;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishFrom(AggregateRoot aggregate) {
        aggregate.pullEvents().forEach(this::publish);
    }

    private void publish(DomainEvent event) {
        outbox.save(new OutboxEvent(
                Ids.newId(),
                event.context() + ".events",
                event.getClass().getSimpleName(),
                event.aggregateId(),
                toJson(event),
                event.occurredAt()));
        spring.publishEvent(event);
    }

    private String toJson(DomainEvent event) {
        try {
            return json.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialise " + event.getClass().getSimpleName(), e);
        }
    }
}
