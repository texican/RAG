#!/bin/bash

# Enterprise RAG System - Advanced Alerting System
# This script provides comprehensive alerting for the RAG system with multiple notification channels
#
# Usage: ./scripts/monitoring/alerting-system.sh [options]
#
# Options:
#   --config <file>      Alerting configuration file (default: alerts.conf)
#   --check-interval <s> Alert checking interval in seconds (default: 60)
#   --email <address>    Email address for alerts
#   --slack <webhook>    Slack webhook URL for alerts
#   --discord <webhook>  Discord webhook URL for alerts
#   --pagerduty <key>    PagerDuty integration key
#   --severity <level>   Minimum severity level: info|warning|critical (default: warning)
#   --test               Send test alerts to verify configuration

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ALERTS_DIR="${PROJECT_ROOT}/alerts"
CONFIG_DIR="${PROJECT_ROOT}/config/alerts"
LOGS_DIR="${PROJECT_ROOT}/logs/alerts"

# Default options
CONFIG_FILE="${CONFIG_DIR}/alerts.conf"
CHECK_INTERVAL=60
EMAIL_ADDRESS=""
SLACK_WEBHOOK=""
DISCORD_WEBHOOK=""
PAGERDUTY_KEY=""
MIN_SEVERITY="warning"
TEST_MODE=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

# Alert severity levels
declare -A severity_levels=(
    ["info"]=1
    ["warning"]=2
    ["critical"]=3
)

# Alert state tracking
declare -A active_alerts
declare -A alert_counts
declare -A last_alert_time

# Alert rules
declare -a alert_rules

# Notification channels
declare -a notification_channels

# Logging function
log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    mkdir -p "${LOGS_DIR}"
    
    case $level in
        INFO)
            echo -e "${GREEN}[INFO]${NC} $message"
            echo "[$timestamp] [INFO] $message" >> "${LOGS_DIR}/alerting.log"
            ;;
        WARN)
            echo -e "${YELLOW}[WARN]${NC} $message"
            echo "[$timestamp] [WARN] $message" >> "${LOGS_DIR}/alerting.log"
            ;;
        ERROR)
            echo -e "${RED}[ERROR]${NC} $message"
            echo "[$timestamp] [ERROR] $message" >> "${LOGS_DIR}/alerting.log"
            ;;
        ALERT)
            echo -e "${RED}[ALERT]${NC} $message"
            echo "[$timestamp] [ALERT] $message" >> "${LOGS_DIR}/alerts.log"
            ;;
        SUCCESS)
            echo -e "${GREEN}[SUCCESS]${NC} $message"
            ;;
    esac
}

# Error handler
error_exit() {
    log ERROR "$1"
    exit 1
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --config)
                CONFIG_FILE="$2"
                shift 2
                ;;
            --check-interval)
                CHECK_INTERVAL="$2"
                shift 2
                ;;
            --email)
                EMAIL_ADDRESS="$2"
                shift 2
                ;;
            --slack)
                SLACK_WEBHOOK="$2"
                shift 2
                ;;
            --discord)
                DISCORD_WEBHOOK="$2"
                shift 2
                ;;
            --pagerduty)
                PAGERDUTY_KEY="$2"
                shift 2
                ;;
            --severity)
                MIN_SEVERITY="$2"
                shift 2
                ;;
            --test)
                TEST_MODE=true
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                error_exit "Unknown option: $1"
                ;;
        esac
    done
    
    # Validate severity level
    if [[ -z "${severity_levels[$MIN_SEVERITY]:-}" ]]; then
        error_exit "Invalid severity level: $MIN_SEVERITY. Valid options: ${!severity_levels[*]}"
    fi
    
    # Validate check interval
    if [[ ! "$CHECK_INTERVAL" =~ ^[0-9]+$ ]] || [[ "$CHECK_INTERVAL" -lt 10 ]]; then
        error_exit "Invalid check interval. Must be >= 10 seconds."
    fi
}

# Show help message
show_help() {
    cat << EOF
Enterprise RAG System - Advanced Alerting System

Usage: $0 [options]

Options:
  --config <file>      Alerting configuration file (default: alerts.conf)
  --check-interval <s> Alert checking interval in seconds (default: 60, minimum: 10)
  --email <address>    Email address for alerts (requires sendmail or similar)
  --slack <webhook>    Slack webhook URL for alerts
  --discord <webhook>  Discord webhook URL for alerts
  --pagerduty <key>    PagerDuty integration key for critical alerts
  --severity <level>   Minimum severity level: info|warning|critical (default: warning)
  --test               Send test alerts to verify configuration
  -h, --help           Show this help message

Alert Types:
  - Service health (down, degraded, high response times)
  - System resources (CPU, memory, disk usage)
  - Infrastructure (database, cache, message queue)
  - Business metrics (error rates, throughput)
  - Security events (failed logins, unauthorized access)

Configuration:
  The alerting system uses a configuration file to define alert rules,
  thresholds, and notification preferences. A default configuration
  will be created if none exists.

Examples:
  $0                                         # Start with default configuration
  $0 --email admin@company.com --severity critical  # Email critical alerts only
  $0 --slack https://hooks.slack.com/...    # Send alerts to Slack
  $0 --test                                 # Test alert configuration

Logs are saved to: ${LOGS_DIR}/
Configuration: ${CONFIG_DIR}/
EOF
}

# Initialize alerting system
initialize_alerting() {
    log INFO "Initializing RAG system alerting..."
    
    # Create directories
    mkdir -p "${ALERTS_DIR}" "${CONFIG_DIR}" "${LOGS_DIR}"
    
    # Load or create configuration
    load_configuration
    
    # Initialize notification channels
    initialize_notification_channels
    
    # Load alert rules
    load_alert_rules
    
    log SUCCESS "Alerting system initialized"
    log INFO "Configuration: $CONFIG_FILE"
    log INFO "Check interval: ${CHECK_INTERVAL}s"
    log INFO "Minimum severity: $MIN_SEVERITY"
    log INFO "Notification channels: ${#notification_channels[@]}"
}

# Load configuration file
load_configuration() {
    if [[ ! -f "$CONFIG_FILE" ]]; then
        log WARN "Configuration file not found: $CONFIG_FILE"
        log INFO "Creating default configuration..."
        create_default_configuration
    fi
    
    log INFO "Loading configuration from: $CONFIG_FILE"
    
    # Source configuration file
    if [[ -f "$CONFIG_FILE" ]]; then
        source "$CONFIG_FILE"
    fi
}

# Create default configuration
create_default_configuration() {
    cat > "$CONFIG_FILE" << 'EOF'
# RAG System Alerting Configuration

# Alert thresholds
SERVICE_DOWN_THRESHOLD=3           # Consecutive failures before alert
SERVICE_RESPONSE_TIME_WARNING=2000 # Response time warning (ms)
SERVICE_RESPONSE_TIME_CRITICAL=5000 # Response time critical (ms)
CPU_WARNING_THRESHOLD=75          # CPU usage warning (%)
CPU_CRITICAL_THRESHOLD=90         # CPU usage critical (%)
MEMORY_WARNING_THRESHOLD=75       # Memory usage warning (%)
MEMORY_CRITICAL_THRESHOLD=90      # Memory usage critical (%)
DISK_WARNING_THRESHOLD=80         # Disk usage warning (%)
DISK_CRITICAL_THRESHOLD=90        # Disk usage critical (%)

# Alert cooldown periods (seconds)
ALERT_COOLDOWN_INFO=300           # 5 minutes for info alerts
ALERT_COOLDOWN_WARNING=600        # 10 minutes for warning alerts
ALERT_COOLDOWN_CRITICAL=300       # 5 minutes for critical alerts

# Notification settings
ENABLE_EMAIL_NOTIFICATIONS=true
ENABLE_SLACK_NOTIFICATIONS=true
ENABLE_DISCORD_NOTIFICATIONS=false
ENABLE_PAGERDUTY_NOTIFICATIONS=false

# Business hours (24-hour format)
BUSINESS_HOURS_START=9
BUSINESS_HOURS_END=17
BUSINESS_DAYS="1,2,3,4,5"  # Monday to Friday

# Alert escalation
ESCALATE_AFTER_MINUTES=30
ESCALATION_EMAIL=""
ESCALATION_PHONE=""
EOF

    log SUCCESS "Default configuration created: $CONFIG_FILE"
}

# Initialize notification channels
initialize_notification_channels() {
    notification_channels=()
    
    # Email notification
    if [[ -n "$EMAIL_ADDRESS" && "${ENABLE_EMAIL_NOTIFICATIONS:-true}" == "true" ]]; then
        notification_channels+=("email:$EMAIL_ADDRESS")
        log INFO "Email notifications enabled: $EMAIL_ADDRESS"
    fi
    
    # Slack notification
    if [[ -n "$SLACK_WEBHOOK" && "${ENABLE_SLACK_NOTIFICATIONS:-true}" == "true" ]]; then
        notification_channels+=("slack:$SLACK_WEBHOOK")
        log INFO "Slack notifications enabled"
    fi
    
    # Discord notification
    if [[ -n "$DISCORD_WEBHOOK" && "${ENABLE_DISCORD_NOTIFICATIONS:-false}" == "true" ]]; then
        notification_channels+=("discord:$DISCORD_WEBHOOK")
        log INFO "Discord notifications enabled"
    fi
    
    # PagerDuty notification
    if [[ -n "$PAGERDUTY_KEY" && "${ENABLE_PAGERDUTY_NOTIFICATIONS:-false}" == "true" ]]; then
        notification_channels+=("pagerduty:$PAGERDUTY_KEY")
        log INFO "PagerDuty notifications enabled"
    fi
    
    if [[ ${#notification_channels[@]} -eq 0 ]]; then
        log WARN "No notification channels configured. Alerts will only be logged."
    fi
}

# Load alert rules
load_alert_rules() {
    alert_rules=(
        "service_health:check_service_health:warning"
        "system_resources:check_system_resources:warning" 
        "infrastructure:check_infrastructure:critical"
        "response_times:check_response_times:warning"
        "error_rates:check_error_rates:warning"
        "security_events:check_security_events:critical"
    )
    
    log INFO "Loaded ${#alert_rules[@]} alert rules"
}

# Check service health
check_service_health() {
    local alerts=()
    
    # Define services to check (Gateway bypassed per ADR-001)
    local services=("rag-auth-service:8081" "rag-document-service:8082"
                   "rag-embedding-service:8083" "rag-core-service:8084" "rag-admin-service:8085")
    
    for service_info in "${services[@]}"; do
        local service_name=${service_info%:*}
        local service_port=${service_info#*:}
        local health_url="http://localhost:${service_port}/actuator/health"
        
        # Check service health
        local response=$(curl -s -w "%{http_code}" --max-time 5 "$health_url" 2>/dev/null || echo "000")
        local http_code="${response: -3}"
        
        if [[ "$http_code" != "200" ]]; then
            # Increment failure count
            alert_counts["${service_name}_failures"]=$((${alert_counts["${service_name}_failures"]:-0} + 1))
            
            if [[ ${alert_counts["${service_name}_failures"]} -ge ${SERVICE_DOWN_THRESHOLD:-3} ]]; then
                alerts+=("critical:Service $service_name is down (HTTP $http_code)")
                alert_counts["${service_name}_failures"]=0  # Reset after alerting
            fi
        else
            # Reset failure count on success
            alert_counts["${service_name}_failures"]=0
        fi
    done
    
    printf '%s\n' "${alerts[@]}"
}

# Check system resources
check_system_resources() {
    local alerts=()
    
    # CPU usage
    local cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | sed 's/%us,//' | cut -d. -f1 || echo "0")
    if [[ $cpu_usage -ge ${CPU_CRITICAL_THRESHOLD:-90} ]]; then
        alerts+=("critical:CPU usage critical: ${cpu_usage}%")
    elif [[ $cpu_usage -ge ${CPU_WARNING_THRESHOLD:-75} ]]; then
        alerts+=("warning:CPU usage high: ${cpu_usage}%")
    fi
    
    # Memory usage
    local memory_usage=$(free | grep Mem | awk '{printf "%.0f", $3/$2 * 100.0}' || echo "0")
    if [[ $memory_usage -ge ${MEMORY_CRITICAL_THRESHOLD:-90} ]]; then
        alerts+=("critical:Memory usage critical: ${memory_usage}%")
    elif [[ $memory_usage -ge ${MEMORY_WARNING_THRESHOLD:-75} ]]; then
        alerts+=("warning:Memory usage high: ${memory_usage}%")
    fi
    
    # Disk usage
    local disk_usage=$(df / | tail -1 | awk '{print $5}' | sed 's/%//' || echo "0")
    if [[ $disk_usage -ge ${DISK_CRITICAL_THRESHOLD:-90} ]]; then
        alerts+=("critical:Disk usage critical: ${disk_usage}%")
    elif [[ $disk_usage -ge ${DISK_WARNING_THRESHOLD:-80} ]]; then
        alerts+=("warning:Disk usage high: ${disk_usage}%")
    fi
    
    printf '%s\n' "${alerts[@]}"
}

# Check infrastructure components
check_infrastructure() {
    local alerts=()
    
    # PostgreSQL
    if ! docker-compose exec -T postgres pg_isready -U rag_user &>/dev/null; then
        alerts+=("critical:PostgreSQL database is not responding")
    fi
    
    # Redis
    if ! docker-compose exec -T redis redis-cli ping &>/dev/null; then
        alerts+=("critical:Redis cache is not responding")
    fi
    
    # Kafka
    if ! docker-compose ps kafka | grep -q "Up"; then
        alerts+=("critical:Kafka message queue is not running")
    fi
    
    printf '%s\n' "${alerts[@]}"
}

# Check response times
check_response_times() {
    local alerts=()
    
    local services=("rag-auth-service:8081" "rag-document-service:8082" "rag-embedding-service:8083")
    
    for service_info in "${services[@]}"; do
        local service_name=${service_info%:*}
        local service_port=${service_info#*:}
        local health_url="http://localhost:${service_port}/actuator/health"
        
        local start_time=$(date +%s%3N)
        local response=$(curl -s "$health_url" 2>/dev/null || echo "")
        local end_time=$(date +%s%3N)
        
        if [[ -n "$response" ]]; then
            local response_time=$((end_time - start_time))
            
            if [[ $response_time -ge ${SERVICE_RESPONSE_TIME_CRITICAL:-5000} ]]; then
                alerts+=("critical:Service $service_name response time critical: ${response_time}ms")
            elif [[ $response_time -ge ${SERVICE_RESPONSE_TIME_WARNING:-2000} ]]; then
                alerts+=("warning:Service $service_name response time high: ${response_time}ms")
            fi
        fi
    done
    
    printf '%s\n' "${alerts[@]}"
}

# Check error rates
check_error_rates() {
    local alerts=()
    
    # Check application logs for error patterns
    local log_files=("${PROJECT_ROOT}/logs"/*.log)
    local current_time=$(date +%s)
    local five_minutes_ago=$((current_time - 300))
    
    for log_file in "${log_files[@]}"; do
        if [[ -f "$log_file" ]]; then
            local service_name=$(basename "$log_file" .log)
            
            # Count recent errors (last 5 minutes)
            local error_count=$(awk -v start_time="$five_minutes_ago" '
                /ERROR|FATAL/ && match($0, /([0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2})/) {
                    cmd = "date -d \"" substr($0, RSTART, RLENGTH) "\" +%s"
                    cmd | getline timestamp
                    close(cmd)
                    if (timestamp >= start_time) count++
                }
                END { print count+0 }
            ' "$log_file" 2>/dev/null || echo "0")
            
            if [[ $error_count -ge 10 ]]; then
                alerts+=("warning:High error rate in $service_name: $error_count errors in 5 minutes")
            fi
        fi
    done
    
    printf '%s\n' "${alerts[@]}"
}

# Check security events
check_security_events() {
    local alerts=()
    
    # Check for failed authentication attempts
    local auth_log="${PROJECT_ROOT}/logs/rag-auth-service.log"
    if [[ -f "$auth_log" ]]; then
        local failed_logins=$(grep -c "Authentication failed\|Invalid credentials" "$auth_log" 2>/dev/null || echo "0")
        if [[ $failed_logins -ge 5 ]]; then
            alerts+=("warning:Multiple failed authentication attempts: $failed_logins")
        fi
    fi
    
    # Check for unauthorized access attempts in auth service logs
    local auth_log="${PROJECT_ROOT}/logs/rag-auth-service.log"
    if [[ -f "$auth_log" ]]; then
        local unauthorized_attempts=$(grep -c "403\|401\|Unauthorized" "$auth_log" 2>/dev/null || echo "0")
        if [[ $unauthorized_attempts -ge 10 ]]; then
            alerts+=("critical:Multiple unauthorized access attempts: $unauthorized_attempts")
        fi
    fi
    
    printf '%s\n' "${alerts[@]}"
}

# Process alert rules
process_alert_rules() {
    local new_alerts=()
    
    for rule in "${alert_rules[@]}"; do
        local rule_name=${rule%%:*}
        local rule_function=${rule#*:}
        rule_function=${rule_function%:*}
        local rule_min_severity=${rule##*:}
        
        # Skip if rule severity is below minimum
        if [[ ${severity_levels[$rule_min_severity]} -lt ${severity_levels[$MIN_SEVERITY]} ]]; then
            continue
        fi
        
        # Execute rule function
        local rule_alerts
        if declare -f "$rule_function" >/dev/null; then
            mapfile -t rule_alerts < <($rule_function)
            new_alerts+=("${rule_alerts[@]}")
        fi
    done
    
    # Process new alerts
    for alert in "${new_alerts[@]}"; do
        if [[ -n "$alert" ]]; then
            process_alert "$alert"
        fi
    done
}

# Process individual alert
process_alert() {
    local alert=$1
    local severity=${alert%%:*}
    local message=${alert#*:}
    local alert_id=$(echo "$message" | md5sum | cut -d' ' -f1)
    
    # Check if alert is already active and within cooldown period
    local current_time=$(date +%s)
    local last_time=${last_alert_time[$alert_id]:-0}
    local cooldown_var="ALERT_COOLDOWN_${severity^^}"
    local cooldown=${!cooldown_var:-600}
    
    if [[ $((current_time - last_time)) -lt $cooldown ]]; then
        return 0  # Skip due to cooldown
    fi
    
    # Record alert
    active_alerts[$alert_id]="$severity:$message"
    last_alert_time[$alert_id]=$current_time
    
    log ALERT "[$severity] $message"
    
    # Send notifications
    send_notifications "$severity" "$message"
    
    # Create alert record
    create_alert_record "$severity" "$message" "$alert_id"
}

# Send notifications to configured channels
send_notifications() {
    local severity=$1
    local message=$2
    
    for channel in "${notification_channels[@]}"; do
        local channel_type=${channel%%:*}
        local channel_config=${channel#*:}
        
        case $channel_type in
            email)
                send_email_notification "$severity" "$message" "$channel_config"
                ;;
            slack)
                send_slack_notification "$severity" "$message" "$channel_config"
                ;;
            discord)
                send_discord_notification "$severity" "$message" "$channel_config"
                ;;
            pagerduty)
                if [[ "$severity" == "critical" ]]; then
                    send_pagerduty_notification "$severity" "$message" "$channel_config"
                fi
                ;;
        esac
    done
}

# Send email notification
send_email_notification() {
    local severity=$1
    local message=$2
    local email_address=$3
    
    local subject="RAG System Alert [$severity]: $(date)"
    local body="Alert Details:
Severity: $severity
Message: $message
Timestamp: $(date)
System: $(hostname)

This is an automated alert from the RAG System monitoring."
    
    if command -v sendmail >/dev/null; then
        echo -e "Subject: $subject\n\n$body" | sendmail "$email_address"
        log INFO "Email alert sent to: $email_address"
    elif command -v mail >/dev/null; then
        echo "$body" | mail -s "$subject" "$email_address"
        log INFO "Email alert sent to: $email_address"
    else
        log WARN "No email command available (sendmail or mail)"
    fi
}

# Send Slack notification
send_slack_notification() {
    local severity=$1
    local message=$2
    local webhook_url=$3
    
    local color=""
    case $severity in
        critical) color="danger" ;;
        warning) color="warning" ;;
        info) color="good" ;;
    esac
    
    local payload=$(cat << EOF
{
    "attachments": [
        {
            "color": "$color",
            "title": "RAG System Alert",
            "fields": [
                {
                    "title": "Severity",
                    "value": "$severity",
                    "short": true
                },
                {
                    "title": "Message",
                    "value": "$message",
                    "short": false
                },
                {
                    "title": "Timestamp",
                    "value": "$(date)",
                    "short": true
                }
            ]
        }
    ]
}
EOF
)
    
    if curl -s -X POST -H 'Content-type: application/json' --data "$payload" "$webhook_url" >/dev/null; then
        log INFO "Slack alert sent"
    else
        log WARN "Failed to send Slack alert"
    fi
}

# Send Discord notification
send_discord_notification() {
    local severity=$1
    local message=$2
    local webhook_url=$3
    
    local color=""
    case $severity in
        critical) color="16711680" ;;  # Red
        warning) color="16776960" ;;   # Yellow
        info) color="65280" ;;         # Green
    esac
    
    local payload=$(cat << EOF
{
    "embeds": [
        {
            "title": "RAG System Alert",
            "color": $color,
            "fields": [
                {
                    "name": "Severity",
                    "value": "$severity",
                    "inline": true
                },
                {
                    "name": "Message",
                    "value": "$message",
                    "inline": false
                }
            ],
            "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%S.000Z)"
        }
    ]
}
EOF
)
    
    if curl -s -X POST -H 'Content-type: application/json' --data "$payload" "$webhook_url" >/dev/null; then
        log INFO "Discord alert sent"
    else
        log WARN "Failed to send Discord alert"
    fi
}

# Send PagerDuty notification
send_pagerduty_notification() {
    local severity=$1
    local message=$2
    local integration_key=$3
    
    local payload=$(cat << EOF
{
    "routing_key": "$integration_key",
    "event_action": "trigger",
    "payload": {
        "summary": "RAG System Critical Alert: $message",
        "severity": "critical",
        "source": "rag-system-monitoring",
        "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%S.000Z)",
        "custom_details": {
            "severity": "$severity",
            "message": "$message",
            "system": "$(hostname)"
        }
    }
}
EOF
)
    
    if curl -s -X POST -H 'Content-type: application/json' --data "$payload" \
       "https://events.pagerduty.com/v2/enqueue" >/dev/null; then
        log INFO "PagerDuty alert sent"
    else
        log WARN "Failed to send PagerDuty alert"
    fi
}

# Create alert record
create_alert_record() {
    local severity=$1
    local message=$2
    local alert_id=$3
    
    local alert_file="${ALERTS_DIR}/alert_${alert_id}_$(date +%Y%m%d_%H%M%S).json"
    
    cat > "$alert_file" << EOF
{
    "alert_id": "$alert_id",
    "timestamp": "$(date -Iseconds)",
    "severity": "$severity",
    "message": "$message",
    "hostname": "$(hostname)",
    "resolved": false,
    "notification_channels": $(printf '%s\n' "${notification_channels[@]}" | jq -R . | jq -s .)
}
EOF
}

# Send test alerts
send_test_alerts() {
    log INFO "Sending test alerts to verify configuration..."
    
    # Test info alert
    process_alert "info:Test info alert - alerting system is working"
    sleep 2
    
    # Test warning alert
    process_alert "warning:Test warning alert - this is a test"
    sleep 2
    
    # Test critical alert
    process_alert "critical:Test critical alert - immediate attention required"
    
    log SUCCESS "Test alerts sent to all configured channels"
}

# Cleanup and signal handlers
cleanup_alerting() {
    log INFO "Cleaning up alerting system..."
    
    # Save final state
    local state_file="${ALERTS_DIR}/alerting_state_$(date +%Y%m%d_%H%M%S).json"
    cat > "$state_file" << EOF
{
    "shutdown_time": "$(date -Iseconds)",
    "active_alerts": $(printf '%s\n' "${!active_alerts[@]}" | jq -R . | jq -s .),
    "alert_counts": $(printf '%s\n' "${!alert_counts[@]}" | jq -R . | jq -s .)
}
EOF

    log SUCCESS "Alerting system shutdown complete"
}

setup_signal_handlers() {
    trap 'log INFO "Received interrupt signal"; cleanup_alerting; exit 0' INT TERM
}

# Main alerting loop
main_alerting_loop() {
    log INFO "Starting alerting system main loop..."
    
    while true; do
        log INFO "Running alert checks..."
        
        # Process all alert rules
        process_alert_rules
        
        # Log status
        log INFO "Alert check cycle completed. Active alerts: ${#active_alerts[@]}"
        
        # Wait for next check
        sleep "$CHECK_INTERVAL"
    done
}

# Main function
main() {
    echo "🚨 Enterprise RAG System - Advanced Alerting System"
    echo "================================================="
    
    parse_args "$@"
    setup_signal_handlers
    initialize_alerting
    
    # Handle test mode
    if [[ "$TEST_MODE" == "true" ]]; then
        send_test_alerts
        exit 0
    fi
    
    log INFO "Starting alerting system with ${CHECK_INTERVAL}s check interval"
    
    main_alerting_loop
    cleanup_alerting
    
    log SUCCESS "Alerting system completed! 🚨"
}

# Run main function with all arguments
main "$@"