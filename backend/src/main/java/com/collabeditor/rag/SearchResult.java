package com.collabeditor.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchResult {
    private Long documentId;
    private String title;
    private String snippet;
    private double relevanceScore;
}
