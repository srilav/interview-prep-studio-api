package dev.prepstudio.catalog.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeyDraftServiceTest {

    @Test
    void parsesTheThreeSections() {
        var draft = KeyDraftService.parse("""
                ### SHORT
                A thread-safe map.

                ### DETAIL
                Uses CAS and per-bin locks.

                ### JAVA
                ```java
                var m = new ConcurrentHashMap<String, Integer>();
                ```
                """);

        assertThat(draft.shortDefinition()).isEqualTo("A thread-safe map.");
        assertThat(draft.detail()).isEqualTo("Uses CAS and per-bin locks.");
        assertThat(draft.javaExample()).isEqualTo("var m = new ConcurrentHashMap<String, Integer>();");
    }

    @Test
    void fallsBackToDetailWhenTheModelIgnoresTheFormat() {
        var draft = KeyDraftService.parse("Just some prose.");
        assertThat(draft.shortDefinition()).isEmpty();
        assertThat(draft.detail()).isEqualTo("Just some prose.");
    }

    @Test
    void trimsAnOverlongShortDefinition() {
        var draft = KeyDraftService.parse("### SHORT\n" + "x".repeat(400) + "\n### DETAIL\nd\n### JAVA\n```java\nx();\n```");
        assertThat(draft.shortDefinition()).hasSize(280).endsWith("…");
    }
}
