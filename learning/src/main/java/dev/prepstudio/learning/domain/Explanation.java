package dev.prepstudio.learning.domain;

import dev.prepstudio.catalog.domain.Difficulty;
import dev.prepstudio.learning.domain.events.ExplanationGenerated;
import dev.prepstudio.learning.domain.events.ExplanationGenerationFailed;
import dev.prepstudio.learning.domain.events.ExplanationRequested;
import dev.prepstudio.learning.domain.events.ExplanationViewed;
import dev.prepstudio.shared.domain.AggregateRoot;
import dev.prepstudio.shared.domain.DomainException;
import dev.prepstudio.shared.domain.Ids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Duration;
import java.time.Instant;

/**
 * AI-generated study material for one key at one difficulty level.
 * Invariants: content is accepted only if it contains a Java code block; a READY explanation
 * keeps serving its last good content while a regeneration is in progress.
 */
@Entity
@Table(name = "explanation")
public class Explanation extends AggregateRoot {

    private static final int MAX_REASON = 500;
    /** A generation older than this is treated as abandoned (for example, the server restarted mid-stream). */
    private static final Duration STALE_AFTER = Duration.ofMinutes(5);

    @Id
    private String id;
    private String keyId;
    @Enumerated(EnumType.STRING)
    private Difficulty level;
    @Enumerated(EnumType.STRING)
    private ExplanationStatus status;
    @Column(columnDefinition = "text")
    private String markdown;
    private int revision;
    private String requestedBy;
    private String failureReason;
    private Instant updatedAt;

    @Version
    @Column(name = "lock_version")
    private long lockVersion;

    protected Explanation() {
    }

    public static Explanation request(String keyId, Difficulty level, String requestedBy) {
        var e = new Explanation();
        e.id = Ids.newId();
        e.keyId = keyId;
        e.level = level;
        e.status = ExplanationStatus.REQUESTED;
        e.requestedBy = requestedBy;
        e.updatedAt = Instant.now();
        e.registerEvent(new ExplanationRequested(e.id, keyId, level, requestedBy, e.updatedAt));
        return e;
    }

    /** Starts a new generation, for a fresh request or a regeneration. */
    public void startGenerating(String requestedBy) {
        boolean stillRunning = updatedAt.isAfter(Instant.now().minus(STALE_AFTER));
        if (status == ExplanationStatus.GENERATING && stillRunning) {
            throw new DomainException("An explanation for this key is already being generated. Try again in a moment.");
        }
        if (status != ExplanationStatus.REQUESTED) {
            registerEvent(new ExplanationRequested(id, keyId, level, requestedBy, Instant.now()));
        }
        this.requestedBy = requestedBy;
        this.status = ExplanationStatus.GENERATING;
        this.updatedAt = Instant.now();
    }

    public void complete(String generatedMarkdown) {
        if (status != ExplanationStatus.GENERATING) throw new DomainException("No generation in progress");
        if (generatedMarkdown == null || !generatedMarkdown.contains("```java")) {
            throw new DomainException("An explanation must include a Java example");
        }
        this.markdown = generatedMarkdown.strip();
        this.revision++;
        this.status = ExplanationStatus.READY;
        this.failureReason = null;
        this.updatedAt = Instant.now();
        registerEvent(new ExplanationGenerated(id, keyId, level, revision, requestedBy, updatedAt));
    }

    public void fail(String reason) {
        String why = reason == null ? "Unknown error" : reason;
        this.failureReason = why.length() > MAX_REASON ? why.substring(0, MAX_REASON) : why;
        // Keep serving the previous revision if there is one.
        this.status = markdown == null ? ExplanationStatus.FAILED : ExplanationStatus.READY;
        this.updatedAt = Instant.now();
        registerEvent(new ExplanationGenerationFailed(id, keyId, level, failureReason, updatedAt));
    }

    /** A learner was served the stored explanation. */
    public void viewedBy(String learner) {
        if (status != ExplanationStatus.READY) throw new DomainException("Explanation is not ready");
        registerEvent(new ExplanationViewed(id, keyId, level, learner, Instant.now()));
    }

    public boolean isReady() {
        return status == ExplanationStatus.READY;
    }

    public String getId() { return id; }
    public String getKeyId() { return keyId; }
    public Difficulty getLevel() { return level; }
    public ExplanationStatus getStatus() { return status; }
    public String getMarkdown() { return markdown; }
    public int getRevision() { return revision; }
    public String getFailureReason() { return failureReason; }
}
