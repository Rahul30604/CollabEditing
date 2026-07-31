package com.collabeditor.controller;

import com.collabeditor.dto.AiQuestionRequest;
import com.collabeditor.dto.AiSearchRequest;
import com.collabeditor.entity.Document;
import com.collabeditor.rag.AiResponse;
import com.collabeditor.rag.RagService;
import com.collabeditor.rag.SearchResult;
import com.collabeditor.repository.DocumentRepository;
import com.collabeditor.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AiController {

    private final RagService ragService;
    private final DocumentRepository documentRepository;

    /**
     * Ask a question about a specific document.
     */
    @PostMapping("/documents/{documentId}/ask")
    public ResponseEntity<AiResponse> askQuestion(
            @PathVariable Long documentId,
            @Valid @RequestBody AiQuestionRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        // Verify user has access to this document
        verifyAccess(documentId, userDetails.getId());

        AiResponse response = ragService.askQuestion(documentId, request.getQuestion(), request.getDraftContent());
        return ResponseEntity.ok(response);
    }

    /**
     * Summarize a document.
     */
    @PostMapping("/documents/{documentId}/summary")
    public ResponseEntity<AiResponse> summarizeDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        verifyAccess(documentId, userDetails.getId());

        AiResponse response = ragService.summarizeDocument(documentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Semantic search across all accessible documents.
     */
    @PostMapping("/search")
    public ResponseEntity<List<SearchResult>> semanticSearch(
            @Valid @RequestBody AiSearchRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        // Get all document IDs the user can access
        List<Long> accessibleDocIds = documentRepository.findAccessibleByUserId(userDetails.getId())
                .stream()
                .map(Document::getId)
                .toList();

        List<SearchResult> results = ragService.semanticSearch(request.getQuery(), accessibleDocIds);
        return ResponseEntity.ok(results);
    }

    private void verifyAccess(Long documentId, Long userId) {
        List<Long> accessibleDocIds = documentRepository.findAccessibleByUserId(userId)
                .stream()
                .map(Document::getId)
                .toList();

        if (!accessibleDocIds.contains(documentId)) {
            throw new RuntimeException("You don't have access to this document");
        }
    }
}
