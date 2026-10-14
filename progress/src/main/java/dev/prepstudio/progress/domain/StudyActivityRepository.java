package dev.prepstudio.progress.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyActivityRepository extends JpaRepository<StudyActivity, String> {

    long countByLearnerIgnoreCaseAndType(String learner, ActivityType type);

    List<StudyActivity> findTop10ByLearnerIgnoreCaseOrderByOccurredAtDesc(String learner);
}
