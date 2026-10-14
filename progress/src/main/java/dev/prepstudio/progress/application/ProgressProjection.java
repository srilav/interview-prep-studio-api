package dev.prepstudio.progress.application;

import dev.prepstudio.discussion.domain.events.MessagePosted;
import dev.prepstudio.learning.domain.events.ExplanationGenerated;
import dev.prepstudio.learning.domain.events.ExplanationViewed;
import dev.prepstudio.progress.domain.ActivityType;
import dev.prepstudio.progress.domain.StudyActivity;
import dev.prepstudio.progress.domain.StudyActivityRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

/**
 * Projects other contexts' events into the learner's study log. Runs after the source transaction
 * commits, in its own transaction, so a projection failure never rolls back the source change.
 */
@Component
public class ProgressProjection {

    private final StudyActivityRepository activities;

    public ProgressProjection(StudyActivityRepository activities) {
        this.activities = activities;
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ExplanationViewed e) {
        record(e.learner(), e.keyId(), ActivityType.EXPLANATION_VIEWED, e.occurredAt());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ExplanationGenerated e) {
        record(e.requestedBy(), e.keyId(), ActivityType.EXPLANATION_VIEWED, e.occurredAt());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(MessagePosted e) {
        if (!e.fromAiTutor()) record(e.author(), e.keyId(), ActivityType.MESSAGE_POSTED, e.occurredAt());
    }

    private void record(String learner, String keyId, ActivityType type, Instant at) {
        if (learner == null || learner.isBlank()) return; // anonymous request
        activities.save(new StudyActivity(learner.strip(), keyId, type, at));
    }
}
