package com.byo.rag.document.service;

import com.byo.rag.shared.exception.DocumentProcessingException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final Path rootLocation;

    public FileStorageService(@Value("${file.storage.location:./storage}") String storageLocation) {
        this.rootLocation = Paths.get(storageLocation).toAbsolutePath().normalize();
        createDirectories();
    }

    public String storeFile(MultipartFile file, UUID tenantId, UUID documentId) {
        try {
            if (file.isEmpty()) {
                throw new DocumentProcessingException("Cannot store empty file");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                throw new DocumentProcessingException("File must have a name");
            }

            // Create tenant-specific directory
            Path tenantDir = rootLocation.resolve(tenantId.toString());
            Files.createDirectories(tenantDir);

            // Generate unique filename
            String fileExtension = getFileExtension(originalFilename);
            String storedFilename = documentId.toString() + fileExtension;
            
            Path targetLocation = tenantDir.resolve(storedFilename);

            // Copy file to target location
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            logger.info("Stored file {} for tenant {} as {}", originalFilename, tenantId, storedFilename);

            return targetLocation.toString();

        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to store file", e);
        }
    }

    public byte[] loadFileAsBytes(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!path.startsWith(rootLocation)) {
                throw new DocumentProcessingException("Cannot access file outside storage directory");
            }
            
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to load file: " + filePath, e);
        }
    }

    public Path loadFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!path.startsWith(rootLocation)) {
                throw new DocumentProcessingException("Cannot access file outside storage directory");
            }
            
            if (!Files.exists(path)) {
                throw new DocumentProcessingException("File not found: " + filePath);
            }
            
            return path;
        } catch (Exception e) {
            throw new DocumentProcessingException("Failed to load file: " + filePath, e);
        }
    }

    public void deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!path.startsWith(rootLocation)) {
                throw new DocumentProcessingException("Cannot delete file outside storage directory");
            }
            
            Files.deleteIfExists(path);
            logger.info("Deleted file: {}", filePath);
        } catch (IOException e) {
            logger.warn("Failed to delete file: {}", filePath, e);
        }
    }

    public long getFileSize(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.size(path);
        } catch (IOException e) {
            logger.warn("Failed to get file size: {}", filePath, e);
            return 0;
        }
    }

    public long getTenantStorageUsage(UUID tenantId) {
        try {
            Path tenantDir = rootLocation.resolve(tenantId.toString());
            if (!Files.exists(tenantDir)) {
                return 0;
            }
            
            return FileUtils.sizeOfDirectory(tenantDir.toFile());
        } catch (Exception e) {
            logger.warn("Failed to calculate storage usage for tenant: {}", tenantId, e);
            return 0;
        }
    }

    private void createDirectories() {
        try {
            Files.createDirectories(rootLocation);
            logger.info("Created storage directory: {}", rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directory", e);
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
    }
}