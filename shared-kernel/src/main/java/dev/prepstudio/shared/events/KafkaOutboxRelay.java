package dev.prepstudio.shared.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/** Relays unpublished outbox rows to Kafka, keyed by aggregate id so each aggregate's events stay ordered. */
@Component
@ConditionalOnProperty(name = "prepstudio.events.kafka-enabled", havingValue = "true")
class KafkaOutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(KafkaOutboxRelay.class);

    private final OutboxEventRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    KafkaOutboxRelay(OutboxEventRepository outbox, KafkaTemplate<String, String> kafka) {
        this.outbox = outbox;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${prepstudio.events.relay-interval-ms:500}")
    @Transactional
    public void relay() {
        for (OutboxEvent event : outbox.findTop100ByPublishedAtIsNullOrderByOccurredAtAsc()) {
            try {
                kafka.send(event.getTopic(), event.getAggregateId(), event.getPayload()).get(10, TimeUnit.SECONDS);
                event.markPublished();
            } catch (Exception e) {
                log.warn("Kafka unavailable, will retry outbox event {} ({}): {}", event.getId(), event.getEventType(), e.getMessage());
                return; // keep order: stop at the first failure and retry on the next tick
            }
        }
    }
}
