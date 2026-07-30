package com.collabeditor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConfigurationProperties(prefix = "app.ai")
@Data
public class AiConfig {

    private String provider = "openai";
    private OpenAiProperties openai = new OpenAiProperties();
    private GeminiProperties gemini = new GeminiProperties();
    private ChromaDbProperties chromadb = new ChromaDbProperties();
    private EmbeddingProperties embedding = new EmbeddingProperties();

    @Data
    public static class OpenAiProperties {
        private String apiKey;
        private String model = "gpt-4.1";
        private String embeddingModel = "text-embedding-3-small";
        private String baseUrl = "https://api.openai.com/v1";
    }

    @Data
    public static class GeminiProperties {
        private String apiKey;
        private String model = "gemini-2.0-flash";
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    }

    @Data
    public static class ChromaDbProperties {
        private String baseUrl = "http://localhost:8000";
        private String collectionName = "collabeditor-documents";
    }

    @Data
    public static class EmbeddingProperties {
        private int chunkSize = 500;
        private int chunkOverlap = 100;
        private int topK = 5;
    }

    @Bean
    public WebClient openAiWebClient() {
        return WebClient.builder()
                .baseUrl(openai.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + openai.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    @Bean
    public WebClient chromaDbWebClient() {
        return WebClient.builder()
                .baseUrl(chromadb.getBaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
}
