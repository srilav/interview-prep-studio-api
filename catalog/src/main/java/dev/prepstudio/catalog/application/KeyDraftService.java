package dev.prepstudio.catalog.application;

import dev.prepstudio.ai.LlmGateway;
import dev.prepstudio.ai.LlmRequest;
import dev.prepstudio.catalog.domain.Difficulty;
import dev.prepstudio.catalog.domain.KeyContent;
import dev.prepstudio.catalog.domain.KeyPath;
import dev.prepstudio.shared.domain.DomainException;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Asks the LLM for a first draft of a key's definition and Java example. Editors review before publishing. */
@Service
public class KeyDraftService {

    static final String SYSTEM_PROMPT = """
            You write entries for a study glossary used by software engineers preparing for technical interviews.
            Given a term and where it sits in the glossary, answer with exactly these three sections and nothing else:

            ### SHORT
            One or two sentences a candidate could say out loud in an interview. At most 250 characters.

            ### DETAIL
            How it works, trade-offs, complexity and the pitfalls interviewers probe. Plain prose, 80 to 180 words.

            ### JAVA
            One short, compilable Java 21 snippet in a ```java fenced block that shows the idea. Comment the key line.

            Match the depth to the difficulty: EASY covers fundamentals, MEDIUM internals and trade-offs,
            HARD edge cases, concurrency and performance. Be technically precise.
            """;

    private static final Pattern SECTION = Pattern.compile(
            "###\\s*SHORT\\s*(?<short>.*?)###\\s*DETAIL\\s*(?<detail>.*?)###\\s*JAVA\\s*(?<java>.*)", Pattern.DOTALL);
    private static final Pattern CODE = Pattern.compile("```(?:java)?\\s*\\n(?<code>.*?)```", Pattern.DOTALL);

    public record KeyDraft(String shortDefinition, String detail, String javaExample) {
    }

    private final LlmGateway llm;
    private final KeyService keys;

    public KeyDraftService(LlmGateway llm, KeyService keys) {
        this.llm = llm;
        this.keys = keys;
    }

    public KeyDraft draft(String subcategoryId, String term, Difficulty difficulty) {
        if (term == null || term.isBlank()) throw new DomainException("Enter a term first, then the AI can draft the rest");
        KeyPath path = keys.pathOf(subcategoryId);
        String prompt = "Term: %s%nGlossary location: %s%nDifficulty: %s".formatted(
                term.strip(), path.display(), difficulty == null ? Difficulty.MEDIUM : difficulty);
        return parse(llm.complete(LlmRequest.single(SYSTEM_PROMPT, prompt, 2000)));
    }

    static KeyDraft parse(String text) {
        Matcher m = SECTION.matcher(text);
        if (!m.find()) return new KeyDraft("", text.strip(), "");
        String shortDef = m.group("short").strip();
        if (shortDef.length() > KeyContent.MAX_SHORT_DEFINITION) {
            shortDef = shortDef.substring(0, KeyContent.MAX_SHORT_DEFINITION - 1).stripTrailing() + "…";
        }
        String javaSection = m.group("java");
        Matcher code = CODE.matcher(javaSection);
        String java = code.find() ? code.group("code").stripTrailing() : javaSection.strip();
        return new KeyDraft(shortDef, m.group("detail").strip(), java);
    }
}
