package dev.prepstudio.discussion.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, String> {
    /** Newest first; callers reverse for display. */
    List<Message> findByThreadIdOrderByPostedAtDesc(String threadId, Pageable page);
}
