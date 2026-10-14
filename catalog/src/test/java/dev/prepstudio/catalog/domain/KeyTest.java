package dev.prepstudio.catalog.domain;

import dev.prepstudio.catalog.domain.events.KeyCreated;
import dev.prepstudio.catalog.domain.events.KeyMoved;
import dev.prepstudio.catalog.domain.events.KeyPublished;
import dev.prepstudio.catalog.domain.events.KeyUpdated;
import dev.prepstudio.shared.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeyTest {

    private static KeyContent content(String term, String shortDef) {
        return new KeyContent(term, List.of("java.util.HashMap", " ", "java.util.HashMap"), shortDef, null, null,
                null, List.of("hashing"), List.of());
    }

    @Test
    void createsADraftAndNormalisesContent() {
        Key key = Key.create("s-map", content(" HashMap ", "Hash table Map."), null);

        assertThat(key.getTerm()).isEqualTo("HashMap");
        assertThat(key.getAliases()).containsExactly("java.util.HashMap");
        assertThat(key.getDifficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(key.getStatus()).isEqualTo(KeyStatus.DRAFT);
        assertThat(key.pullEvents()).hasExactlyElementsOfTypes(KeyCreated.class);
    }

    @Test
    void publishingRaisesKeyPublishedOnlyOnce() {
        Key key = Key.create("s-map", content("HashMap", "Hash table Map."), KeyStatus.PUBLISHED);
        assertThat(key.pullEvents()).hasExactlyElementsOfTypes(KeyCreated.class, KeyPublished.class);

        key.revise(content("HashMap", "Updated."), KeyStatus.PUBLISHED);
        assertThat(key.pullEvents()).hasExactlyElementsOfTypes(KeyUpdated.class);
    }

    @Test
    void movingToAnotherSubcategoryRaisesKeyMoved() {
        Key key = Key.create("s-map", content("HashMap", "Hash table Map."), null);
        key.pullEvents();

        key.moveTo("s-map");
        assertThat(key.pullEvents()).isEmpty();
        key.moveTo("s-hashing");
        assertThat(key.getSubcategoryId()).isEqualTo("s-hashing");
        assertThat(key.pullEvents()).hasExactlyElementsOfTypes(KeyMoved.class);
    }

    @Test
    void enforcesTheShortDefinitionRules() {
        assertThatThrownBy(() -> content("HashMap", " ")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> content("HashMap", "x".repeat(281)))
                .isInstanceOf(DomainException.class).hasMessageContaining("by 1 characters");
        assertThat(content("HashMap", "x".repeat(280)).shortDefinition()).hasSize(280);
    }
}
