package dev.prepstudio.learning.domain;

import dev.prepstudio.catalog.domain.Difficulty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExplanationRepository extends JpaRepository<Explanation, String> {
    Optional<Explanation> findByKeyIdAndLevel(String keyId, Difficulty level);
}
