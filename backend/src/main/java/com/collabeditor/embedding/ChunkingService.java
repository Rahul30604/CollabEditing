package com.collabeditor.embedding;

import com.collabeditor.config.AiConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChunkingService {

    private final AiConfig aiConfig;

    /**
     * Split document content into overlapping chunks based on word count.
     */
    public List<DocumentChunk> chunkDocument(Long documentId, String title, String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        int chunkSize = aiConfig.getEmbedding().getChunkSize();
        int overlap = aiConfig.getEmbedding().getChunkOverlap();

        String[] words = content.split("\\s+");
        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;

        for (int i = 0; i < words.length; i += (chunkSize - overlap)) {
            int end = Math.min(i + chunkSize, words.length);
            StringBuilder chunkText = new StringBuilder();
            for (int j = i; j < end; j++) {
                if (j > i) chunkText.append(" ");
                chunkText.append(words[j]);
            }

            chunks.add(DocumentChunk.builder()
                    .documentId(documentId)
                    .chunkIndex(chunkIndex)
                    .chunkId(documentId + "_chunk_" + chunkIndex)
                    .title(title)
                    .text(chunkText.toString())
                    .build());

            chunkIndex++;

            if (end >= words.length) break;
        }

        return chunks;
    }
}
