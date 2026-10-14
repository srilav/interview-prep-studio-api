package dev.prepstudio.ai;

import java.util.List;
import java.util.Objects;

/**
 * @param system    instructions that frame every turn
 * @param turns     conversation so far; the first turn must come from the user
 * @param maxTokens upper bound on the response length
 */
public record LlmRequest(String system, List<LlmTurn> turns, long maxTokens) {

    public LlmRequest {
        Objects.requireNonNull(system, "system");
        turns = List.copyOf(turns);
        if (turns.isEmpty() || turns.get(0).speaker() != LlmTurn.Speaker.USER) {
            throw new IllegalArgumentException("The first turn must come from the user");
        }
        if (maxTokens <= 0) throw new IllegalArgumentException("maxTokens must be positive");
    }

    public static LlmRequest single(String system, String userMessage, long maxTokens) {
        return new LlmRequest(system, List.of(LlmTurn.user(userMessage)), maxTokens);
    }
}
