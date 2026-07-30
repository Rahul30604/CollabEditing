package com.collabeditor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VersionResponse {
    private Long id;
    private Long documentId;
    private Integer version;
    private String title;
    private String content;
    private Long modifiedById;
    private String modifiedByName;
    private LocalDateTime createdAt;
}
