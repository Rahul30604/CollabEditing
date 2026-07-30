package com.collabeditor.rag;

import com.collabeditor.ai.OpenAiClient;
import com.collabeditor.config.AiConfig;
import com.collabeditor.embedding.EmbeddingService;
import com.collabeditor.vector.ChromaDbClient;
import com.collabeditor.vector.ChromaQueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final EmbeddingService embeddingService;
    private final ChromaDbClient chromaDbClient;
    private final OpenAiClient openAiClient;
    private final AiConfig aiConfig;

    /**
     * Answer a question about a specific document using RAG.
     */
    @Cacheable(value = "ai-responses", key = "#documentId + ':ask:' + #question")
    public AiResponse askQuestion(Long documentId, String question) {
        // 1. Generate embedding for the question
        List<Double> queryEmbedding = embeddingService.generateQueryEmbedding(question);

        // 2. Search ChromaDB for relevant chunks (scoped to this document)
        List<ChromaQueryResult> results = chromaDbClient.query(
                queryEmbedding,
                aiConfig.getEmbedding().getTopK(),
                Map.of("document_id", documentId.toString())
        );

        if (results.isEmpty()) {
            return AiResponse.builder()
                    .answer("I don't have enough context from this document to answer your question. Make sure the document has been saved at least once.")
                    .sources(List.of())
                    .build();
        }

        // 3. Build context from retrieved chunks
        String context = results.stream()
                .map(ChromaQueryResult::getDocument)
                .collect(Collectors.joining("\n\n---\n\n"));

        // 4. Build prompt
        String systemPrompt = """
                You are an AI assistant helping users understand a document. 
                Answer questions based ONLY on the provided document context below.
                If the answer cannot be found in the context, say so clearly.
                Be concise and specific. Reference relevant parts of the document when possible.
                
                DOCUMENT CONTEXT:
                """ + context;

        // 5. Call LLM
        String answer = openAiClient.chatCompletion(systemPrompt, question);

        // 6. Build response with sources
        List<String> sources = results.stream()
                .map(r -> r.getDocument().length() > 100
                        ? r.getDocument().substring(0, 100) + "..."
                        : r.getDocument())
                .toList();

        return AiResponse.builder()
                .answer(answer)
                .sources(sources)
                .build();
    }

    /**
     * Summarize a document.
     */
    @Cacheable(value = "ai-responses", key = "#documentId + ':summary'")
    public AiResponse summarizeDocument(Long documentId) {
        // Get all chunks for this document
        List<Double> dummyEmbedding = embeddingService.generateQueryEmbedding("summarize this document");

        List<ChromaQueryResult> results = chromaDbClient.query(
                dummyEmbedding,
                20, // Get more chunks for summarization
                Map.of("document_id", documentId.toString())
        );

        if (results.isEmpty()) {
            return AiResponse.builder()
                    .answer("This document has no content to summarize.")
                    .sources(List.of())
                    .build();
        }

        String fullContent = results.stream()
                .map(ChromaQueryResult::getDocument)
                .collect(Collectors.joining("\n\n"));

        String systemPrompt = """
                You are an AI assistant that creates clear, well-structured document summaries.
                Summarize the following document content. Include:
                - A brief overview (2-3 sentences)
                - Key points or sections
                - Important details or findings
                
                Keep the summary concise but comprehensive.
                """;

        String answer = openAiClient.chatCompletion(systemPrompt, fullContent);

        return AiResponse.builder()
                .answer(answer)
                .sources(List.of())
                .build();
    }

    /**
     * Semantic search across all documents the user can access.
     */
    @Cacheable(value = "search-results", key = "#query")
    public List<SearchResult> semanticSearch(String query, List<Long> accessibleDocumentIds) {
        // 1. Generate embedding for query
        List<Double> queryEmbedding = embeddingService.generateQueryEmbedding(query);

        // 2. Search ChromaDB (no document filter - search across all)
        List<ChromaQueryResult> results = chromaDbClient.query(
                queryEmbedding,
                aiConfig.getEmbedding().getTopK() * 2, // Get more results for cross-doc search
                null // No filter - search all
        );

        // 3. Filter to only accessible documents and map to SearchResult
        return results.stream()
                .filter(r -> {
                    if (r.getMetadata() == null) return false;
                    Object docId = r.getMetadata().get("document_id");
                    if (docId == null) return false;
                    Long parsedId = Long.parseLong(docId.toString());
                    return accessibleDocumentIds.contains(parsedId);
                })
                .map(r -> SearchResult.builder()
                        .documentId(Long.parseLong(r.getMetadata().get("document_id").toString()))
                        .title(r.getMetadata().getOrDefault("title", "Untitled").toString())
                        .snippet(r.getDocument())
                        .relevanceScore(1.0 - r.getDistance()) // Convert distance to similarity
                        .build())
                .toList();
    }
}
