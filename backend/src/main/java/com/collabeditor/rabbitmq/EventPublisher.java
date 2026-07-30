package com.collabeditor.rabbitmq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishDocumentSaved(Long documentId, Long userId, String userName, String title) {
        DocumentEvent event = DocumentEvent.builder()
                .documentId(documentId)
                .userId(userId)
                .userName(userName)
                .eventType("SAVED")
                .title(title)
                .timestamp(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.DOCUMENT_SAVED_KEY, event);
        log.info("Published SAVED event for document {} by user {}", documentId, userName);
    }

    public void publishDocumentUpdated(Long documentId, Long userId, String userName, String title) {
        DocumentEvent event = DocumentEvent.builder()
                .documentId(documentId)
                .userId(userId)
                .userName(userName)
                .eventType("UPDATED")
                .title(title)
                .timestamp(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.DOCUMENT_UPDATED_KEY, event);
        log.info("Published UPDATED event for document {} by user {}", documentId, userName);
    }

    public void publishNotification(Long documentId, Long userId, String userName, String eventType, String title) {
        DocumentEvent event = DocumentEvent.builder()
                .documentId(documentId)
                .userId(userId)
                .userName(userName)
                .eventType(eventType)
                .title(title)
                .timestamp(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.NOTIFICATION_KEY, event);
        log.info("Published notification: {} for document {} by user {}", eventType, documentId, userName);
    }

    public void publishEmbeddingGeneration(Long documentId) {
        DocumentEvent event = DocumentEvent.builder()
                .documentId(documentId)
                .eventType("EMBEDDING")
                .timestamp(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.EMBEDDING_KEY, event);
        log.info("Published EMBEDDING event for document {}", documentId);
    }
}
