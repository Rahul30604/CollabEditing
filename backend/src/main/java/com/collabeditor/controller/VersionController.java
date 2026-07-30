package com.collabeditor.controller;

import com.collabeditor.dto.VersionCompareResponse;
import com.collabeditor.dto.VersionResponse;
import com.collabeditor.security.UserDetailsImpl;
import com.collabeditor.service.VersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents/{documentId}/versions")
@RequiredArgsConstructor
public class VersionController {

    private final VersionService versionService;

    @GetMapping
    public ResponseEntity<List<VersionResponse>> getVersionHistory(
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(versionService.getVersionHistory(documentId, userDetails.getId()));
    }

    @GetMapping("/{version}")
    public ResponseEntity<VersionResponse> getVersion(
            @PathVariable Long documentId,
            @PathVariable Integer version) {
        return ResponseEntity.ok(versionService.getVersion(documentId, version));
    }

    @PostMapping("/{version}/restore")
    public ResponseEntity<VersionResponse> restoreVersion(
            @PathVariable Long documentId,
            @PathVariable Integer version,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(versionService.restoreVersion(documentId, version, userDetails.getId()));
    }

    @GetMapping("/compare")
    public ResponseEntity<VersionCompareResponse> compareVersions(
            @PathVariable Long documentId,
            @RequestParam Integer versionA,
            @RequestParam Integer versionB) {
        return ResponseEntity.ok(versionService.compareVersions(documentId, versionA, versionB));
    }
}
