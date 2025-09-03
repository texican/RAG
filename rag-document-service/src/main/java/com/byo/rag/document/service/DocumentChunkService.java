package com.byo.rag.document.service;

import com.byo.rag.document.repository.DocumentChunkRepository;
import com.byo.rag.shared.dto.TenantDto;
import com.byo.rag.shared.entity.Document;
import com.byo.rag.shared.entity.DocumentChunk;
import com.byo.rag.shared.util.TextChunker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DocumentChunkService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentChunkService.class);

    private final DocumentChunkRepository chunkRepository;

    public DocumentChunkService(DocumentChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    public List<DocumentChunk> createChunks(Document document, String extractedText, TenantDto.ChunkingConfig config) {
        logger.info("Creating chunks for document: {} using strategy: {}", document.getId(), config.strategy());

        List<String> textChunks = TextChunker.chunkText(extractedText, config);
        List<DocumentChunk> documentChunks = new ArrayList<>();

        for (int i = 0; i < textChunks.size(); i++) {
            String chunkText = textChunks.get(i);
            
            DocumentChunk chunk = new DocumentChunk();
            chunk.setContent(chunkText);
            chunk.setSequenceNumber(i);
            chunk.setTokenCount(TextChunker.estimateTokenCount(chunkText));
            chunk.setDocument(document);
            chunk.setTenant(document.getTenant());

            // Calculate start and end indices for fixed-size chunking
            if (config.strategy() == TenantDto.ChunkingStrategy.FIXED_SIZE) {
                int chunkStart = i * (config.chunkSize() - config.chunkOverlap());
                chunk.setStartIndex(chunkStart);
                chunk.setEndIndex(Math.min(chunkStart + chunkText.length(), extractedText.length()));
            }

            documentChunks.add(chunk);
        }

        documentChunks = chunkRepository.saveAll(documentChunks);
        
        logger.info("Created {} chunks for document: {}", documentChunks.size(), document.getId());

        return documentChunks;
    }

    @Transactional(readOnly = true)
    public List<DocumentChunk> getChunksByDocument(UUID documentId) {
        return chunkRepository.findByDocumentIdOrderBySequenceNumber(documentId);
    }

    @Transactional(readOnly = true)
    public List<DocumentChunk> getChunksWithoutEmbeddings(UUID tenantId, int limit) {
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(0, limit);
        return chunkRepository.findChunksWithoutEmbeddings(tenantId, pageable);
    }

    public void updateChunkEmbedding(UUID chunkId, String embeddingVectorId) {
        DocumentChunk chunk = chunkRepository.findById(chunkId)
            .orElseThrow(() -> new RuntimeException("Chunk not found: " + chunkId));
        
        chunk.setEmbeddingVectorId(embeddingVectorId);
        chunkRepository.save(chunk);
        
        logger.debug("Updated embedding for chunk: {}", chunkId);
    }

    public void deleteChunksByDocument(UUID documentId) {
        chunkRepository.deleteByDocumentId(documentId);
        logger.info("Deleted all chunks for document: {}", documentId);
    }

    @Transactional(readOnly = true)
    public long getChunkCountByDocument(UUID documentId) {
        return chunkRepository.countByDocumentId(documentId);
    }

    @Transactional(readOnly = true)
    public long getChunkCountByTenant(UUID tenantId) {
        return chunkRepository.countByTenantId(tenantId);
    }
}