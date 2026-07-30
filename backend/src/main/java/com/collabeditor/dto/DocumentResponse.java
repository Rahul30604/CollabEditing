package com.collabeditor.dto;

import com.collabeditor.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentResponse {
    private Long id;
    private String title;
    private String content;
    private Long ownerId;
    private String ownerName;
    private Role currentUserRole;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
