package com.collabeditor.service;

import com.collabeditor.dto.VersionCompareResponse;
import com.collabeditor.dto.VersionResponse;
import com.collabeditor.entity.Document;
import com.collabeditor.entity.DocumentVersion;
import com.collabeditor.entity.User;
import com.collabeditor.repository.DocumentRepository;
import com.collabeditor.repository.DocumentVersionRepository;
import com.collabeditor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VersionService {

    private final DocumentVersionRepository versionRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    /**
     * Save a new version snapshot of the document.
     * Called whenever a user saves the document.
     */
    @Transactional
    public VersionResponse saveVersion(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Integer nextVersion = versionRepository.findMaxVersionByDocumentId(documentId) + 1;

        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .version(nextVersion)
                .title(document.getTitle())
                .content(document.getContent())
                .modifiedBy(user)
                .build();

        version = versionRepository.save(version);
        return toResponse(version);
    }

    /**
     * Get all versions of a document (metadata only, no content for list).
     */
    public List<VersionResponse> getVersionHistory(Long documentId, Long userId) {
        // Verify access
        documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        return versionRepository.findByDocumentIdOrderByVersionDesc(documentId).stream()
                .map(this::toResponseWithoutContent)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific version with full content.
     */
    public VersionResponse getVersion(Long documentId, Integer versionNumber) {
        DocumentVersion version = versionRepository.findByDocumentIdAndVersion(documentId, versionNumber)
                .orElseThrow(() -> new RuntimeException("Version " + versionNumber + " not found"));

        return toResponse(version);
    }

    /**
     * Restore a document to a specific version.
     */
    @Transactional
    public VersionResponse restoreVersion(Long documentId, Integer versionNumber, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Only the owner can restore versions");
        }

        DocumentVersion version = versionRepository.findByDocumentIdAndVersion(documentId, versionNumber)
                .orElseThrow(() -> new RuntimeException("Version " + versionNumber + " not found"));

        // Update document to the restored version's content
        document.setTitle(version.getTitle());
        document.setContent(version.getContent());
        documentRepository.save(document);

        // Save a new version marking the restore
        return saveVersion(documentId, userId);
    }

    /**
     * Compare two versions.
     */
    public VersionCompareResponse compareVersions(Long documentId, Integer versionA, Integer versionB) {
        DocumentVersion a = versionRepository.findByDocumentIdAndVersion(documentId, versionA)
                .orElseThrow(() -> new RuntimeException("Version " + versionA + " not found"));

        DocumentVersion b = versionRepository.findByDocumentIdAndVersion(documentId, versionB)
                .orElseThrow(() -> new RuntimeException("Version " + versionB + " not found"));

        return VersionCompareResponse.builder()
                .versionA(toResponse(a))
                .versionB(toResponse(b))
                .contentA(a.getContent())
                .contentB(b.getContent())
                .build();
    }

    private VersionResponse toResponse(DocumentVersion version) {
        return VersionResponse.builder()
                .id(version.getId())
                .documentId(version.getDocument().getId())
                .version(version.getVersion())
                .title(version.getTitle())
                .content(version.getContent())
                .modifiedById(version.getModifiedBy().getId())
                .modifiedByName(version.getModifiedBy().getName())
                .createdAt(version.getCreatedAt())
                .build();
    }

    private VersionResponse toResponseWithoutContent(DocumentVersion version) {
        return VersionResponse.builder()
                .id(version.getId())
                .documentId(version.getDocument().getId())
                .version(version.getVersion())
                .title(version.getTitle())
                .content(null)
                .modifiedById(version.getModifiedBy().getId())
                .modifiedByName(version.getModifiedBy().getName())
                .createdAt(version.getCreatedAt())
                .build();
    }
}
