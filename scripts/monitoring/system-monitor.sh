#!/bin/bash

# Enterprise RAG System - Comprehensive System Monitor
# This script provides real-time monitoring of all RAG system components
#
# Usage: ./scripts/monitoring/system-monitor.sh [options]
#
# Options:
#   --interval <seconds>  Monitoring interval (default: 30)
#   --duration <minutes>  Monitoring duration (default: unlimited)
#   --output <file>       Output file for monitoring data
#   --alerts              Enable alert notifications
#   --dashboard           Start interactive dashboard
#   --format <type>       Output format: json|csv|table (default: table)

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
MONITOR_LOG_DIR="${PROJECT_ROOT}/logs/monitoring"
ALERTS_DIR="${PROJECT_ROOT}/alerts"

# Default options
INTERVAL=30
DURATION=0  # 0 = unlimited
OUTPUT_FILE=""
ALERTS=false
DASHBOARD=false
FORMAT="table"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Service configuration (Gateway bypassed per ADR-001)
declare -A services=(
    ["rag-auth-service"]="8081"
    ["rag-document-service"]="8082"
    ["rag-embedding-service"]="8083"
    ["rag-core-service"]="8084"
    ["rag-admin-service"]="8085"
)

declare -A infrastructure=(
    ["postgres"]="5432"
    ["redis"]="6379"
    ["kafka"]="9092"
)

# Thresholds for alerts
declare -A thresholds=(
    ["cpu_critical"]=90
    ["cpu_warning"]=75
    ["memory_critical"]=90
    ["memory_warning"]=75
    ["disk_critical"]=90
    ["disk_warning"]=80
    ["response_time_critical"]=5000
    ["response_time_warning"]=2000
)

# Global monitoring state
declare -A service_status
declare -A service_metrics
declare -A service_alerts
monitoring_start_time=$(date +%s)

# Logging function
log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    mkdir -p "${MONITOR_LOG_DIR}"
    
    case $level in
        INFO)
            echo -e "${GREEN}[INFO]${NC} $message"
            echo "[$timestamp] [INFO] $message" >> "${MONITOR_LOG_DIR}/monitor.log"
            ;;
        WARN)
            echo -e "${YELLOW}[WARN]${NC} $message"
            echo "[$timestamp] [WARN] $message" >> "${MONITOR_LOG_DIR}/monitor.log"
            ;;
        ERROR)
            echo -e "${RED}[ERROR]${NC} $message"
            echo "[$timestamp] [ERROR] $message" >> "${MONITOR_LOG_DIR}/monitor.log"
            ;;
        ALERT)
            echo -e "${RED}[ALERT]${NC} $message"
            echo "[$timestamp] [ALERT] $message" >> "${MONITOR_LOG_DIR}/alerts.log"
            ;;
        SUCCESS)
            echo -e "${GREEN}[SUCCESS]${NC} $message"
            ;;
    esac
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --interval)
                INTERVAL="$2"
                shift 2
                ;;
            --duration)
                DURATION="$2"
                shift 2
                ;;
            --output)
                OUTPUT_FILE="$2"
                shift 2
                ;;
            --alerts)
                ALERTS=true
                shift
                ;;
            --dashboard)
                DASHBOARD=true
                shift
                ;;
            --format)
                FORMAT="$2"
                shift 2
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                echo "Unknown option: $1"
                exit 1
                ;;
        esac
    done
    
    # Validate interval
    if [[ ! "$INTERVAL" =~ ^[0-9]+$ ]] || [[ "$INTERVAL" -lt 5 ]]; then
        echo "Invalid interval. Must be >= 5 seconds."
        exit 1
    fi
    
    # Validate duration
    if [[ ! "$DURATION" =~ ^[0-9]+$ ]]; then
        echo "Invalid duration. Must be a positive number."
        exit 1
    fi
    
    # Validate format
    if [[ ! "$FORMAT" =~ ^(json|csv|table)$ ]]; then
        echo "Invalid format. Must be: json, csv, or table."
        exit 1
    fi
}

# Show help message
show_help() {
    cat << EOF
Enterprise RAG System - Comprehensive System Monitor

Usage: $0 [options]

Options:
  --interval <seconds>  Monitoring interval in seconds (default: 30, minimum: 5)
  --duration <minutes>  Monitoring duration in minutes (default: unlimited)
  --output <file>       Output file for monitoring data (auto-format based on extension)
  --alerts              Enable alert notifications via log files
  --dashboard           Start interactive dashboard mode
  --format <type>       Output format: json|csv|table (default: table)
  -h, --help            Show this help message

Monitoring includes:
  - Application service health and performance
  - Infrastructure component status
  - System resource usage (CPU, memory, disk)
  - Network connectivity and response times
  - Custom business metrics

Examples:
  $0                                    # Basic monitoring with 30s interval
  $0 --interval 10 --duration 60       # Monitor for 1 hour with 10s interval
  $0 --output monitoring.json --alerts # JSON output with alerts enabled
  $0 --dashboard                       # Interactive dashboard mode

Logs are saved to: ${MONITOR_LOG_DIR}/
Alerts are saved to: ${ALERTS_DIR}/
EOF
}

# Initialize monitoring
initialize_monitoring() {
    log INFO "Initializing RAG system monitoring..."
    
    # Create directories
    mkdir -p "${MONITOR_LOG_DIR}" "${ALERTS_DIR}"
    
    # Initialize output file if specified
    if [[ -n "$OUTPUT_FILE" ]]; then
        # Determine format from file extension if not specified explicitly
        if [[ "$OUTPUT_FILE" =~ \.json$ ]]; then
            FORMAT="json"
        elif [[ "$OUTPUT_FILE" =~ \.csv$ ]]; then
            FORMAT="csv"
        fi
        
        # Initialize output file with headers
        case $FORMAT in
            csv)
                echo "timestamp,service,status,response_time_ms,cpu_percent,memory_percent,alerts" > "$OUTPUT_FILE"
                ;;
            json)
                echo '{"monitoring_session": {"start_time": "'$(date -Iseconds)'", "data": [' > "$OUTPUT_FILE"
                ;;
        esac
    fi
    
    # Initialize service status tracking
    for service in "${!services[@]}"; do
        service_status[$service]="unknown"
        service_metrics[$service]=""
        service_alerts[$service]=""
    done
    
    for component in "${!infrastructure[@]}"; do
        service_status[$component]="unknown"
        service_metrics[$component]=""
        service_alerts[$component]=""
    done
    
    log SUCCESS "Monitoring initialized"
    
    # Show monitoring configuration
    log INFO "Configuration:"
    log INFO "  Interval: ${INTERVAL}s"
    log INFO "  Duration: $([ $DURATION -eq 0 ] && echo 'unlimited' || echo "${DURATION} minutes")"
    log INFO "  Format: $FORMAT"
    log INFO "  Alerts: $ALERTS"
    log INFO "  Dashboard: $DASHBOARD"
    [[ -n "$OUTPUT_FILE" ]] && log INFO "  Output: $OUTPUT_FILE"
}

# Check application service health
check_service_health() {
    local service=$1
    local port=${services[$service]}
    local health_url="http://localhost:${port}/actuator/health"
    
    local start_time=$(date +%s%3N)
    local response=$(curl -s -w "%{http_code}" --max-time 10 "$health_url" 2>/dev/null || echo "000")
    local end_time=$(date +%s%3N)
    
    local http_code="${response: -3}"
    local response_body="${response%???}"
    local response_time=$((end_time - start_time))
    
    local status="unknown"
    local alerts=""
    
    if [[ "$http_code" == "200" ]]; then
        if echo "$response_body" | grep -q '"status":"UP"'; then
            status="healthy"
        else
            status="degraded"
            alerts="Service reports non-UP status"
        fi
    elif [[ "$http_code" == "000" ]]; then
        status="down"
        alerts="Service unreachable"
    else
        status="error"
        alerts="HTTP $http_code"
    fi
    
    # Check response time thresholds
    if [[ $response_time -gt ${thresholds[response_time_critical]} ]]; then
        alerts="${alerts:+$alerts; }Response time critical: ${response_time}ms"
    elif [[ $response_time -gt ${thresholds[response_time_warning]} ]]; then
        alerts="${alerts:+$alerts; }Response time high: ${response_time}ms"
    fi
    
    service_status[$service]="$status"
    service_metrics[$service]="response_time:${response_time}ms"
    service_alerts[$service]="$alerts"
    
    return 0
}

# Check infrastructure component health
check_infrastructure_health() {
    local component=$1
    local port=${infrastructure[$component]}
    
    local status="unknown"
    local alerts=""
    local metrics=""
    
    case $component in
        postgres)
            if docker-compose exec -T postgres pg_isready -U rag_user &>/dev/null; then
                status="healthy"
                # Get connection count
                local connections=$(docker-compose exec -T postgres psql -U rag_user -d rag_enterprise -t -c "SELECT count(*) FROM pg_stat_activity;" 2>/dev/null | tr -d ' ' || echo "0")
                metrics="connections:$connections"
            else
                status="down"
                alerts="PostgreSQL not responding"
            fi
            ;;
        redis)
            if docker-compose exec -T redis redis-cli ping &>/dev/null; then
                status="healthy"
                # Get memory usage
                local memory_info=$(docker-compose exec -T redis redis-cli info memory | grep used_memory_human || echo "used_memory_human:unknown")
                metrics="$memory_info"
            else
                status="down"
                alerts="Redis not responding"
            fi
            ;;
        kafka)
            if docker-compose ps kafka | grep -q "Up"; then
                status="healthy"
                # Get topic count
                local topics=$(docker-compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null | wc -l || echo "0")
                metrics="topics:$topics"
            else
                status="down"
                alerts="Kafka not running"
            fi
            ;;
    esac
    
    service_status[$component]="$status"
    service_metrics[$component]="$metrics"
    service_alerts[$component]="$alerts"
}

# Get system resource metrics
get_system_metrics() {
    local cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | sed 's/%us,//' || echo "0")
    local memory_usage=$(free | grep Mem | awk '{printf "%.1f", $3/$2 * 100.0}' || echo "0")
    local disk_usage=$(df / | tail -1 | awk '{print $5}' | sed 's/%//' || echo "0")
    
    # Check system thresholds
    local system_alerts=""
    
    if (( $(echo "$cpu_usage >= ${thresholds[cpu_critical]}" | bc -l 2>/dev/null || echo 0) )); then
        system_alerts="${system_alerts:+$system_alerts; }CPU critical: ${cpu_usage}%"
    elif (( $(echo "$cpu_usage >= ${thresholds[cpu_warning]}" | bc -l 2>/dev/null || echo 0) )); then
        system_alerts="${system_alerts:+$system_alerts; }CPU high: ${cpu_usage}%"
    fi
    
    if (( $(echo "$memory_usage >= ${thresholds[memory_critical]}" | bc -l 2>/dev/null || echo 0) )); then
        system_alerts="${system_alerts:+$system_alerts; }Memory critical: ${memory_usage}%"
    elif (( $(echo "$memory_usage >= ${thresholds[memory_warning]}" | bc -l 2>/dev/null || echo 0) )); then
        system_alerts="${system_alerts:+$system_alerts; }Memory high: ${memory_usage}%"
    fi
    
    if [[ $disk_usage -ge ${thresholds[disk_critical]} ]]; then
        system_alerts="${system_alerts:+$system_alerts; }Disk critical: ${disk_usage}%"
    elif [[ $disk_usage -ge ${thresholds[disk_warning]} ]]; then
        system_alerts="${system_alerts:+$system_alerts; }Disk high: ${disk_usage}%"
    fi
    
    service_status["system"]="healthy"
    service_metrics["system"]="cpu:${cpu_usage}%,memory:${memory_usage}%,disk:${disk_usage}%"
    service_alerts["system"]="$system_alerts"
}

# Perform monitoring cycle
perform_monitoring_cycle() {
    local cycle_start=$(date +%s)
    local timestamp=$(date -Iseconds)
    
    # Check all application services
    for service in "${!services[@]}"; do
        check_service_health "$service"
    done
    
    # Check all infrastructure components
    for component in "${!infrastructure[@]}"; do
        check_infrastructure_health "$component"
    done
    
    # Get system metrics
    get_system_metrics
    
    # Process alerts
    if [[ "$ALERTS" == "true" ]]; then
        process_alerts
    fi
    
    # Output monitoring data
    output_monitoring_data "$timestamp"
    
    local cycle_duration=$(($(date +%s) - cycle_start))
    log INFO "Monitoring cycle completed in ${cycle_duration}s"
}

# Process and handle alerts
process_alerts() {
    local alert_timestamp=$(date -Iseconds)
    local alerts_found=false
    
    # Check for new alerts
    for component in "${!service_alerts[@]}"; do
        local alerts="${service_alerts[$component]}"
        if [[ -n "$alerts" ]]; then
            alerts_found=true
            log ALERT "$component: $alerts"
            
            # Write to alerts file
            echo "[$alert_timestamp] $component: $alerts" >> "${ALERTS_DIR}/current_alerts.log"
        fi
    done
    
    # Create alert summary if alerts found
    if [[ "$alerts_found" == "true" ]]; then
        create_alert_summary "$alert_timestamp"
    fi
}

# Create alert summary
create_alert_summary() {
    local timestamp=$1
    local summary_file="${ALERTS_DIR}/alert_summary_$(date +%Y%m%d_%H%M%S).json"
    
    cat > "$summary_file" << EOF
{
    "timestamp": "$timestamp",
    "alert_level": "warning",
    "summary": {
        "total_services_monitored": $((${#services[@]} + ${#infrastructure[@]} + 1)),
        "services_with_alerts": $(echo "${service_alerts[@]}" | tr ' ' '\n' | grep -c '[^[:space:]]' || echo 0),
        "critical_alerts": 0,
        "warning_alerts": 0
    },
    "alerts": [
EOF

    local first_alert=true
    for component in "${!service_alerts[@]}"; do
        local alerts="${service_alerts[$component]}"
        if [[ -n "$alerts" ]]; then
            if [[ "$first_alert" == "false" ]]; then
                echo "," >> "$summary_file"
            fi
            cat >> "$summary_file" << EOF
        {
            "component": "$component",
            "status": "${service_status[$component]}",
            "alerts": "$alerts",
            "metrics": "${service_metrics[$component]}"
        }EOF
            first_alert=false
        fi
    done

    cat >> "$summary_file" << EOF

    ]
}
EOF
}

# Output monitoring data
output_monitoring_data() {
    local timestamp=$1
    
    case $FORMAT in
        table)
            display_table_format "$timestamp"
            ;;
        csv)
            output_csv_format "$timestamp"
            ;;
        json)
            output_json_format "$timestamp"
            ;;
    esac
}

# Display table format output
display_table_format() {
    local timestamp=$1
    
    if [[ "$DASHBOARD" == "true" ]]; then
        clear
        echo "======================================================================"
        echo "                    RAG System Monitor Dashboard"
        echo "======================================================================"
        echo "Timestamp: $timestamp"
        echo "Uptime: $(($(date +%s) - monitoring_start_time))s"
        echo ""
    fi
    
    printf "%-25s %-12s %-15s %-20s %-30s\n" "Component" "Status" "Response Time" "Metrics" "Alerts"
    printf "%s\n" "$(printf '=%.0s' {1..120})"
    
    # Application services
    for service in "${!services[@]}"; do
        local status="${service_status[$service]}"
        local metrics="${service_metrics[$service]}"
        local alerts="${service_alerts[$service]:-none}"
        local response_time=$(echo "$metrics" | grep -o 'response_time:[0-9]*ms' | cut -d: -f2 || echo "n/a")
        
        # Color code status
        local status_color=""
        case $status in
            healthy) status_color="${GREEN}$status${NC}" ;;
            degraded) status_color="${YELLOW}$status${NC}" ;;
            down|error) status_color="${RED}$status${NC}" ;;
            *) status_color="$status" ;;
        esac
        
        printf "%-25s %-22s %-15s %-20s %-30s\n" "$service" "$status_color" "$response_time" "$metrics" "${alerts:0:30}"
    done
    
    echo ""
    
    # Infrastructure components
    for component in "${!infrastructure[@]}"; do
        local status="${service_status[$component]}"
        local metrics="${service_metrics[$component]}"
        local alerts="${service_alerts[$component]:-none}"
        
        local status_color=""
        case $status in
            healthy) status_color="${GREEN}$status${NC}" ;;
            degraded) status_color="${YELLOW}$status${NC}" ;;
            down|error) status_color="${RED}$status${NC}" ;;
            *) status_color="$status" ;;
        esac
        
        printf "%-25s %-22s %-15s %-20s %-30s\n" "$component" "$status_color" "n/a" "$metrics" "${alerts:0:30}"
    done
    
    echo ""
    
    # System metrics
    local system_status="${service_status[system]}"
    local system_metrics="${service_metrics[system]}"
    local system_alerts="${service_alerts[system]:-none}"
    
    printf "%-25s %-22s %-15s %-20s %-30s\n" "System" "${GREEN}$system_status${NC}" "n/a" "$system_metrics" "${system_alerts:0:30}"
    
    if [[ "$DASHBOARD" != "true" ]]; then
        echo ""
    fi
}

# Output CSV format
output_csv_format() {
    local timestamp=$1
    
    # Output to file if specified, otherwise stdout
    local output_dest="${OUTPUT_FILE:-/dev/stdout}"
    
    for component in "${!service_status[@]}"; do
        local status="${service_status[$component]}"
        local metrics="${service_metrics[$component]}"
        local alerts="${service_alerts[$component]}"
        local response_time=$(echo "$metrics" | grep -o 'response_time:[0-9]*' | cut -d: -f2 || echo "0")
        local cpu_percent=$(echo "$metrics" | grep -o 'cpu:[0-9.]*' | cut -d: -f2 | sed 's/%//' || echo "0")
        local memory_percent=$(echo "$metrics" | grep -o 'memory:[0-9.]*' | cut -d: -f2 | sed 's/%//' || echo "0")
        
        echo "$timestamp,$component,$status,$response_time,$cpu_percent,$memory_percent,\"$alerts\"" >> "$output_dest"
    done
}

# Output JSON format
output_json_format() {
    local timestamp=$1
    local json_entry=""
    
    # Build JSON entry
    json_entry="{"
    json_entry="$json_entry\"timestamp\": \"$timestamp\","
    json_entry="$json_entry\"services\": {"
    
    local first_service=true
    for component in "${!service_status[@]}"; do
        if [[ "$first_service" == "false" ]]; then
            json_entry="$json_entry,"
        fi
        
        local status="${service_status[$component]}"
        local metrics="${service_metrics[$component]}"
        local alerts="${service_alerts[$component]}"
        
        json_entry="$json_entry\"$component\": {"
        json_entry="$json_entry\"status\": \"$status\","
        json_entry="$json_entry\"metrics\": \"$metrics\","
        json_entry="$json_entry\"alerts\": \"$alerts\""
        json_entry="$json_entry}"
        
        first_service=false
    done
    
    json_entry="$json_entry}}"
    
    # Output to file if specified, otherwise stdout
    if [[ -n "$OUTPUT_FILE" ]]; then
        echo ",$json_entry" >> "$OUTPUT_FILE"
    else
        echo "$json_entry"
    fi
}

# Cleanup and finalize
cleanup_monitoring() {
    log INFO "Cleaning up monitoring session..."
    
    # Finalize JSON output file
    if [[ -n "$OUTPUT_FILE" && "$FORMAT" == "json" ]]; then
        echo ']}}' >> "$OUTPUT_FILE"
    fi
    
    # Create session summary
    local end_time=$(date +%s)
    local duration=$((end_time - monitoring_start_time))
    
    log SUCCESS "Monitoring session completed"
    log INFO "Duration: ${duration}s"
    log INFO "Logs saved to: ${MONITOR_LOG_DIR}/"
    
    if [[ "$ALERTS" == "true" ]]; then
        log INFO "Alerts saved to: ${ALERTS_DIR}/"
    fi
    
    if [[ -n "$OUTPUT_FILE" ]]; then
        log INFO "Monitoring data saved to: $OUTPUT_FILE"
    fi
}

# Signal handlers
setup_signal_handlers() {
    trap 'log INFO "Received interrupt signal"; cleanup_monitoring; exit 0' INT TERM
}

# Main monitoring loop
main_monitoring_loop() {
    local cycles=0
    local max_cycles=0
    
    # Calculate max cycles if duration is specified
    if [[ $DURATION -gt 0 ]]; then
        max_cycles=$(( (DURATION * 60) / INTERVAL ))
        log INFO "Will run for $max_cycles cycles (${DURATION} minutes)"
    fi
    
    while true; do
        perform_monitoring_cycle
        cycles=$((cycles + 1))
        
        # Check if duration limit reached
        if [[ $max_cycles -gt 0 && $cycles -ge $max_cycles ]]; then
            log INFO "Duration limit reached ($cycles cycles)"
            break
        fi
        
        # Wait for next cycle
        if [[ "$DASHBOARD" == "true" ]]; then
            sleep "$INTERVAL"
        else
            log INFO "Next monitoring cycle in ${INTERVAL}s..."
            sleep "$INTERVAL"
        fi
    done
}

# Main function
main() {
    echo "📊 Enterprise RAG System - Comprehensive Monitor"
    echo "==============================================="
    
    parse_args "$@"
    setup_signal_handlers
    initialize_monitoring
    
    log INFO "Starting monitoring with ${INTERVAL}s interval"
    
    main_monitoring_loop
    cleanup_monitoring
    
    log SUCCESS "Monitoring completed successfully! 📊"
}

# Run main function with all arguments
main "$@"