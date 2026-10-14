package dev.prepstudio.catalog.domain;

import dev.prepstudio.catalog.domain.events.CategoryAdded;
import dev.prepstudio.catalog.domain.events.PlatformAdded;
import dev.prepstudio.catalog.domain.events.SubcategoryAdded;
import dev.prepstudio.shared.domain.ConflictException;
import dev.prepstudio.shared.domain.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformTest {

    @Test
    void buildsTheTaxonomyAndRaisesAnEventPerStep() {
        Platform java = Platform.create("  Java ", PlatformKind.LANGUAGE);
        Category collections = java.addCategory("Collections");
        Subcategory map = java.addSubcategory(collections.getId(), "Map");

        assertThat(java.getName()).isEqualTo("Java");
        assertThat(java.pathTo(map.getId())).get()
                .extracting(KeyPath::display).isEqualTo("Java › Collections › Map");
        assertThat(java.pullEvents()).hasExactlyElementsOfTypes(PlatformAdded.class, CategoryAdded.class, SubcategoryAdded.class);
        assertThat(java.pullEvents()).isEmpty();
    }

    @Test
    void rejectsDuplicateSiblingNamesIgnoringCase() {
        Platform java = Platform.create("Java", PlatformKind.LANGUAGE);
        Category collections = java.addCategory("Collections");
        java.addSubcategory(collections.getId(), "Map");

        assertThatThrownBy(() -> java.addCategory("collections")).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> java.addSubcategory(collections.getId(), "MAP")).isInstanceOf(ConflictException.class);
    }

    @Test
    void requiresNameAndKind() {
        assertThatThrownBy(() -> Platform.create(" ", PlatformKind.TOOL)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Platform.create("Docker", null)).isInstanceOf(DomainException.class);
    }
}
