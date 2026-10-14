package dev.prepstudio.ai;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Port that bounded contexts use to talk to a language model. Contexts own their prompts;
 * the adapter behind this interface owns the vendor SDK, so no SDK type leaks into a domain.
 */
public interface LlmGateway {

    /**
     * Streams the model's text as it is generated. The caller must close the stream
     * (use try-with-resources) so the HTTP connection is released.
     *
     * @throws LlmUnavailableException when the model cannot be reached or rejects the request
     */
    Stream<String> stream(LlmRequest request);

    /** Waits for the full response. */
    default String complete(LlmRequest request) {
        try (Stream<String> parts = stream(request)) {
            return parts.collect(Collectors.joining());
        }
    }
}
