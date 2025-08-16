package com.enterprise.rag.core.controller;

import com.enterprise.rag.core.dto.QuestionRequest;
import com.enterprise.rag.core.dto.RagResponse;
import com.enterprise.rag.core.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rag")
@Tag(name = "RAG (Retrieval Augmented Generation)", description = "Question answering using document knowledge base")
public class RagController {

    private static final Logger logger = LoggerFactory.getLogger(RagController.class);

    private final RagService ragService;

    @Autowired
    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    @Operation(
        summary = "Ask a question using RAG",
        description = "Process a question by retrieving relevant document chunks and generating an AI-powered answer"
    )
    @ApiResponse(responseCode = "200", description = "Question processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<RagResponse> askQuestion(
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            
            @Parameter(description = "Question request", required = true)
            @Valid @RequestBody QuestionRequest request) {

        logger.info("Received RAG question from tenant: {}", tenantId);
        logger.debug("Question: {}", request.question());

        try {
            UUID tenantUuid = UUID.fromString(tenantId);
            RagResponse response = ragService.processQuestion(request, tenantUuid);
            
            logger.info("RAG question processed successfully in {}ms", response.processingTimeMs());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid tenant ID format: {}", tenantId);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error processing RAG question", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    @Operation(
        summary = "Check RAG service health",
        description = "Verify that the RAG service is operational and can connect to dependencies"
    )
    public ResponseEntity<HealthStatus> checkHealth() {
        logger.debug("Health check requested");
        
        // TODO: Add actual health checks for dependencies
        HealthStatus status = new HealthStatus(
            "healthy",
            "RAG service is operational",
            System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(status);
    }

    public record HealthStatus(
        String status,
        String message,
        long timestamp
    ) {}
}