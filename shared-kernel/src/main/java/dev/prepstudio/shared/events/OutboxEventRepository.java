package dev.prepstudio.shared.events;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();
}
