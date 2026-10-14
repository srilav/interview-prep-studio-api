package dev.prepstudio.catalog.application;

import dev.prepstudio.catalog.domain.Category;
import dev.prepstudio.catalog.domain.KeyRepository;
import dev.prepstudio.catalog.domain.Platform;
import dev.prepstudio.catalog.domain.PlatformKind;
import dev.prepstudio.catalog.domain.PlatformRepository;
import dev.prepstudio.catalog.domain.Subcategory;
import dev.prepstudio.shared.domain.ConflictException;
import dev.prepstudio.shared.domain.NotFoundException;
import dev.prepstudio.shared.events.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Use cases for building the Language/Platform › Category › Subcategory taxonomy. */
@Service
public class TaxonomyService {

    private final PlatformRepository platforms;
    private final KeyRepository keys;
    private final DomainEventPublisher events;

    public TaxonomyService(PlatformRepository platforms, KeyRepository keys, DomainEventPublisher events) {
        this.platforms = platforms;
        this.keys = keys;
        this.events = events;
    }

    public record TreeView(List<Platform> platforms, Map<String, Integer> keyCounts) {
    }

    @Transactional(readOnly = true)
    public TreeView tree() {
        Map<String, Integer> counts = new HashMap<>();
        for (Object[] row : keys.countPerSubcategory()) {
            counts.put((String) row[0], ((Number) row[1]).intValue());
        }
        return new TreeView(platforms.findAllByOrderByNameAsc(), counts);
    }

    @Transactional
    public Platform createPlatform(String name, PlatformKind kind) {
        if (name != null && platforms.existsByNameIgnoreCase(name.strip())) {
            throw new ConflictException("\"" + name.strip() + "\" already exists");
        }
        Platform platform = Platform.create(name, kind);
        platforms.save(platform);
        events.publishFrom(platform);
        return platform;
    }

    @Transactional
    public Category createCategory(String platformId, String name) {
        Platform platform = platform(platformId);
        Category category = platform.addCategory(name);
        platforms.save(platform);
        events.publishFrom(platform);
        return category;
    }

    @Transactional
    public Subcategory createSubcategory(String platformId, String categoryId, String name) {
        Platform platform = platform(platformId);
        Subcategory subcategory = platform.addSubcategory(categoryId, name);
        platforms.save(platform);
        events.publishFrom(platform);
        return subcategory;
    }

    private Platform platform(String id) {
        return platforms.findById(id).orElseThrow(() -> new NotFoundException("Language or platform", id));
    }
}
