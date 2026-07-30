package com.collabeditor.controller;

import com.collabeditor.dto.PermissionResponse;
import com.collabeditor.dto.ShareDocumentRequest;
import com.collabeditor.security.UserDetailsImpl;
import com.collabeditor.service.ShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents/{documentId}/share")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    @PostMapping
    public ResponseEntity<PermissionResponse> shareDocument(
            @PathVariable Long documentId,
            @Valid @RequestBody ShareDocumentRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(shareService.shareDocument(documentId, request, userDetails.getId()));
    }

    @GetMapping
    public ResponseEntity<List<PermissionResponse>> getPermissions(
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(shareService.getDocumentPermissions(documentId, userDetails.getId()));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removePermission(
            @PathVariable Long documentId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        shareService.removePermission(documentId, userId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
