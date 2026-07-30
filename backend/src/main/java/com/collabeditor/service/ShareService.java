package com.collabeditor.service;

import com.collabeditor.dto.PermissionResponse;
import com.collabeditor.dto.ShareDocumentRequest;
import com.collabeditor.entity.Document;
import com.collabeditor.entity.DocumentPermission;
import com.collabeditor.entity.Role;
import com.collabeditor.entity.User;
import com.collabeditor.repository.DocumentPermissionRepository;
import com.collabeditor.repository.DocumentRepository;
import com.collabeditor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final DocumentRepository documentRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final UserRepository userRepository;

    @Transactional
    public PermissionResponse shareDocument(Long documentId, ShareDocumentRequest request, Long currentUserId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Only owner can share
        if (!document.getOwner().getId().equals(currentUserId)) {
            throw new RuntimeException("Only the document owner can share");
        }

        // Cannot share with yourself
        User targetUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User with email '" + request.getEmail() + "' not found"));

        if (targetUser.getId().equals(currentUserId)) {
            throw new RuntimeException("Cannot share document with yourself");
        }

        // Cannot assign OWNER role
        if (request.getRole() == Role.OWNER) {
            throw new RuntimeException("Cannot assign OWNER role. Transfer ownership instead.");
        }

        // Check if permission already exists, update if so
        Optional<DocumentPermission> existing = permissionRepository.findByDocumentIdAndUserId(documentId, targetUser.getId());

        DocumentPermission permission;
        if (existing.isPresent()) {
            permission = existing.get();
            permission.setRole(request.getRole());
        } else {
            permission = DocumentPermission.builder()
                    .document(document)
                    .user(targetUser)
                    .role(request.getRole())
                    .build();
        }

        permission = permissionRepository.save(permission);
        return toResponse(permission);
    }

    public List<PermissionResponse> getDocumentPermissions(Long documentId, Long currentUserId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Only owner can see all permissions
        if (!document.getOwner().getId().equals(currentUserId)) {
            throw new RuntimeException("Only the document owner can view permissions");
        }

        return permissionRepository.findByDocumentId(documentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removePermission(Long documentId, Long targetUserId, Long currentUserId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getOwner().getId().equals(currentUserId)) {
            throw new RuntimeException("Only the document owner can remove permissions");
        }

        if (targetUserId.equals(currentUserId)) {
            throw new RuntimeException("Cannot remove your own permission");
        }

        DocumentPermission permission = permissionRepository.findByDocumentIdAndUserId(documentId, targetUserId)
                .orElseThrow(() -> new RuntimeException("Permission not found"));

        permissionRepository.delete(permission);
    }

    private PermissionResponse toResponse(DocumentPermission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .userId(permission.getUser().getId())
                .userName(permission.getUser().getName())
                .userEmail(permission.getUser().getEmail())
                .role(permission.getRole())
                .createdAt(permission.getCreatedAt())
                .build();
    }
}
