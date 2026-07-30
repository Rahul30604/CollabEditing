package com.collabeditor.vector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChromaQueryResult {
    private String id;
    private String document;
    private Map<String, Object> metadata;
    private double distance;
}
