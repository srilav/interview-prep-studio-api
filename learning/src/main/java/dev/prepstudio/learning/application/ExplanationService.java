package dev.prepstudio.learning.application;

import dev.prepstudio.ai.LlmGateway;
import dev.prepstudio.ai.LlmRequest;
import dev.prepstudio.catalog.application.CatalogQueries;
import dev.prepstudio.catalog.application.CatalogQueries.KeySnapshot;
import dev.prepstudio.catalog.domain.Difficulty;
import dev.prepstudio.learning.domain.Explanation;
import dev.prepstudio.learning.domain.ExplanationRepository;
import dev.prepstudio.shared.domain.NotFoundException;
import dev.prepstudio.shared.events.DomainEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.stream.Stream;

/**
 * Serves a stored explanation or generates one with the LLM and streams it.
 * The LLM call runs outside any transaction; state changes before and after it each commit on their own.
 */
@Service
public class ExplanationService {

    static final String SYSTEM_PROMPT = """
            You are an expert Java interview coach. Explain the given key to a candidate preparing for a
            technical interview. Respond in Markdown with these sections, in this order:
            ## Summary
            ## Key points
            ## Java example  (one compilable Java 21 snippet in a ```java fenced block, with comments)
            ## Complexity
            ## Common pitfalls
            ## Likely interview questions
            Match the depth to the difficulty: EASY covers fundamentals, MEDIUM internals and trade-offs,
            HARD edge cases, concurrency and performance. Be technically precise and concise.
            """;

    /** Receives the explanation as it is produced. */
    public interface Sink {
        void delta(String markdown);

        void completed(Explanation explanation);
    }

    private final ExplanationRepository explanations;
    private final CatalogQueries catalog;
    private final LlmGateway llm;
    private final DomainEventPublisher events;
    private final TransactionTemplate tx;

    public ExplanationService(ExplanationRepository explanations, CatalogQueries catalog, LlmGateway llm,
                              DomainEventPublisher events, TransactionTemplate tx) {
        this.explanations = explanations;
        this.catalog = catalog;
        this.llm = llm;
        this.events = events;
        this.tx = tx;
    }

    public void stream(String keyId, Difficulty level, boolean regenerate, String requestedBy, Sink sink) {
        KeySnapshot key = catalog.keySnapshot(keyId).orElseThrow(() -> new NotFoundException("Key", keyId));
        Difficulty lvl = level == null ? Difficulty.MEDIUM : level;

        // 1. Serve the stored explanation, or mark a generation as started.
        Explanation cached = tx.execute(status -> {
            Explanation e = explanations.findByKeyIdAndLevel(keyId, lvl)
                    .orElseGet(() -> Explanation.request(keyId, lvl, requestedBy));
            if (e.isReady() && !regenerate) {
                e.viewedBy(requestedBy);
                events.publishFrom(e);
                return e;
            }
            e.startGenerating(requestedBy);
            save(e);
            return null;
        });
        if (cached != null) {
            sink.completed(cached);
            return;
        }

        // 2. Generate without holding a transaction.
        var text = new StringBuilder();
        String failure = null;
        try (Stream<String> parts = llm.stream(LlmRequest.single(SYSTEM_PROMPT, prompt(key, lvl), 4000))) {
            parts.forEach(part -> {
                text.append(part);
                sink.delta(part);
            });
        } catch (RuntimeException e) {
            failure = e.getMessage();
        }

        // 3. Record the outcome.
        String[] error = {failure};
        Explanation result = tx.execute(status -> {
            Explanation e = explanations.findByKeyIdAndLevel(keyId, lvl).orElseThrow();
            if (error[0] == null) {
                try {
                    e.complete(text.toString());
                } catch (RuntimeException invalid) {
                    error[0] = invalid.getMessage();
                }
            }
            if (error[0] != null) e.fail(error[0]);
            save(e);
            return e;
        });
        if (error[0] != null) throw new GenerationFailedException(result.getFailureReason());
        sink.completed(result);
    }

    private void save(Explanation e) {
        try {
            explanations.saveAndFlush(e);
        } catch (DataIntegrityViolationException race) {
            // Another request created the same (key, level) row first.
            throw new dev.prepstudio.shared.domain.ConflictException(
                    "An explanation for this key is already being generated. Try again in a moment.");
        }
        events.publishFrom(e);
    }

    private static String prompt(KeySnapshot key, Difficulty level) {
        return "Key: %s%nWhere it sits: %s%nDefinition on file: %s%nDifficulty: %s"
                .formatted(key.term(), key.path(), key.shortDefinition(), level);
    }

    /** Generation failed; the reason has been recorded on the aggregate. */
    public static class GenerationFailedException extends RuntimeException {
        public GenerationFailedException(String reason) {
            super("Could not generate the explanation: " + reason);
        }
    }
}
