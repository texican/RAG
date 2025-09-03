package com.byo.rag.document.service;

import com.byo.rag.shared.entity.Document;
import com.byo.rag.shared.exception.DocumentProcessingException;
import com.byo.rag.shared.util.TextChunker;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Service
public class TextExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(TextExtractionService.class);

    private final Tika tika;

    public TextExtractionService() {
        this.tika = new Tika();
    }

    public String extractText(Path filePath, Document.DocumentType documentType) {
        try {
            logger.info("Extracting text from file: {} (type: {})", filePath.getFileName(), documentType);

            String extractedText;
            
            switch (documentType) {
                case TXT, MD -> extractedText = extractPlainText(filePath);
                case PDF, DOCX, DOC, RTF, ODT, HTML -> extractedText = extractWithTika(filePath);
                default -> throw new DocumentProcessingException("Unsupported document type: " + documentType);
            }

            String cleanedText = TextChunker.cleanText(extractedText);
            
            logger.info("Successfully extracted {} characters from {}", 
                       cleanedText.length(), filePath.getFileName());

            return cleanedText;

        } catch (Exception e) {
            throw new DocumentProcessingException("Failed to extract text from file: " + filePath.getFileName(), e);
        }
    }

    public Map<String, Object> extractMetadata(Path filePath) {
        try {
            Metadata metadata = new Metadata();
            
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                tika.parse(inputStream, metadata);
            }

            Map<String, Object> metadataMap = new HashMap<>();
            
            for (String name : metadata.names()) {
                String value = metadata.get(name);
                if (value != null && !value.trim().isEmpty()) {
                    metadataMap.put(name, value);
                }
            }

            // Add file-specific metadata
            metadataMap.put("file_size", Files.size(filePath));
            metadataMap.put("file_name", filePath.getFileName().toString());
            
            return metadataMap;

        } catch (Exception e) {
            logger.warn("Failed to extract metadata from file: {}", filePath.getFileName(), e);
            return new HashMap<>();
        }
    }

    public Document.DocumentType detectDocumentType(String filename, String contentType) {
        String extension = getFileExtension(filename).toLowerCase();
        
        return switch (extension) {
            case ".pdf" -> Document.DocumentType.PDF;
            case ".docx" -> Document.DocumentType.DOCX;
            case ".doc" -> Document.DocumentType.DOC;
            case ".txt" -> Document.DocumentType.TXT;
            case ".md", ".markdown" -> Document.DocumentType.MD;
            case ".html", ".htm" -> Document.DocumentType.HTML;
            case ".rtf" -> Document.DocumentType.RTF;
            case ".odt" -> Document.DocumentType.ODT;
            default -> {
                // Fallback to content type
                if (contentType != null) {
                    yield switch (contentType) {
                        case "application/pdf" -> Document.DocumentType.PDF;
                        case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> Document.DocumentType.DOCX;
                        case "application/msword" -> Document.DocumentType.DOC;
                        case "text/plain" -> Document.DocumentType.TXT;
                        case "text/markdown" -> Document.DocumentType.MD;
                        case "text/html" -> Document.DocumentType.HTML;
                        case "application/rtf" -> Document.DocumentType.RTF;
                        case "application/vnd.oasis.opendocument.text" -> Document.DocumentType.ODT;
                        default -> Document.DocumentType.TXT; // Default fallback
                    };
                }
                yield Document.DocumentType.TXT;
            }
        };
    }

    private String extractPlainText(Path filePath) throws IOException {
        return Files.readString(filePath);
    }

    private String extractWithTika(Path filePath) throws IOException, TikaException {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            return tika.parseToString(inputStream);
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
    }

    public boolean isValidDocumentType(String filename, String contentType) {
        try {
            detectDocumentType(filename, contentType);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public long getMaxFileSize() {
        return 50 * 1024 * 1024; // 50MB
    }
}