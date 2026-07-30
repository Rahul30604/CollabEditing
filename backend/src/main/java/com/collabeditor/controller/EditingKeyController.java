package com.collabeditor.controller;

import com.collabeditor.dto.EditingKeyResponse;
import com.collabeditor.entity.User;
import com.collabeditor.repository.UserRepository;
import com.collabeditor.security.UserDetailsImpl;
import com.collabeditor.websocket.EditingKeyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents/{documentId}/key")
@RequiredArgsConstructor
public class EditingKeyController {

    private final EditingKeyManager editingKeyManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @PostMapping("/request")
    public ResponseEntity<EditingKeyResponse> requestKey(
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        boolean granted = editingKeyManager.requestKey(documentId, userDetails.getId());

        EditingKeyResponse response;
        if (granted) {
            response = EditingKeyResponse.builder()
                    .documentId(documentId)
                    .holderId(userDetails.getId())
                    .holderName(userDetails.getName())
                    .granted(true)
                    .queuePosition(0)
                    .message("Editing key acquired")
                    .build();

            // Notify other users via WebSocket
            broadcastKeyStatus(documentId, userDetails.getId(), userDetails.getName());
        } else {
            Long holderId = editingKeyManager.getKeyHolder(documentId);
            String holderName = getUserName(holderId);
            int queueSize = editingKeyManager.getQueueSize(documentId);

            response = EditingKeyResponse.builder()
                    .documentId(documentId)
                    .holderId(holderId)
                    .holderName(holderName)
                    .granted(false)
                    .queuePosition(queueSize)
                    .message("Key held by " + holderName + ". Added to queue.")
                    .build();

            // Notify the current key holder that someone is requesting
            broadcastKeyRequest(documentId, userDetails.getId(), userDetails.getName());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/release")
    public ResponseEntity<EditingKeyResponse> releaseKey(
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Long nextUserId = editingKeyManager.releaseKey(documentId, userDetails.getId());

        EditingKeyResponse response;
        if (nextUserId != null) {
            String nextUserName = getUserName(nextUserId);
            response = EditingKeyResponse.builder()
                    .documentId(documentId)
                    .holderId(nextUserId)
                    .holderName(nextUserName)
                    .granted(false)
                    .queuePosition(0)
                    .message("Key released. Assigned to " + nextUserName)
                    .build();

            // Notify the next user and all others
            broadcastKeyStatus(documentId, nextUserId, nextUserName);
        } else {
            response = EditingKeyResponse.builder()
                    .documentId(documentId)
                    .holderId(null)
                    .holderName(null)
                    .granted(false)
                    .queuePosition(0)
                    .message("Key released. No one in queue.")
                    .build();

            // Notify everyone key is free
            broadcastKeyStatus(documentId, null, null);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/decline/{requesterId}")
    public ResponseEntity<EditingKeyResponse> declineRequest(
            @PathVariable Long documentId,
            @PathVariable Long requesterId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        // Only the current key holder can decline
        if (!editingKeyManager.isKeyHolder(documentId, userDetails.getId())) {
            throw new RuntimeException("Only the key holder can decline requests");
        }

        // Remove requester from queue
        editingKeyManager.forceRelease(documentId, requesterId);

        // Notify the requester that their request was declined
        Map<String, Object> notification = Map.of(
                "type", "KEY_DECLINED",
                "documentId", documentId,
                "message", "Your key request was declined"
        );
        messagingTemplate.convertAndSend("/topic/document/" + documentId + "/key-request", notification);

        return ResponseEntity.ok(EditingKeyResponse.builder()
                .documentId(documentId)
                .holderId(userDetails.getId())
                .holderName(userDetails.getName())
                .granted(true)
                .queuePosition(editingKeyManager.getQueueSize(documentId))
                .message("Request declined")
                .build());
    }

    @GetMapping("/status")
    public ResponseEntity<EditingKeyResponse> getKeyStatus(
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Long holderId = editingKeyManager.getKeyHolder(documentId);
        String holderName = holderId != null ? getUserName(holderId) : null;
        boolean isHolder = holderId != null && holderId.equals(userDetails.getId());

        return ResponseEntity.ok(EditingKeyResponse.builder()
                .documentId(documentId)
                .holderId(holderId)
                .holderName(holderName)
                .granted(isHolder)
                .queuePosition(editingKeyManager.getQueueSize(documentId))
                .message(holderId == null ? "Key is available" : "Key held by " + holderName)
                .build());
    }

    private void broadcastKeyStatus(Long documentId, Long holderId, String holderName) {
        EditingKeyResponse status = EditingKeyResponse.builder()
                .documentId(documentId)
                .holderId(holderId)
                .holderName(holderName)
                .granted(false)
                .queuePosition(editingKeyManager.getQueueSize(documentId))
                .message(holderId == null ? "Key is available" : "Key held by " + holderName)
                .build();

        messagingTemplate.convertAndSend("/topic/document/" + documentId + "/key", status);
    }

    private void broadcastKeyRequest(Long documentId, Long requesterId, String requesterName) {
        Map<String, Object> notification = Map.of(
                "type", "KEY_REQUEST",
                "documentId", documentId,
                "requesterId", requesterId,
                "requesterName", requesterName,
                "message", requesterName + " is requesting the editing key"
        );

        messagingTemplate.convertAndSend("/topic/document/" + documentId + "/key-request", notification);
    }

    private String getUserName(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(User::getName)
                .orElse("Unknown");
    }
}
