package com.byo.rag.document.listener;

import com.byo.rag.document.service.DocumentService;
import com.byo.rag.shared.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kafka listener for document processing events.
 *
 * <p>This listener consumes document processing events from Kafka and triggers
 * the asynchronous document processing pipeline. It handles text extraction,
 * chunking, and initiates embedding generation for uploaded documents.</p>
 *
 * <p><strong>Event Flow:</strong></p>
 * <ol>
 *   <li>Document uploaded → Event published to Kafka</li>
 *   <li>This listener consumes the event</li>
 *   <li>Triggers DocumentService.processDocument()</li>
 *   <li>Text extraction and chunking performed</li>
 *   <li>Chunks sent for embedding generation</li>
 * </ol>
 *
 * <p><strong>Error Handling:</strong></p>
 * <ul>
 *   <li>Errors logged with full context</li>
 *   <li>Processing failures don't block Kafka consumer</li>
 *   <li>Document status updated to FAILED on errors</li>
 * </ul>
 *
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 */
@Component
@Profile("!test")
@ConditionalOnBean(KafkaTemplate.class)
public class DocumentProcessingKafkaListener {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingKafkaListener.class);

    private final DocumentService documentService;

    public DocumentProcessingKafkaListener(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Listens for document processing events and triggers document processing.
     *
     * @param message the Kafka message containing document ID
     * @param partition the Kafka partition
     * @param offset the message offset
     */
    @KafkaListener(
        topics = "${kafka.topics.document-processing:document-processing}",
        groupId = "${kafka.consumer.group-id:document-service-group}"
    )
    public void handleDocumentProcessing(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        try {
            logger.info("Received document processing event from partition {} at offset {}", partition, offset);
            logger.debug("Message: {}", message);

            // Parse message to extract document ID
            DocumentProcessingMessage msg = JsonUtils.fromJson(message, DocumentProcessingMessage.class);
            UUID documentId = msg.documentId();

            logger.info("Processing document: {}", documentId);

            // Trigger async document processing
            documentService.processDocument(documentId);

        } catch (Exception e) {
            logger.error("Error processing document event from partition {} at offset {}: {}",
                        partition, offset, message, e);
            // Don't throw - we don't want to block the Kafka consumer
            // Document status will be updated to FAILED by DocumentService
        }
    }

    /**
     * Message structure for document processing events.
     * Matches the structure sent by DocumentProcessingKafkaService.
     */
    public record DocumentProcessingMessage(UUID documentId) {}
}
