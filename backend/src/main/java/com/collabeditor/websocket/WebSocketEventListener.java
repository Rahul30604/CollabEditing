package com.collabeditor.websocket;

import com.collabeditor.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final EditingKeyManager editingKeyManager;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        if (event.getUser() instanceof UsernamePasswordAuthenticationToken auth) {
            UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
            log.info("User disconnected: {} (ID: {})", user.getName(), user.getId());

            // Release any keys held by this user and reassign
            Map<Long, Long> reassignments = editingKeyManager.removeUser(user.getId());

            // Notify documents about key changes
            reassignments.forEach((documentId, nextUserId) -> {
                log.info("Key for document {} reassigned to user {} after disconnect", documentId, nextUserId);
                messagingTemplate.convertAndSend(
                        "/topic/document/" + documentId + "/key",
                        Map.of(
                                "documentId", documentId,
                                "holderId", nextUserId,
                                "message", "Key reassigned due to disconnect"
                        )
                );
            });
        }
    }
}
