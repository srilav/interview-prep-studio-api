package dev.prepstudio.ai;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Deterministic stand-in for tests and offline development (prepstudio.llm.provider=fake).
 * Echoes the last user turn inside a response shaped like the real prompts ask for.
 */
public class FakeLlmGateway implements LlmGateway {

    @Override
    public Stream<String> stream(LlmRequest request) {
        String topic = request.turns().get(request.turns().size() - 1).text().lines().findFirst().orElse("");
        String text = """
                ## Summary
                Offline answer for: %s

                ### SHORT
                Placeholder definition produced by the fake model.

                ### DETAIL
                Set prepstudio.llm.provider=claude and ANTHROPIC_API_KEY to get real content.

                ### JAVA
                ```java
                System.out.println("fake model");
                ```
                """.formatted(topic);
        return Arrays.stream(text.split("(?<= )"));
    }
}
