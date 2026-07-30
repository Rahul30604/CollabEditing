package com.collabeditor.vector;

import com.collabeditor.config.AiConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChromaDbClient {

    private final WebClient chromaDbWebClient;
    private final AiConfig aiConfig;

    private String collectionId;

    @PostConstruct
    public void init() {
        try {
            ensureCollection();
        } catch (Exception e) {
            log.warn("ChromaDB not available yet. Will retry on first use. Error: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void ensureCollection() {
        String collectionName = aiConfig.getChromadb().getCollectionName();

        // Try to get existing collection
        try {
            Map<String, Object> response = chromaDbWebClient.get()
                    .uri("/api/v1/collections/{name}", collectionName)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("id")) {
                collectionId = (String) response.get("id");
                log.info("Using existing ChromaDB collection: {} ({})", collectionName, collectionId);
                return;
            }
        } catch (WebClientResponseException.NotFound e) {
            // Collection doesn't exist, create it
        } catch (Exception e) {
            // ChromaDB 0.5.0 returns 500 when collection doesn't exist
            log.info("Collection not found ({}), creating...", e.getMessage());
        }

        // Create collection
        Map<String, Object> createBody = Map.of(
                "name", collectionName,
                "metadata", Map.of("description", "CollabEditor document embeddings")
        );

        Map<String, Object> response = chromaDbWebClient.post()
                .uri("/api/v1/collections")
                .bodyValue(createBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response != null && response.containsKey("id")) {
            collectionId = (String) response.get("id");
            log.info("Created ChromaDB collection: {} ({})", collectionName, collectionId);
        } else {
            throw new RuntimeException("Failed to create ChromaDB collection");
        }
    }

    private String getCollectionId() {
        if (collectionId == null) {
            ensureCollection();
        }
        return collectionId;
    }

    /**
     * Upsert embeddings into ChromaDB.
     */
    public void upsert(List<String> ids, List<List<Double>> embeddings,
                       List<Map<String, String>> metadatas, List<String> documents) {

        Map<String, Object> body = new HashMap<>();
        body.put("ids", ids);
        body.put("embeddings", embeddings);
        body.put("metadatas", metadatas);
        body.put("documents", documents);

        chromaDbWebClient.post()
                .uri("/api/v1/collections/{id}/upsert", getCollectionId())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block();

        log.info("Upserted {} embeddings into ChromaDB", ids.size());
    }

    /**
     * Delete all embeddings for a document.
     */
    public void deleteByDocumentId(Long documentId) {
        Map<String, Object> body = Map.of(
                "where", Map.of("document_id", documentId.toString())
        );

        chromaDbWebClient.post()
                .uri("/api/v1/collections/{id}/delete", getCollectionId())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block();

        log.info("Deleted embeddings for document {}", documentId);
    }

    /**
     * Query similar embeddings.
     */
    @SuppressWarnings("unchecked")
    public List<ChromaQueryResult> query(List<Double> queryEmbedding, int topK, Map<String, String> whereFilter) {
        Map<String, Object> body = new HashMap<>();
        body.put("query_embeddings", List.of(queryEmbedding));
        body.put("n_results", topK);
        body.put("include", List.of("documents", "metadatas", "distances"));

        if (whereFilter != null && !whereFilter.isEmpty()) {
            body.put("where", whereFilter);
        }

        Map<String, Object> response = chromaDbWebClient.post()
                .uri("/api/v1/collections/{id}/query", getCollectionId())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            return List.of();
        }

        List<List<String>> ids = (List<List<String>>) response.get("ids");
        List<List<String>> documents = (List<List<String>>) response.get("documents");
        List<List<Map<String, Object>>> metadatas = (List<List<Map<String, Object>>>) response.get("metadatas");
        List<List<Double>> distances = (List<List<Double>>) response.get("distances");

        if (ids == null || ids.isEmpty() || ids.get(0).isEmpty()) {
            return List.of();
        }

        List<ChromaQueryResult> results = new ArrayList<>();
        for (int i = 0; i < ids.get(0).size(); i++) {
            results.add(ChromaQueryResult.builder()
                    .id(ids.get(0).get(i))
                    .document(documents != null ? documents.get(0).get(i) : null)
                    .metadata(metadatas != null ? metadatas.get(0).get(i) : null)
                    .distance(distances != null ? distances.get(0).get(i) : 0.0)
                    .build());
        }

        return results;
    }
}
