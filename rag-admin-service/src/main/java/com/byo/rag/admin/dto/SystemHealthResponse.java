package com.byo.rag.admin.dto;

import java.time.LocalDateTime;

/**
 * System health and performance monitoring response for administrative dashboards.
 * 
 * <p>This record provides comprehensive system resource utilization metrics and
 * health indicators for the RAG platform. It enables administrators to monitor
 * system performance, plan capacity, and identify potential issues proactively.</p>
 * 
 * <p>Health monitoring categories:</p>
 * <ul>
 *   <li><strong>System Status:</strong> Overall health classification (HEALTHY/WARNING/CRITICAL)</li>
 *   <li><strong>CPU Metrics:</strong> Current processor utilization percentages</li>
 *   <li><strong>Memory Usage:</strong> RAM consumption and availability statistics</li>
 *   <li><strong>Storage Metrics:</strong> Disk space utilization and capacity</li>
 *   <li><strong>Network Activity:</strong> Active connection counts and throughput</li>
 *   <li><strong>Uptime Tracking:</strong> System availability and reliability metrics</li>
 * </ul>
 * 
 * <p>Administrative use cases:</p>
 * <ul>
 *   <li>Real-time system monitoring and alerting</li>
 *   <li>Performance trend analysis and capacity planning</li>
 *   <li>Infrastructure scaling decision support</li>
 *   <li>Troubleshooting and diagnostic operations</li>
 * </ul>
 * 
 * @param status overall system health status (HEALTHY, WARNING, or CRITICAL)
 * @param cpuUsagePercent current CPU utilization as a percentage (0.0-100.0)
 * @param memoryUsedBytes currently allocated memory in bytes
 * @param memoryTotalBytes total system memory capacity in bytes
 * @param memoryUsagePercent memory utilization as a percentage (0.0-100.0)
 * @param diskUsedBytes currently consumed disk space in bytes
 * @param diskTotalBytes total disk storage capacity in bytes
 * @param diskUsagePercent disk utilization as a percentage (0.0-100.0)
 * @param activeConnections number of currently active network connections
 * @param uptimeMillis system uptime duration in milliseconds
 * @param timestamp when this health snapshot was captured
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see com.byo.rag.admin.controller.AdminAuthController
 */
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