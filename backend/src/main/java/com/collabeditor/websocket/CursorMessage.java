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
public class CursorMessage {
    private Long documentId;
    private Long userId;
    private String userName;
    private int position;      // cursor character position
    private int selectionStart;
    private int selectionEnd;
    private LocalDateTime timestamp;
}
