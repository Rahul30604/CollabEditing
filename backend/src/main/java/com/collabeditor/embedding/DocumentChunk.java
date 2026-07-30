package com.collabeditor.embedding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentChunk {
    private Long documentId;
    private int chunkIndex;
    private String chunkId;
    private String title;
    private String text;
}
