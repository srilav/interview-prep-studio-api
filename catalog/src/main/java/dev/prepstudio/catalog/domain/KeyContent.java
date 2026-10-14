package dev.prepstudio.catalog.domain;

import dev.prepstudio.shared.domain.DomainException;

import java.util.LinkedHashSet;
import java.util.List;

/** The editable content of a key, validated on construction. */
public record KeyContent(String term,
                         List<String> aliases,
                         String shortDefinition,
                         String detail,
                         String javaExample,
                         Difficulty difficulty,
                         List<String> tags,
                         List<String> relatedTerms) {

    public static final int MAX_SHORT_DEFINITION = 280;
    public static final int MAX_TERM = 160;

    public KeyContent {
        if (term == null || term.isBlank()) throw new DomainException("Enter the key term");
        term = term.strip();
        if (term.length() > MAX_TERM) throw new DomainException("A term can have at most " + MAX_TERM + " characters");
        if (shortDefinition == null || shortDefinition.isBlank()) {
            throw new DomainException("Add a short definition so the key shows up in search");
        }
        shortDefinition = shortDefinition.strip();
        if (shortDefinition.length() > MAX_SHORT_DEFINITION) {
            throw new DomainException("Shorten the short definition by " + (shortDefinition.length() - MAX_SHORT_DEFINITION)
                    + " characters; put the rest in the detailed explanation");
        }
        detail = detail == null ? "" : detail.strip();
        javaExample = javaExample == null ? "" : javaExample.stripTrailing();
        difficulty = difficulty == null ? Difficulty.MEDIUM : difficulty;
        aliases = clean(aliases);
        tags = clean(tags);
        relatedTerms = clean(relatedTerms);
    }

    private static List<String> clean(List<String> values) {
        if (values == null) return List.of();
        var unique = new LinkedHashSet<String>();
        values.stream().filter(v -> v != null && !v.isBlank()).map(String::strip).forEach(unique::add);
        return List.copyOf(unique);
    }
}
