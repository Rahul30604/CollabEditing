package com.collabeditor.dto;

import com.collabeditor.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class PermissionResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private Role role;
    private LocalDateTime createdAt;
}
