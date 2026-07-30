package com.collabeditor.rabbitmq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.DOCUMENT_UPDATES_QUEUE)
    public void handleDocumentUpdate(DocumentEvent event) {
        log.info("Received document event: {} for document {}", event.getEventType(), event.getDocumentId());

        // Broadcast to all subscribers of this document
        messagingTemplate.convertAndSend(
                "/topic/document/" + event.getDocumentId() + "/events",
                event
        );
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATIONS_QUEUE)
    public void handleNotification(DocumentEvent event) {
        log.info("Received notification: {} for document {}", event.getEventType(), event.getDocumentId());

        // Broadcast to general notifications topic
        messagingTemplate.convertAndSend("/topic/notifications", event);
    }
}
