package dev.prepstudio.discussion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscussionThreadRepository extends JpaRepository<DiscussionThread, String> {
    List<DiscussionThread> findByKeyIdOrderByOpenedAtDesc(String keyId);
}
