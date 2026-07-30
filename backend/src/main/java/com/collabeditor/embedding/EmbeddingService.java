package com.collabeditor.embedding;

import com.collabeditor.ai.OpenAiClient;
import com.collabeditor.config.AiConfig;
import com.collabeditor.entity.Document;
import com.collabeditor.repository.DocumentRepository;
import com.collabeditor.vector.ChromaDbClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final ChunkingService chunkingService;
    private final OpenAiClient openAiClient;
    private final ChromaDbClient chromaDbClient;
    private final DocumentRepository documentRepository;
    private final AiConfig aiConfig;

    /**
     * Generate embeddings for a document and store in ChromaDB.
     * Called whenever a document is saved.
     */
    public void indexDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        String content = document.getContent();
        if (content == null || content.isBlank()) {
            // Delete existing embeddings if content is empty
            chromaDbClient.deleteByDocumentId(documentId);
            log.info("Document {} has no content, cleared embeddings", documentId);
            return;
        }

        // 1. Chunk the document
        List<DocumentChunk> chunks = chunkingService.chunkDocument(
                documentId, document.getTitle(), content);

        if (chunks.isEmpty()) {
            return;
        }

        log.info("Document {} chunked into {} pieces", documentId, chunks.size());

        // 2. Delete old embeddings for this document
        chromaDbClient.deleteByDocumentId(documentId);

        // 3. Generate embeddings in batches
        List<String> texts = chunks.stream().map(DocumentChunk::getText).toList();
        List<List<Double>> embeddings = generateEmbeddingsInBatches(texts);

        // 4. Prepare metadata and store in ChromaDB
        List<String> ids = new ArrayList<>();
        List<Map<String, String>> metadatas = new ArrayList<>();
        List<String> documents = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            ids.add(chunk.getChunkId());
            metadatas.add(Map.of(
                    "document_id", documentId.toString(),
                    "chunk_index", String.valueOf(chunk.getChunkIndex()),
                    "title", chunk.getTitle()
            ));
            documents.add(chunk.getText());
        }

        chromaDbClient.upsert(ids, embeddings, metadatas, documents);
        log.info("Indexed document {} with {} chunks into ChromaDB", documentId, chunks.size());
    }

    /**
     * Generate embedding for a query string (for search).
     */
    public List<Double> generateQueryEmbedding(String query) {
        return openAiClient.generateEmbedding(query);
    }

    private List<List<Double>> generateEmbeddingsInBatches(List<String> texts) {
        int batchSize = 20; // OpenAI allows up to 2048 inputs, but keep batches manageable
        List<List<Double>> allEmbeddings = new ArrayList<>();

        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);
            List<List<Double>> batchEmbeddings = openAiClient.generateEmbeddings(batch);
            allEmbeddings.addAll(batchEmbeddings);
        }

        return allEmbeddings;
    }
}
