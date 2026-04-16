package com.cloudrishi.codebasechat.config;

import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration class for LangChain4j Ollama integration.
 * <p>
 * Registers the Ollama chat model as a Spring Bean, making it
 * available for dependency injection throughout the application.
 * The model connects to the locally running Ollama instance
 * and uses the configured LLM to generate context-aware answers.
 * </p>
 */
@Configuration
public class OllamaConfig {

    @Value("${ollama.base-url}")
    private String baseUrl;

    @Value("${ollama.model}")
    private String model;

    /**
     * Creates and configures the Ollama chat model bean.
     * <p>
     * Timeout is set to 120 seconds to accommodate larger models
     * like llama3.1:8b which may take time on first inference.
     * </p>
     *
     * @return a configured {@link OllamaChatModel} instance
     */
    @Bean
    public OllamaChatModel ollamaChatModel() {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(model)
                .timeout(Duration.ofSeconds(120))
                .build();
    }
}