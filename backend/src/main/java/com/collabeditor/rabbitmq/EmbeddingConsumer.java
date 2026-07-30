package com.collabeditor.rabbitmq;

import com.collabeditor.embedding.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingConsumer {

    private final EmbeddingService embeddingService;

    @RabbitListener(queues = RabbitMQConfig.EMBEDDING_QUEUE)
    public void handleEmbeddingGeneration(DocumentEvent event) {
        Long documentId = event.getDocumentId();
        log.info("Received embedding generation event for document {}", documentId);

        try {
            embeddingService.indexDocument(documentId);
            log.info("Successfully indexed document {} into ChromaDB", documentId);
        } catch (Exception e) {
            log.error("Failed to generate embeddings for document {}: {}", documentId, e.getMessage());
            // Don't rethrow - message will be acked. In production, use DLQ.
        }
    }
}
