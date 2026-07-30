package com.collabeditor.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PresenceMessage {
    private Long documentId;
    private Long userId;
    private String userName;
    private String action; // JOINED, LEFT
    private LocalDateTime timestamp;
}
