package com.enterprise.rag.shared.dto;

import java.util.Map;
import java.util.UUID;

public record DocumentChunkDto(
    UUID id,
    String content,
    Integer sequenceNumber,
    Integer startIndex,
    Integer endIndex,
    Integer tokenCount,
    String documentFilename,
    Map<String, Object> metadata
) {}