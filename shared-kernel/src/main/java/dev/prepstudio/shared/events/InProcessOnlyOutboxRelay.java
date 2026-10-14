package dev.prepstudio.shared.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Used when Kafka is switched off (local development without a broker). Events are still delivered
 * to in-process listeners; this only marks outbox rows as handled so the table does not grow.
 */
@Component
@ConditionalOnProperty(name = "prepstudio.events.kafka-enabled", havingValue = "false", matchIfMissing = true)
class InProcessOnlyOutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(InProcessOnlyOutboxRelay.class);

    private final OutboxEventRepository outbox;

    InProcessOnlyOutboxRelay(OutboxEventRepository outbox) {
        this.outbox = outbox;
    }

    @Scheduled(fixedDelayString = "${prepstudio.events.relay-interval-ms:500}")
    @Transactional
    public void relay() {
        for (OutboxEvent event : outbox.findTop100ByPublishedAtIsNullOrderByOccurredAtAsc()) {
            log.debug("Kafka disabled; {} {} delivered in-process only", event.getEventType(), event.getAggregateId());
            event.markPublished();
        }
    }
}
