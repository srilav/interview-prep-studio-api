package dev.prepstudio.catalog.application;

import dev.prepstudio.catalog.domain.KeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Published API of the Catalog context for other contexts (Learning, Discussion).
 * They get a read-only snapshot, never the Key aggregate itself.
 */
@Service
public class CatalogQueries {

    public record KeySnapshot(String keyId, String term, String shortDefinition, String path) {
    }

    private final KeyRepository keys;
    private final KeyService keyService;

    public CatalogQueries(KeyRepository keys, KeyService keyService) {
        this.keys = keys;
        this.keyService = keyService;
    }

    @Transactional(readOnly = true)
    public Optional<KeySnapshot> keySnapshot(String keyId) {
        return keys.findById(keyId).map(k -> new KeySnapshot(
                k.getId(), k.getTerm(), k.getShortDefinition(), keyService.pathOf(k.getSubcategoryId()).display()));
    }
}
