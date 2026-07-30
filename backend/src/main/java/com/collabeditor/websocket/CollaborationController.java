package com.collabeditor.websocket;

import com.collabeditor.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CollaborationController {

    private final SimpMessagingTemplate messagingTemplate;
    private final EditingKeyManager editingKeyManager;

    /**
     * Handle content updates from the key holder.
     * Only the key holder's edits are broadcast to others.
     */
    @MessageMapping("/document/{documentId}/edit")
    public void handleEdit(
            @DestinationVariable Long documentId,
            @Payload DocumentEditMessage message,
            Principal principal) {

        UserDetailsImpl user = extractUser(principal);
        if (user == null) return;

        // Only the key holder can broadcast edits
        if (!editingKeyManager.isKeyHolder(documentId, user.getId())) {
            log.warn("User {} attempted to edit document {} without key", user.getId(), documentId);
            return;
        }

        // Enrich message with sender info
        message.setUserId(user.getId());
        message.setUserName(user.getName());
        message.setTimestamp(LocalDateTime.now());

        // Broadcast to all subscribers except sender
        messagingTemplate.convertAndSend("/topic/document/" + documentId + "/edits", message);
        log.debug("Broadcast edit from user {} on document {}", user.getName(), documentId);
    }

    /**
     * Handle cursor position updates.
     * Any connected user can share their cursor position.
     */
    @MessageMapping("/document/{documentId}/cursor")
    public void handleCursor(
            @DestinationVariable Long documentId,
            @Payload CursorMessage message,
            Principal principal) {

        UserDetailsImpl user = extractUser(principal);
        if (user == null) return;

        message.setUserId(user.getId());
        message.setUserName(user.getName());
        message.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/document/" + documentId + "/cursors", message);
    }

    /**
     * Handle user join/leave notifications for a document.
     */
    @MessageMapping("/document/{documentId}/presence")
    public void handlePresence(
            @DestinationVariable Long documentId,
            @Payload PresenceMessage message,
            Principal principal) {

        UserDetailsImpl user = extractUser(principal);
        if (user == null) return;

        message.setUserId(user.getId());
        message.setUserName(user.getName());
        message.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/document/" + documentId + "/presence", message);
        log.info("User {} {} document {}", user.getName(), message.getAction(), documentId);
    }

    private UserDetailsImpl extractUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            return (UserDetailsImpl) auth.getPrincipal();
        }
        return null;
    }
}
