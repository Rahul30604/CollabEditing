package com.collabeditor.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Slf4j
public class EditingKeyManager {

    // documentId -> userId who holds the key
    private final Map<Long, Long> keyHolders = new ConcurrentHashMap<>();

    // documentId -> queue of userIds waiting for the key
    private final Map<Long, Queue<Long>> waitingQueues = new ConcurrentHashMap<>();

    /**
     * Try to acquire the editing key for a document.
     * Returns true if the key was acquired, false if someone else holds it.
     */
    public synchronized boolean requestKey(Long documentId, Long userId) {
        Long currentHolder = keyHolders.get(documentId);

        if (currentHolder == null) {
            // Key is free, assign it
            keyHolders.put(documentId, userId);
            log.info("User {} acquired editing key for document {}", userId, documentId);
            return true;
        }

        if (currentHolder.equals(userId)) {
            // User already holds the key
            return true;
        }

        // Key is held by someone else, add to queue
        waitingQueues.computeIfAbsent(documentId, k -> new ConcurrentLinkedQueue<>());
        Queue<Long> queue = waitingQueues.get(documentId);
        if (!queue.contains(userId)) {
            queue.add(userId);
            log.info("User {} added to waiting queue for document {}", userId, documentId);
        }
        return false;
    }

    /**
     * Release the editing key. Returns the next user in queue (if any).
     */
    public synchronized Long releaseKey(Long documentId, Long userId) {
        Long currentHolder = keyHolders.get(documentId);

        if (currentHolder == null || !currentHolder.equals(userId)) {
            return null; // User doesn't hold the key
        }

        keyHolders.remove(documentId);
        log.info("User {} released editing key for document {}", userId, documentId);

        // Assign to next in queue
        Queue<Long> queue = waitingQueues.get(documentId);
        if (queue != null && !queue.isEmpty()) {
            Long nextUser = queue.poll();
            keyHolders.put(documentId, nextUser);
            log.info("Key for document {} assigned to next user {}", documentId, nextUser);
            return nextUser;
        }

        return null;
    }

    /**
     * Force release when a user disconnects.
     */
    public synchronized Long forceRelease(Long documentId, Long userId) {
        // Remove from waiting queue if present
        Queue<Long> queue = waitingQueues.get(documentId);
        if (queue != null) {
            queue.remove(userId);
        }

        // If user holds the key, release it
        Long currentHolder = keyHolders.get(documentId);
        if (currentHolder != null && currentHolder.equals(userId)) {
            return releaseKey(documentId, userId);
        }

        return null;
    }

    /**
     * Get who currently holds the key.
     */
    public Long getKeyHolder(Long documentId) {
        return keyHolders.get(documentId);
    }

    /**
     * Check if a specific user holds the key.
     */
    public boolean isKeyHolder(Long documentId, Long userId) {
        Long holder = keyHolders.get(documentId);
        return holder != null && holder.equals(userId);
    }

    /**
     * Get the waiting queue size for a document.
     */
    public int getQueueSize(Long documentId) {
        Queue<Long> queue = waitingQueues.get(documentId);
        return queue == null ? 0 : queue.size();
    }

    /**
     * Remove user from all documents (on disconnect).
     * Returns a map of documentId -> nextUserId for keys that were reassigned.
     */
    public synchronized Map<Long, Long> removeUser(Long userId) {
        Map<Long, Long> reassignments = new ConcurrentHashMap<>();

        // Remove from all waiting queues
        waitingQueues.values().forEach(queue -> queue.remove(userId));

        // Release any keys held by this user
        keyHolders.entrySet().stream()
                .filter(e -> e.getValue().equals(userId))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(docId -> {
                    Long nextUser = releaseKey(docId, userId);
                    if (nextUser != null) {
                        reassignments.put(docId, nextUser);
                    }
                });

        return reassignments;
    }
}
