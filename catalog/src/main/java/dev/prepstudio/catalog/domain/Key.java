package dev.prepstudio.catalog.domain;

import dev.prepstudio.catalog.domain.events.KeyCreated;
import dev.prepstudio.catalog.domain.events.KeyDeleted;
import dev.prepstudio.catalog.domain.events.KeyMoved;
import dev.prepstudio.catalog.domain.events.KeyPublished;
import dev.prepstudio.catalog.domain.events.KeyUpdated;
import dev.prepstudio.shared.domain.AggregateRoot;
import dev.prepstudio.shared.domain.Ids;
import dev.prepstudio.shared.persistence.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An interview term or concept with its definition, filed under one subcategory.
 * Its own aggregate (not part of Platform) so editing keys never contends with taxonomy changes.
 * Invariants live in {@link KeyContent}; term uniqueness per subcategory is checked by the application service.
 */
@Entity
@Table(name = "catalog_key")
public class Key extends AggregateRoot {

    @Id
    private String id;
    private String subcategoryId;
    private String term;
    /** Lower-cased term and aliases, maintained for search. */
    private String searchText;
    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "text")
    private List<String> aliases;
    private String shortDefinition;
    @Column(columnDefinition = "text")
    private String detail;
    @Column(columnDefinition = "text")
    private String javaExample;
    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;
    @Enumerated(EnumType.STRING)
    private KeyStatus status;
    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "text")
    private List<String> tags;
    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "text")
    private List<String> relatedTerms;
    private Instant createdAt;
    private Instant updatedAt;

    @Version
    @Column(name = "lock_version")
    private long lockVersion;

    protected Key() {
    }

    public static Key create(String subcategoryId, KeyContent content, KeyStatus status) {
        var key = new Key();
        key.id = Ids.newId();
        key.subcategoryId = subcategoryId;
        key.createdAt = Instant.now();
        key.status = KeyStatus.DRAFT;
        key.apply(content);
        key.registerEvent(new KeyCreated(key.id, subcategoryId, key.term, key.updatedAt));
        key.changeStatus(status == null ? KeyStatus.DRAFT : status);
        return key;
    }

    public void revise(KeyContent content, KeyStatus newStatus) {
        apply(content);
        registerEvent(new KeyUpdated(id, subcategoryId, term, updatedAt));
        if (newStatus != null) changeStatus(newStatus);
    }

    public void moveTo(String targetSubcategoryId) {
        if (targetSubcategoryId.equals(subcategoryId)) return;
        String from = subcategoryId;
        subcategoryId = targetSubcategoryId;
        updatedAt = Instant.now();
        registerEvent(new KeyMoved(id, from, targetSubcategoryId, updatedAt));
    }

    /** Records the deletion; the caller then removes the aggregate. */
    public void delete() {
        registerEvent(new KeyDeleted(id, subcategoryId, term, Instant.now()));
    }

    private void changeStatus(KeyStatus newStatus) {
        boolean publishing = newStatus == KeyStatus.PUBLISHED && status != KeyStatus.PUBLISHED;
        status = newStatus;
        if (publishing) registerEvent(new KeyPublished(id, subcategoryId, term, Instant.now()));
    }

    private void apply(KeyContent c) {
        term = c.term();
        aliases = c.aliases();
        shortDefinition = c.shortDefinition();
        detail = c.detail();
        javaExample = c.javaExample();
        difficulty = c.difficulty();
        tags = c.tags();
        relatedTerms = c.relatedTerms();
        searchText = Stream.concat(Stream.of(term), aliases.stream())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.joining("\n"));
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getSubcategoryId() { return subcategoryId; }
    public String getTerm() { return term; }
    public List<String> getAliases() { return aliases; }
    public String getShortDefinition() { return shortDefinition; }
    public String getDetail() { return detail; }
    public String getJavaExample() { return javaExample; }
    public Difficulty getDifficulty() { return difficulty; }
    public KeyStatus getStatus() { return status; }
    public List<String> getTags() { return tags; }
    public List<String> getRelatedTerms() { return relatedTerms; }
    public Instant getUpdatedAt() { return updatedAt; }
}
