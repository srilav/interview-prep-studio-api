package dev.prepstudio.catalog.application;

import dev.prepstudio.catalog.domain.Key;
import dev.prepstudio.catalog.domain.KeyContent;
import dev.prepstudio.catalog.domain.KeyPath;
import dev.prepstudio.catalog.domain.KeyRepository;
import dev.prepstudio.catalog.domain.KeyStatus;
import dev.prepstudio.catalog.domain.Platform;
import dev.prepstudio.catalog.domain.PlatformRepository;
import dev.prepstudio.shared.domain.ConflictException;
import dev.prepstudio.shared.domain.NotFoundException;
import dev.prepstudio.shared.events.DomainEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Use cases for keys: authoring (admin) and lookup (learners). */
@Service
public class KeyService {

    private final KeyRepository keys;
    private final PlatformRepository platforms;
    private final DomainEventPublisher events;

    public KeyService(KeyRepository keys, PlatformRepository platforms, DomainEventPublisher events) {
        this.keys = keys;
        this.platforms = platforms;
        this.events = events;
    }

    public record KeyView(Key key, KeyPath path) {
    }

    @Transactional(readOnly = true)
    public List<KeyView> list(String subcategoryId, KeyStatus status) {
        KeyPath path = pathOf(subcategoryId);
        List<Key> found = status == null
                ? keys.findBySubcategoryIdOrderByTermAsc(subcategoryId)
                : keys.findBySubcategoryIdAndStatusOrderByTermAsc(subcategoryId, status);
        return found.stream().map(k -> new KeyView(k, path)).toList();
    }

    @Transactional(readOnly = true)
    public KeyView get(String keyId) {
        Key key = key(keyId);
        return new KeyView(key, pathOf(key.getSubcategoryId()));
    }

    /** Creates the key when {@code keyId} is blank, otherwise revises it (and moves it if the subcategory changed). */
    @Transactional
    public KeyView upsert(String keyId, String subcategoryId, KeyContent content, KeyStatus status) {
        KeyPath path = pathOf(subcategoryId);
        Key key;
        if (keyId == null || keyId.isBlank()) {
            requireUniqueTerm(subcategoryId, content.term(), null);
            key = Key.create(subcategoryId, content, status);
        } else {
            key = key(keyId);
            requireUniqueTerm(subcategoryId, content.term(), key.getId());
            key.revise(content, status);
            key.moveTo(subcategoryId);
        }
        keys.save(key);
        events.publishFrom(key);
        return new KeyView(key, path);
    }

    @Transactional
    public KeyView move(String keyId, String targetSubcategoryId) {
        Key key = key(keyId);
        KeyPath path = pathOf(targetSubcategoryId);
        requireUniqueTerm(targetSubcategoryId, key.getTerm(), key.getId());
        key.moveTo(targetSubcategoryId);
        keys.save(key);
        events.publishFrom(key);
        return new KeyView(key, path);
    }

    @Transactional
    public void delete(String keyId) {
        Key key = key(keyId);
        key.delete();
        events.publishFrom(key);
        keys.delete(key);
    }

    /** Learner search over published keys, by term or alias. */
    @Transactional(readOnly = true)
    public List<KeyView> search(String query, String platformId, int limit) {
        int size = limit <= 0 ? 20 : Math.min(limit, 100);
        String pattern = "%" + (query == null ? "" : query.strip().toLowerCase(Locale.ROOT)) + "%";
        var page = PageRequest.of(0, size);
        List<Key> found;
        if (platformId == null || platformId.isBlank()) {
            found = keys.search(KeyStatus.PUBLISHED, pattern, page);
        } else {
            Platform platform = platforms.findById(platformId)
                    .orElseThrow(() -> new NotFoundException("Language or platform", platformId));
            List<String> subIds = platform.subcategoryIds();
            found = subIds.isEmpty() ? List.of() : keys.searchWithin(KeyStatus.PUBLISHED, pattern, subIds, page);
        }
        return found.stream().map(k -> new KeyView(k, pathOf(k.getSubcategoryId()))).toList();
    }

    KeyPath pathOf(String subcategoryId) {
        return platforms.findBySubcategoryId(subcategoryId)
                .flatMap(p -> p.pathTo(subcategoryId))
                .orElseThrow(() -> new NotFoundException("Subcategory", subcategoryId));
    }

    private Key key(String id) {
        return keys.findById(id).orElseThrow(() -> new NotFoundException("Key", id));
    }

    private void requireUniqueTerm(String subcategoryId, String term, String exceptKeyId) {
        boolean taken = keys.findBySubcategoryAndTerm(subcategoryId, term).stream()
                .anyMatch(k -> !k.getId().equals(exceptKeyId));
        if (taken) {
            throw new ConflictException("\"" + term + "\" already exists in this subcategory. Edit that key instead.");
        }
    }
}
