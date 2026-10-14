package dev.prepstudio.progress.domain;

import dev.prepstudio.shared.domain.Ids;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** One row of the progress read model. Written only by event projections. */
@Entity
@Table(name = "study_activity")
public class StudyActivity {

    @Id
    private String id;
    private String learner;
    private String keyId;
    @Enumerated(EnumType.STRING)
    private ActivityType type;
    private Instant occurredAt;

    protected StudyActivity() {
    }

    public StudyActivity(String learner, String keyId, ActivityType type, Instant occurredAt) {
        this.id = Ids.newId();
        this.learner = learner;
        this.keyId = keyId;
        this.type = type;
        this.occurredAt = occurredAt;
    }

    public String getLearner() { return learner; }
    public String getKeyId() { return keyId; }
    public ActivityType getType() { return type; }
    public Instant getOccurredAt() { return occurredAt; }
}
