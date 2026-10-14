package dev.prepstudio.learning.domain;

import dev.prepstudio.catalog.domain.Difficulty;
import dev.prepstudio.learning.domain.events.ExplanationGenerated;
import dev.prepstudio.learning.domain.events.ExplanationGenerationFailed;
import dev.prepstudio.learning.domain.events.ExplanationRequested;
import dev.prepstudio.learning.domain.events.ExplanationViewed;
import dev.prepstudio.shared.domain.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExplanationTest {

    private static final String GOOD = "## Summary\nx\n## Java example\n```java\nint x = 1;\n```";

    @Test
    void completesWhenTheContentHasAJavaExample() {
        Explanation e = Explanation.request("k-1", Difficulty.EASY, "ada");
        e.startGenerating("ada");
        e.complete(GOOD);

        assertThat(e.isReady()).isTrue();
        assertThat(e.getRevision()).isEqualTo(1);
        assertThat(e.pullEvents()).hasExactlyElementsOfTypes(ExplanationRequested.class, ExplanationGenerated.class);
    }

    @Test
    void rejectsContentWithoutJava() {
        Explanation e = Explanation.request("k-1", Difficulty.EASY, "ada");
        e.startGenerating("ada");
        assertThatThrownBy(() -> e.complete("## Summary\nno code")).isInstanceOf(DomainException.class);
    }

    @Test
    void aFailedRegenerationKeepsServingThePreviousRevision() {
        Explanation e = Explanation.request("k-1", Difficulty.HARD, "ada");
        e.startGenerating("ada");
        e.complete(GOOD);
        e.pullEvents();

        e.startGenerating("grace");
        e.fail("timeout");

        assertThat(e.isReady()).isTrue();
        assertThat(e.getMarkdown()).isEqualTo(GOOD);
        assertThat(e.pullEvents()).hasExactlyElementsOfTypes(ExplanationRequested.class, ExplanationGenerationFailed.class);
    }

    @Test
    void refusesASecondConcurrentGeneration() {
        Explanation e = Explanation.request("k-1", Difficulty.MEDIUM, "ada");
        e.startGenerating("ada");
        assertThatThrownBy(() -> e.startGenerating("grace")).isInstanceOf(DomainException.class);
    }

    @Test
    void viewingAReadyExplanationIsRecorded() {
        Explanation e = Explanation.request("k-1", Difficulty.MEDIUM, "ada");
        e.startGenerating("ada");
        e.complete(GOOD);
        e.pullEvents();

        e.viewedBy("linus");
        assertThat(e.pullEvents()).hasExactlyElementsOfTypes(ExplanationViewed.class);
    }
}
