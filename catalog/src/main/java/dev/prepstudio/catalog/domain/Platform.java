package dev.prepstudio.catalog.domain;

import dev.prepstudio.catalog.domain.events.CategoryAdded;
import dev.prepstudio.catalog.domain.events.PlatformAdded;
import dev.prepstudio.catalog.domain.events.SubcategoryAdded;
import dev.prepstudio.shared.domain.AggregateRoot;
import dev.prepstudio.shared.domain.DomainException;
import dev.prepstudio.shared.domain.Ids;
import dev.prepstudio.shared.domain.NotFoundException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Aggregate root for one branch of the taxonomy: a language, framework, platform or tool,
 * with its categories and subcategories (fixed depth of three).
 * Invariant: names are unique (case-insensitive) among siblings.
 */
@Entity
@Table(name = "platform")
public class Platform extends AggregateRoot {

    @Id
    private String id;
    private String name;
    @Enumerated(EnumType.STRING)
    private PlatformKind kind;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "platform_id", nullable = false)
    @OrderColumn(name = "position")
    private List<Category> categories = new ArrayList<>();

    @Version
    @Column(name = "lock_version")
    private long lockVersion;

    protected Platform() {
    }

    public static Platform create(String name, PlatformKind kind) {
        if (kind == null) throw new DomainException("Choose whether this is a language, framework, platform or tool");
        var platform = new Platform();
        platform.id = Ids.newId();
        platform.name = Named.validName(name, "language or platform");
        platform.kind = kind;
        platform.registerEvent(new PlatformAdded(platform.id, platform.name, kind, Instant.now()));
        return platform;
    }

    public Category addCategory(String name) {
        String valid = Named.validName(name, "category");
        Named.requireUnique(categories, valid, "category");
        var category = new Category(valid);
        categories.add(category);
        registerEvent(new CategoryAdded(id, category.getId(), valid, Instant.now()));
        return category;
    }

    public Subcategory addSubcategory(String categoryId, String name) {
        Category category = categories.stream()
                .filter(c -> c.getId().equals(categoryId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Category", categoryId));
        Subcategory subcategory = category.addSubcategory(name);
        registerEvent(new SubcategoryAdded(id, categoryId, subcategory.getId(), subcategory.getName(), Instant.now()));
        return subcategory;
    }

    /** Resolves where a subcategory sits inside this platform. */
    public Optional<KeyPath> pathTo(String subcategoryId) {
        for (Category category : categories) {
            for (Subcategory sub : category.getSubcategories()) {
                if (sub.getId().equals(subcategoryId)) {
                    return Optional.of(new KeyPath(id, name, category.getId(), category.getName(), sub.getId(), sub.getName()));
                }
            }
        }
        return Optional.empty();
    }

    public List<String> subcategoryIds() {
        return categories.stream().flatMap(c -> c.getSubcategories().stream()).map(Subcategory::getId).toList();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public PlatformKind getKind() { return kind; }
    public List<Category> getCategories() { return Collections.unmodifiableList(categories); }
}
