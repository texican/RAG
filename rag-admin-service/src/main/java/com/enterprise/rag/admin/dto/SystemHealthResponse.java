package com.enterprise.rag.admin.dto;

import java.time.LocalDateTime;

public record SystemHealthResponse(
        String status, // "HEALTHY", "WARNING", "CRITICAL"
        double cpuUsagePercent,
        long memoryUsedBytes,
        long memoryTotalBytes,
        double memoryUsagePercent,
        long diskUsedBytes,
        long diskTotalBytes,
        double diskUsagePercent,
        int activeConnections,
        long uptimeMillis,
        LocalDateTime timestamp
) {
}