package com.byo.rag.document.service;

import com.byo.rag.document.repository.DocumentChunkRepository;
import com.byo.rag.shared.dto.TenantDto;
import com.byo.rag.shared.entity.Document;
import com.byo.rag.shared.entity.DocumentChunk;
import com.byo.rag.shared.entity.Tenant;
import com.byo.rag.shared.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for DocumentChunkService covering chunking strategies and operations.
 * 
 * Test Categories:
 * - Text Chunking with Different Strategies (Fixed-size, Semantic, Sliding Window)
 * - Chunk Creation and Persistence
 * - Chunk Retrieval and Querying
 * - Embedding Management and Updates
 * - Chunk Deletion and Cleanup
 * - Statistics and Analytics
 * - Error Handling and Edge Cases
 */
@ExtendWith(MockitoExtension.class)
class DocumentChunkServiceTest {

    @Mock
    private DocumentChunkRepository chunkRepository;
    
    @InjectMocks
    private DocumentChunkService chunkService;

    private Document testDocument;
    private Tenant testTenant;
    private User testUser;
    private UUID documentId;
    private UUID tenantId;
    private UUID chunkId;
    private String extractedText;

    @BeforeEach
    void setUp() {
        documentId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        chunkId = UUID.randomUUID();

        testTenant = createTestTenant(tenantId);
        testUser = createTestUser(testTenant);
        testDocument = createTestDocument(documentId, testTenant, testUser);
        
        extractedText = "This is a sample document with multiple sentences. " +
                       "It contains various information that will be split into chunks. " +
                       "Each chunk should contain meaningful content for embedding generation. " +
                       "The chunking process should preserve context while maintaining optimal size.";
    }

    @Nested
    @DisplayName("Chunk Creation Tests")
    class ChunkCreationTests {

        @Test
        @DisplayName("Should create chunks with fixed-size strategy")
        void shouldCreateChunksWithFixedSizeStrategy() {
            // Given
            TenantDto.ChunkingConfig config = new TenantDto.ChunkingConfig(
                100, 20, TenantDto.ChunkingStrategy.FIXED_SIZE);
            
            List<DocumentChunk> savedChunks = createMockChunks(3);
            when(chunkRepository.saveAll(any())).thenReturn(savedChunks);

            // When
            List<DocumentChunk> result = chunkService.createChunks(testDocument, extractedText, config);

            // Then
            assertThat(result).hasSize(3);
            
            ArgumentCaptor<List<DocumentChunk>> chunksCaptor = ArgumentCaptor.forClass(List.class);
            verify(chunkRepository).saveAll(chunksCaptor.capture());
            
            List<DocumentChunk> capturedChunks = chunksCaptor.getValue();
            assertThat(capturedChunks).allSatisfy(chunk -> {
                assertThat(chunk.getDocument()).isEqualTo(testDocument);
                assertThat(chunk.getTenant()).isEqualTo(testTenant);
                assertThat(chunk.getContent()).isNotEmpty();
                assertThat(chunk.getTokenCount()).isGreaterThan(0);
            });

            // Verify fixed-size specific fields are set
            capturedChunks.forEach(chunk -> {
                assertThat(chunk.getStartIndex()).isNotNull();
                assertThat(chunk.getEndIndex()).isNotNull();
                assertThat(chunk.getEndIndex()).isGreaterThanOrEqualTo(chunk.getStartIndex());
            });
        }

        @Test
        @DisplayName("Should create chunks with semantic strategy")
        void shouldCreateChunksWithSemanticStrategy() {
            // Given
            TenantDto.ChunkingConfig config = new TenantDto.ChunkingConfig(
                512, 64, TenantDto.ChunkingStrategy.SEMANTIC);
            
            List<DocumentChunk> savedChunks = createMockChunks(2);
            when(chunkRepository.saveAll(any())).thenReturn(savedChunks);

            // When
            List<DocumentChunk> result = chunkService.createChunks(testDocument, extractedText, config);

            // Then
            assertThat(result).hasSize(2);
            
            ArgumentCaptor<List<DocumentChunk>> chunksCaptor = ArgumentCaptor.forClass(List.class);
            verify(chunkRepository).saveAll(chunksCaptor.capture());
            
            List<DocumentChunk> capturedChunks = chunksCaptor.getValue();
            assertThat(capturedChunks).allSatisfy(chunk -> {
                assertThat(chunk.getSequenceNumber()).isNotNull();
                assertThat(chunk.getContent()).isNotEmpty();
                assertThat(chunk.getTokenCount()).isGreaterThan(0);
            });
        }

        @Test
        @DisplayName("Should create chunks with sliding window strategy")
        void shouldCreateChunksWithSlidingWindowStrategy() {
            // Given
            TenantDto.ChunkingConfig config = new TenantDto.ChunkingConfig(
                256, 128, TenantDto.ChunkingStrategy.SLIDING_WINDOW);
            
            List<DocumentChunk> savedChunks = createMockChunks(4);
            when(chunkRepository.saveAll(any())).thenReturn(savedChunks);

            // When
            List<DocumentChunk> result = chunkService.createChunks(testDocument, extractedText, config);

            // Then
            assertThat(result).hasSize(4);
            verify(chunkRepository).saveAll(any());
        }

        @Test
        @DisplayName("Should handle empty text input")
        void shouldHandleEmptyTextInput() {
            // Given
            TenantDto.ChunkingConfig config = new TenantDto.ChunkingConfig(
                512, 64, TenantDto.ChunkingStrategy.SEMANTIC);
            String emptyText = "";
            
            when(chunkRepository.saveAll(any())).thenReturn(List.of());

            // When
            List<DocumentChunk> result = chunkService.createChunks(testDocument, emptyText, config);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should assign correct sequence numbers to chunks")
        void shouldAssignCorrectSequenceNumbersToChunks() {
            // Given
            TenantDto.ChunkingConfig config = new TenantDto.ChunkingConfig(
                100, 20, TenantDto.ChunkingStrategy.FIXED_SIZE);
            
            List<DocumentChunk> savedChunks = createMockChunks(3);
            when(chunkRepository.saveAll(any())).thenReturn(savedChunks);

            // When
            chunkService.createChunks(testDocument, extractedText, config);

            // Then
            ArgumentCaptor<List<DocumentChunk>> chunksCaptor = ArgumentCaptor.forClass(List.class);
            verify(chunkRepository).saveAll(chunksCaptor.capture());
            
            List<DocumentChunk> capturedChunks = chunksCaptor.getValue();
            for (int i = 0; i < capturedChunks.size(); i++) {
                assertThat(capturedChunks.get(i).getSequenceNumber()).isEqualTo(i);
            }
        }
    }

    @Nested
    @DisplayName("Chunk Retrieval Tests")
    class ChunkRetrievalTests {

        @Test
        @DisplayName("Should retrieve chunks by document ID ordered by sequence")
        void shouldRetrieveChunksByDocumentIdOrderedBySequence() {
            // Given
            List<DocumentChunk> expectedChunks = createMockChunks(3);
            when(chunkRepository.findByDocumentIdOrderBySequenceNumber(documentId))
                .thenReturn(expectedChunks);

            // When
            List<DocumentChunk> result = chunkService.getChunksByDocument(documentId);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result).isEqualTo(expectedChunks);
            verify(chunkRepository).findByDocumentIdOrderBySequenceNumber(documentId);
        }

        @Test
        @DisplayName("Should retrieve chunks without embeddings with limit")
        void shouldRetrieveChunksWithoutEmbeddingsWithLimit() {
            // Given
            int limit = 10;
            List<DocumentChunk> chunksWithoutEmbeddings = createMockChunks(5);
            Pageable expectedPageable = PageRequest.of(0, limit);
            
            when(chunkRepository.findChunksWithoutEmbeddings(tenantId, expectedPageable))
                .thenReturn(chunksWithoutEmbeddings);

            // When
            List<DocumentChunk> result = chunkService.getChunksWithoutEmbeddings(tenantId, limit);

            // Then
            assertThat(result).hasSize(5);
            assertThat(result).isEqualTo(chunksWithoutEmbeddings);
            verify(chunkRepository).findChunksWithoutEmbeddings(tenantId, expectedPageable);
        }

        @Test
        @DisplayName("Should return empty list when no chunks found")
        void shouldReturnEmptyListWhenNoChunksFound() {
            // Given
            when(chunkRepository.findByDocumentIdOrderBySequenceNumber(documentId))
                .thenReturn(List.of());

            // When
            List<DocumentChunk> result = chunkService.getChunksByDocument(documentId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Embedding Management Tests")
    class EmbeddingManagementTests {

        @Test
        @DisplayName("Should update chunk embedding successfully")
        void shouldUpdateChunkEmbeddingSuccessfully() {
            // Given
            String embeddingVectorId = "embedding-vector-123";
            DocumentChunk chunk = createMockChunk(0);
            
            when(chunkRepository.findById(chunkId)).thenReturn(Optional.of(chunk));
            when(chunkRepository.save(any(DocumentChunk.class))).thenReturn(chunk);

            // When
            chunkService.updateChunkEmbedding(chunkId, embeddingVectorId);

            // Then
            verify(chunkRepository).findById(chunkId);
            
            ArgumentCaptor<DocumentChunk> chunkCaptor = ArgumentCaptor.forClass(DocumentChunk.class);
            verify(chunkRepository).save(chunkCaptor.capture());
            
            DocumentChunk savedChunk = chunkCaptor.getValue();
            assertThat(savedChunk.getEmbeddingVectorId()).isEqualTo(embeddingVectorId);
        }

        @Test
        @DisplayName("Should throw exception when chunk not found for embedding update")
        void shouldThrowExceptionWhenChunkNotFoundForEmbeddingUpdate() {
            // Given
            String embeddingVectorId = "embedding-vector-123";
            when(chunkRepository.findById(chunkId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chunkService.updateChunkEmbedding(chunkId, embeddingVectorId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Chunk not found");
        }
    }

    @Nested
    @DisplayName("Chunk Deletion Tests")
    class ChunkDeletionTests {

        @Test
        @DisplayName("Should delete all chunks by document ID")
        void shouldDeleteAllChunksByDocumentId() {
            // When
            chunkService.deleteChunksByDocument(documentId);

            // Then
            verify(chunkRepository).deleteByDocumentId(documentId);
        }

        @Test
        @DisplayName("Should handle deletion of non-existent document chunks")
        void shouldHandleDeletionOfNonExistentDocumentChunks() {
            // Given
            doNothing().when(chunkRepository).deleteByDocumentId(documentId);

            // When
            chunkService.deleteChunksByDocument(documentId);

            // Then
            verify(chunkRepository).deleteByDocumentId(documentId);
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should return correct chunk count by document")
        void shouldReturnCorrectChunkCountByDocument() {
            // Given
            when(chunkRepository.countByDocumentId(documentId)).thenReturn(15L);

            // When
            long count = chunkService.getChunkCountByDocument(documentId);

            // Then
            assertThat(count).isEqualTo(15L);
            verify(chunkRepository).countByDocumentId(documentId);
        }

        @Test
        @DisplayName("Should return correct chunk count by tenant")
        void shouldReturnCorrectChunkCountByTenant() {
            // Given
            when(chunkRepository.countByTenantId(tenantId)).thenReturn(250L);

            // When
            long count = chunkService.getChunkCountByTenant(tenantId);

            // Then
            assertThat(count).isEqualTo(250L);
            verify(chunkRepository).countByTenantId(tenantId);
        }

        @Test
        @DisplayName("Should return zero for non-existent document")
        void shouldReturnZeroForNonExistentDocument() {
            // Given
            UUID nonExistentDocumentId = UUID.randomUUID();
            when(chunkRepository.countByDocumentId(nonExistentDocumentId)).thenReturn(0L);

            // When
            long count = chunkService.getChunkCountByDocument(nonExistentDocumentId);

            // Then
            assertThat(count).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should return zero for non-existent tenant")
        void shouldReturnZeroForNonExistentTenant() {
            // Given
            UUID nonExistentTenantId = UUID.randomUUID();
            when(chunkRepository.countByTenantId(nonExistentTenantId)).thenReturn(0L);

            // When
            long count = chunkService.getChunkCountByTenant(nonExistentTenantId);

            // Then
            assertThat(count).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Integration and Edge Cases")
    class IntegrationAndEdgeCases {

        @Test
        @DisplayName("Should handle very large text input")
        void shouldHandleVeryLargeTextInput() {
            // Given
            StringBuilder largeText = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                largeText.append("This is sentence number ").append(i).append(". ");
            }
            
            TenantDto.ChunkingConfig config = new TenantDto.ChunkingConfig(
                1000, 100, TenantDto.ChunkingStrategy.FIXED_SIZE);
            
            List<DocumentChunk> savedChunks = createMockChunks(50);
            when(chunkRepository.saveAll(any())).thenReturn(savedChunks);

            // When
            List<DocumentChunk> result = chunkService.createChunks(testDocument, largeText.toString(), config);

            // Then
            assertThat(result).hasSize(50);
            verify(chunkRepository).saveAll(any());
        }

        @Test
        @DisplayName("Should handle special characters in text")
        void shouldHandleSpecialCharactersInText() {
            // Given
            String textWithSpecialChars = "Text with special characters: àáâãäåæçèéêë ñóôõöø ùúûüÿ 中文 العربية русский 🚀 📄 ✨";
            TenantDto.ChunkingConfig config = new TenantDto.ChunkingConfig(
                100, 20, TenantDto.ChunkingStrategy.SEMANTIC);
            
            List<DocumentChunk> savedChunks = createMockChunks(1);
            when(chunkRepository.saveAll(any())).thenReturn(savedChunks);

            // When
            List<DocumentChunk> result = chunkService.createChunks(testDocument, textWithSpecialChars, config);

            // Then
            assertThat(result).hasSize(1);
            
            ArgumentCaptor<List<DocumentChunk>> chunksCaptor = ArgumentCaptor.forClass(List.class);
            verify(chunkRepository).saveAll(chunksCaptor.capture());
            
            List<DocumentChunk> capturedChunks = chunksCaptor.getValue();
            assertThat(capturedChunks.get(0).getContent()).contains("àáâãäåæçèéêë");
            assertThat(capturedChunks.get(0).getContent()).contains("🚀");
        }

        @Test
        @DisplayName("Should handle minimum chunk size configuration")
        void shouldHandleMinimumChunkSizeConfiguration() {
            // Given
            TenantDto.ChunkingConfig config = new TenantDto.ChunkingConfig(
                1, 0, TenantDto.ChunkingStrategy.FIXED_SIZE);
            
            List<DocumentChunk> savedChunks = createMockChunks(extractedText.length());
            when(chunkRepository.saveAll(any())).thenReturn(savedChunks);

            // When
            List<DocumentChunk> result = chunkService.createChunks(testDocument, extractedText, config);

            // Then
            assertThat(result).hasSizeGreaterThan(0);
            verify(chunkRepository).saveAll(any());
        }
    }

    // Helper methods for creating test data
    private Tenant createTestTenant(UUID tenantId) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Test Company");
        tenant.setSlug("test-company");
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        return tenant;
    }

    private User createTestUser(Tenant tenant) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@test-company.com");
        user.setTenant(tenant);
        user.setRole(User.UserRole.USER);
        user.setStatus(User.UserStatus.ACTIVE);
        return user;
    }

    private Document createTestDocument(UUID documentId, Tenant tenant, User user) {
        Document document = new Document();
        document.setId(documentId);
        document.setOriginalFilename("test.pdf");
        document.setFilename("test-" + documentId + ".pdf");
        document.setFileSize(1024L);
        document.setContentType("application/pdf");
        document.setDocumentType(Document.DocumentType.PDF);
        document.setFilePath("/storage/tenant/test.pdf");
        document.setTenant(tenant);
        document.setUploadedBy(user);
        document.setProcessingStatus(Document.ProcessingStatus.PENDING);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        return document;
    }

    private List<DocumentChunk> createMockChunks(int count) {
        return Arrays.stream(new int[count])
            .mapToObj(i -> createMockChunk(i))
            .toList();
    }

    private DocumentChunk createMockChunk(int sequenceNumber) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(UUID.randomUUID());
        chunk.setContent("Test chunk content " + sequenceNumber);
        chunk.setSequenceNumber(sequenceNumber);
        chunk.setTokenCount(15);
        chunk.setDocument(testDocument);
        chunk.setTenant(testTenant);
        chunk.setStartIndex(sequenceNumber * 50);
        chunk.setEndIndex((sequenceNumber + 1) * 50);
        chunk.setCreatedAt(LocalDateTime.now());
        chunk.setUpdatedAt(LocalDateTime.now());
        return chunk;
    }
}