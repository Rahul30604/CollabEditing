package com.collabeditor.ai;

import com.collabeditor.config.AiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiClient {

    private final WebClient openAiWebClient;
    private final AiConfig aiConfig;

    /**
     * Generate embeddings for a list of texts using OpenAI embeddings API.
     */
    @SuppressWarnings("unchecked")
    public List<List<Double>> generateEmbeddings(List<String> texts) {
        Map<String, Object> requestBody = Map.of(
                "input", texts,
                "model", aiConfig.getOpenai().getEmbeddingModel()
        );

        Map<String, Object> response = openAiWebClient.post()
                .uri("/embeddings")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("data")) {
            throw new RuntimeException("Failed to generate embeddings");
        }

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        return data.stream()
                .map(item -> (List<Double>) item.get("embedding"))
                .toList();
    }

    /**
     * Generate a single embedding for one text.
     */
    public List<Double> generateEmbedding(String text) {
        List<List<Double>> embeddings = generateEmbeddings(List.of(text));
        return embeddings.get(0);
    }

    /**
     * Chat completion call.
     */
    @SuppressWarnings("unchecked")
    public String chatCompletion(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = Map.of(
                "model", aiConfig.getOpenai().getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "temperature", 0.3,
                "max_tokens", 2000
        );

        Map<String, Object> response = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("choices")) {
            throw new RuntimeException("Failed to get chat completion");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
