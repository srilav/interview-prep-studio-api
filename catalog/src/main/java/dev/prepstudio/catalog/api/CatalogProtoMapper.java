package dev.prepstudio.catalog.api;

import dev.prepstudio.api.catalog.v1.KeyPath;
import dev.prepstudio.catalog.application.KeyService.KeyView;
import dev.prepstudio.catalog.application.TaxonomyService.TreeView;
import dev.prepstudio.catalog.domain.Category;
import dev.prepstudio.catalog.domain.Difficulty;
import dev.prepstudio.catalog.domain.Key;
import dev.prepstudio.catalog.domain.KeyContent;
import dev.prepstudio.catalog.domain.KeyStatus;
import dev.prepstudio.catalog.domain.Platform;
import dev.prepstudio.catalog.domain.PlatformKind;
import dev.prepstudio.catalog.domain.Subcategory;

import java.util.Map;

/** Translates between the protobuf contract and the Catalog domain. */
public final class CatalogProtoMapper {

    private CatalogProtoMapper() {
    }

    public static dev.prepstudio.api.catalog.v1.Tree tree(TreeView view) {
        var tree = dev.prepstudio.api.catalog.v1.Tree.newBuilder();
        view.platforms().forEach(p -> tree.addPlatforms(platform(p, view.keyCounts())));
        return tree.build();
    }

    public static dev.prepstudio.api.catalog.v1.Platform platform(Platform p, Map<String, Integer> counts) {
        var out = dev.prepstudio.api.catalog.v1.Platform.newBuilder()
                .setId(p.getId()).setName(p.getName()).setKind(kind(p.getKind()));
        p.getCategories().forEach(c -> out.addCategories(category(c, counts)));
        return out.build();
    }

    public static dev.prepstudio.api.catalog.v1.Category category(Category c, Map<String, Integer> counts) {
        var out = dev.prepstudio.api.catalog.v1.Category.newBuilder().setId(c.getId()).setName(c.getName());
        c.getSubcategories().forEach(s -> out.addSubcategories(subcategory(s, counts.getOrDefault(s.getId(), 0))));
        return out.build();
    }

    public static dev.prepstudio.api.catalog.v1.Subcategory subcategory(Subcategory s, int keyCount) {
        return dev.prepstudio.api.catalog.v1.Subcategory.newBuilder()
                .setId(s.getId()).setName(s.getName()).setKeyCount(keyCount).build();
    }

    public static dev.prepstudio.api.catalog.v1.Key key(KeyView view) {
        Key k = view.key();
        var p = view.path();
        return dev.prepstudio.api.catalog.v1.Key.newBuilder()
                .setId(k.getId())
                .setSubcategoryId(k.getSubcategoryId())
                .setTerm(k.getTerm())
                .addAllAliases(k.getAliases())
                .setShortDefinition(k.getShortDefinition())
                .setDetail(k.getDetail())
                .setJavaExample(k.getJavaExample())
                .setDifficulty(difficulty(k.getDifficulty()))
                .setStatus(status(k.getStatus()))
                .addAllTags(k.getTags())
                .addAllRelatedTerms(k.getRelatedTerms())
                .setPath(KeyPath.newBuilder()
                        .setPlatformId(p.platformId()).setPlatformName(p.platformName())
                        .setCategoryId(p.categoryId()).setCategoryName(p.categoryName())
                        .setSubcategoryId(p.subcategoryId()).setSubcategoryName(p.subcategoryName()))
                .build();
    }

    public static KeyContent content(dev.prepstudio.api.catalog.v1.Key k) {
        return new KeyContent(k.getTerm(), k.getAliasesList(), k.getShortDefinition(), k.getDetail(),
                k.getJavaExample(), difficulty(k.getDifficulty()), k.getTagsList(), k.getRelatedTermsList());
    }

    // Enum names match one-to-one; UNSPECIFIED maps to null so the domain applies its default.

    public static dev.prepstudio.api.catalog.v1.PlatformKind kind(PlatformKind k) {
        return dev.prepstudio.api.catalog.v1.PlatformKind.valueOf(k.name());
    }

    public static PlatformKind kind(dev.prepstudio.api.catalog.v1.PlatformKind k) {
        return switch (k) {
            case PLATFORM_KIND_UNSPECIFIED, UNRECOGNIZED -> null;
            default -> PlatformKind.valueOf(k.name());
        };
    }

    public static dev.prepstudio.api.catalog.v1.Difficulty difficulty(Difficulty d) {
        return dev.prepstudio.api.catalog.v1.Difficulty.valueOf(d.name());
    }

    public static Difficulty difficulty(dev.prepstudio.api.catalog.v1.Difficulty d) {
        return switch (d) {
            case DIFFICULTY_UNSPECIFIED, UNRECOGNIZED -> null;
            default -> Difficulty.valueOf(d.name());
        };
    }

    public static dev.prepstudio.api.catalog.v1.KeyStatus status(KeyStatus s) {
        return dev.prepstudio.api.catalog.v1.KeyStatus.valueOf(s.name());
    }

    public static KeyStatus status(dev.prepstudio.api.catalog.v1.KeyStatus s) {
        return switch (s) {
            case KEY_STATUS_UNSPECIFIED, UNRECOGNIZED -> null;
            default -> KeyStatus.valueOf(s.name());
        };
    }
}
