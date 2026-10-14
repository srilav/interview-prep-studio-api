package dev.prepstudio.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfiguration {

    @Bean
    @ConditionalOnProperty(name = "prepstudio.llm.provider", havingValue = "claude", matchIfMissing = true)
    LlmGateway claudeLlmGateway(@Value("${prepstudio.llm.model:claude-haiku-5-5}") String model) {
        return new ClaudeLlmGateway(model);
    }

    @Bean
    @ConditionalOnProperty(name = "prepstudio.llm.provider", havingValue = "fake")
    LlmGateway fakeLlmGateway() {
        return new FakeLlmGateway();
    }
}
