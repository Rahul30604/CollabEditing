package com.collabeditor.service;

import com.collabeditor.dto.CreateDocumentRequest;
import com.collabeditor.dto.DocumentResponse;
import com.collabeditor.dto.UpdateDocumentRequest;
import com.collabeditor.entity.Document;
import com.collabeditor.entity.DocumentPermission;
import com.collabeditor.entity.Role;
import com.collabeditor.entity.User;
import com.collabeditor.rabbitmq.EventPublisher;
import com.collabeditor.repository.DocumentPermissionRepository;
import com.collabeditor.repository.DocumentRepository;
import com.collabeditor.repository.DocumentVersionRepository;
import com.collabeditor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final DocumentVersionRepository versionRepository;
    private final UserRepository userRepository;
    private final VersionService versionService;
    private final EventPublisher eventPublisher;

    @Transactional
    public DocumentResponse createDocument(CreateDocumentRequest request, Long userId) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Document document = Document.builder()
                .title(request.getTitle())
                .content(request.getContent() != null ? request.getContent() : "")
                .owner(owner)
                .build();

        document = documentRepository.save(document);

        // Create OWNER permission entry
        DocumentPermission permission = DocumentPermission.builder()
                .document(document)
                .user(owner)
                .role(Role.OWNER)
                .build();
        permissionRepository.save(permission);

        return toResponse(document, Role.OWNER);
    }

    public List<DocumentResponse> getMyDocuments(Long userId) {
        List<Document> documents = documentRepository.findAccessibleByUserId(userId);
        return documents.stream()
                .map(doc -> {
                    Role role = getUserRole(doc.getId(), userId);
                    return toResponse(doc, role);
                })
                .collect(Collectors.toList());
    }

    @Cacheable(value = "documents", key = "#documentId + ':' + #userId")
    public DocumentResponse getDocument(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        Role role = getUserRole(documentId, userId);
        if (role == null) {
            throw new RuntimeException("You don't have access to this document");
        }

        return toResponse(document, role);
    }

    @Transactional
    @CacheEvict(value = {"documents", "ai-responses", "search-results"}, allEntries = true)
    public DocumentResponse updateDocument(Long documentId, UpdateDocumentRequest request, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        Role role = getUserRole(documentId, userId);
        if (role == null || role == Role.VIEWER) {
            throw new RuntimeException("You don't have permission to edit this document");
        }

        if (request.getTitle() != null) {
            document.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            document.setContent(request.getContent());
        }

        document = documentRepository.save(document);
        
        // Save version snapshot
        versionService.saveVersion(document.getId(), userId);
        
        // Publish event via RabbitMQ
        User user = userRepository.findById(userId).orElse(null);
        String userName = user != null ? user.getName() : "Unknown";
        eventPublisher.publishDocumentSaved(document.getId(), userId, userName, document.getTitle());
        
        // Trigger async embedding generation
        eventPublisher.publishEmbeddingGeneration(document.getId());
        
        return toResponse(document, role);
    }

    @Transactional
    @CacheEvict(value = {"documents", "ai-responses", "search-results"}, allEntries = true)
    public void deleteDocument(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Only the owner can delete this document");
        }

        permissionRepository.deleteByDocumentId(documentId);
        versionRepository.deleteByDocumentId(documentId);
        documentRepository.delete(document);
    }

    private Role getUserRole(Long documentId, Long userId) {
        // Check if user is the owner
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document != null && document.getOwner().getId().equals(userId)) {
            return Role.OWNER;
        }

        // Check permissions table
        return permissionRepository.findByDocumentIdAndUserId(documentId, userId)
                .map(DocumentPermission::getRole)
                .orElse(null);
    }

    private DocumentResponse toResponse(Document document, Role role) {
        return DocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .content(document.getContent())
                .ownerId(document.getOwner().getId())
                .ownerName(document.getOwner().getName())
                .currentUserRole(role)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
