package com.collabeditor.rabbitmq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentEvent {
    private Long documentId;
    private Long userId;
    private String userName;
    private String eventType; // SAVED, UPDATED, SHARED, DELETED
    private String title;
    private LocalDateTime timestamp;
}
