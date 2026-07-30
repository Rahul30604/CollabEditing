package com.collabeditor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EditingKeyResponse {
    private Long documentId;
    private Long holderId;
    private String holderName;
    private boolean granted;
    private int queuePosition;
    private String message;
}
