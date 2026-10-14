package dev.prepstudio.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.RawMessageStreamEvent;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Adapter for the Claude Messages API. The client is created on first use, so the application
 * starts without credentials; requests then fail with {@link LlmUnavailableException}.
 * Credentials come from ANTHROPIC_API_KEY (or an `ant auth login` profile).
 */
public class ClaudeLlmGateway implements LlmGateway {

    private final String model;
    private final Supplier<AnthropicClient> client;

    public ClaudeLlmGateway(String model) {
        this(model, AnthropicOkHttpClient::fromEnv);
    }

    ClaudeLlmGateway(String model, Supplier<AnthropicClient> clientFactory) {
        this.model = model;
        this.client = memoize(clientFactory);
    }

    @Override
    public Stream<String> stream(LlmRequest request) {
        MessageCreateParams.Builder params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(request.maxTokens())
                .system(request.system());
        for (LlmTurn turn : request.turns()) {
            if (turn.speaker() == LlmTurn.Speaker.USER) params.addUserMessage(turn.text());
            else params.addAssistantMessage(turn.text());
        }

        StreamResponse<RawMessageStreamEvent> response;
        try {
            response = client.get().messages().createStreaming(params.build());
        } catch (RuntimeException e) {
            throw new LlmUnavailableException("Claude request failed: " + e.getMessage(), e);
        }
        return response.stream()
                .flatMap(event -> event.contentBlockDelta().stream())
                .flatMap(delta -> delta.delta().text().stream())
                .map(text -> text.text())
                .onClose(response::close);
    }

    private static <T> Supplier<T> memoize(Supplier<T> factory) {
        return new Supplier<>() {
            private volatile T value;

            @Override
            public T get() {
                T v = value;
                if (v == null) {
                    synchronized (this) {
                        if (value == null) value = factory.get();
                        v = value;
                    }
                }
                return v;
            }
        };
    }
}
