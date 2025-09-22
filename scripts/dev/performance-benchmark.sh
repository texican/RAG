#!/bin/bash

# Enterprise RAG System - Performance Benchmark Suite
# This script runs comprehensive performance tests across all services
#
# Usage: ./scripts/dev/performance-benchmark.sh [options]
#
# Options:
#   --service <name>     Benchmark specific service only
#   --duration <time>    Test duration in seconds (default: 300)
#   --users <count>      Concurrent users to simulate (default: 10)
#   --ramp-up <time>     User ramp-up time in seconds (default: 30)
#   --output <dir>       Output directory for reports
#   --profile <name>     Performance profile: light|standard|heavy
#   --monitor-resources  Monitor system resources during tests

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
BENCHMARK_DIR="${PROJECT_ROOT}/benchmark-results"
LOGS_DIR="${PROJECT_ROOT}/logs/benchmark"

# Default options
SPECIFIC_SERVICE=""
DURATION=300
USERS=10
RAMP_UP=30
OUTPUT_DIR="${BENCHMARK_DIR}/$(date +%Y%m%d_%H%M%S)"
PROFILE="standard"
MONITOR_RESOURCES=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

# Performance test configurations
declare -A profiles=(
    ["light"]="duration=60 users=5 ramp_up=10"
    ["standard"]="duration=300 users=10 ramp_up=30"
    ["heavy"]="duration=600 users=50 ramp_up=60"
)

# Service endpoints and test scenarios
declare -A service_endpoints=(
    ["rag-gateway"]="http://localhost:8080"
    ["rag-auth-service"]="http://localhost:8081"
    ["rag-admin-service"]="http://localhost:8085/admin/api"
    ["rag-document-service"]="http://localhost:8082"
    ["rag-embedding-service"]="http://localhost:8083"
    ["rag-core-service"]="http://localhost:8084"
)

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
            echo "[$timestamp] [INFO] $message" >> "${LOGS_DIR}/benchmark.log"
            ;;
        WARN)
            echo -e "${YELLOW}[WARN]${NC} $message"
            echo "[$timestamp] [WARN] $message" >> "${LOGS_DIR}/benchmark.log"
            ;;
        ERROR)
            echo -e "${RED}[ERROR]${NC} $message"
            echo "[$timestamp] [ERROR] $message" >> "${LOGS_DIR}/benchmark.log"
            ;;
        SUCCESS)
            echo -e "${GREEN}[SUCCESS]${NC} $message"
            echo "[$timestamp] [SUCCESS] $message" >> "${LOGS_DIR}/benchmark.log"
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
            --service)
                SPECIFIC_SERVICE="$2"
                shift 2
                ;;
            --duration)
                DURATION="$2"
                shift 2
                ;;
            --users)
                USERS="$2"
                shift 2
                ;;
            --ramp-up)
                RAMP_UP="$2"
                shift 2
                ;;
            --output)
                OUTPUT_DIR="$2"
                shift 2
                ;;
            --profile)
                PROFILE="$2"
                shift 2
                ;;
            --monitor-resources)
                MONITOR_RESOURCES=true
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
    
    # Apply profile settings
    if [[ -n "${profiles[$PROFILE]:-}" ]]; then
        eval "${profiles[$PROFILE]}"
        log INFO "Applied profile '$PROFILE': duration=${DURATION}s, users=${USERS}, ramp-up=${RAMP_UP}s"
    fi
}

# Show help message
show_help() {
    cat << EOF
Enterprise RAG System - Performance Benchmark Suite

Usage: $0 [options]

Options:
  --service <name>     Benchmark specific service only
  --duration <time>    Test duration in seconds (default: 300)
  --users <count>      Concurrent users to simulate (default: 10)
  --ramp-up <time>     User ramp-up time in seconds (default: 30)
  --output <dir>       Output directory for reports
  --profile <name>     Performance profile: light|standard|heavy
  --monitor-resources  Monitor system resources during tests
  -h, --help           Show this help message

Available services:
$(printf "  - %s\n" "${!service_endpoints[@]}")

Profiles:
  light      - Quick test (60s, 5 users, 10s ramp-up)
  standard   - Standard test (300s, 10 users, 30s ramp-up)
  heavy      - Stress test (600s, 50 users, 60s ramp-up)

Examples:
  $0                                         # Run all services with standard profile
  $0 --service rag-embedding-service         # Test specific service
  $0 --profile heavy                         # Run heavy stress test
  $0 --duration 600 --users 20 --ramp-up 60 # Custom settings
  $0 --monitor-resources                     # Include resource monitoring

Reports are saved to: ${BENCHMARK_DIR}/
EOF
}

# Check prerequisites
check_prerequisites() {
    log INFO "Checking prerequisites..."
    
    # Check if services are running
    local healthy_services=0
    local total_services=${#service_endpoints[@]}
    
    for service in "${!service_endpoints[@]}"; do
        local endpoint="${service_endpoints[$service]}/actuator/health"
        if curl -s "$endpoint" | grep -q '"status":"UP"' 2>/dev/null; then
            ((healthy_services++))
            log INFO "✅ $service is healthy"
        else
            log WARN "❌ $service is not healthy"
        fi
    done
    
    if [[ $healthy_services -lt $total_services ]]; then
        log WARN "Only $healthy_services/$total_services services are healthy"
        log INFO "Consider running ./scripts/utils/health-check.sh first"
    fi
    
    # Check for performance testing tools
    if ! command -v curl &> /dev/null; then
        error_exit "curl is required for performance testing"
    fi
    
    # Try to find Apache Bench or similar tools
    local load_tool=""
    if command -v ab &> /dev/null; then
        load_tool="ab"
    elif command -v wrk &> /dev/null; then
        load_tool="wrk"
    elif command -v hey &> /dev/null; then
        load_tool="hey"
    else
        log WARN "No load testing tool found (ab, wrk, hey). Using basic curl tests."
        load_tool="curl"
    fi
    
    log INFO "Using load testing tool: $load_tool"
    echo "LOAD_TOOL=$load_tool" > "${OUTPUT_DIR}/.benchmark_config"
}

# Setup benchmark environment
setup_benchmark_environment() {
    log INFO "Setting up benchmark environment..."
    
    # Create output directories
    mkdir -p "${OUTPUT_DIR}" "${LOGS_DIR}"
    
    # Create benchmark configuration
    cat > "${OUTPUT_DIR}/benchmark_config.json" << EOF
{
    "timestamp": "$(date -Iseconds)",
    "configuration": {
        "duration": $DURATION,
        "users": $USERS,
        "ramp_up": $RAMP_UP,
        "profile": "$PROFILE",
        "specific_service": "$SPECIFIC_SERVICE",
        "monitor_resources": $MONITOR_RESOURCES
    },
    "environment": {
        "hostname": "$(hostname)",
        "os": "$(uname -s)",
        "arch": "$(uname -m)",
        "cpu_cores": $(nproc),
        "memory_gb": $(free -g | awk '/^Mem:/{print $2}')
    }
}
EOF

    # Start resource monitoring if requested
    if [[ "$MONITOR_RESOURCES" == "true" ]]; then
        start_resource_monitoring
    fi
}

# Start resource monitoring
start_resource_monitoring() {
    log INFO "Starting resource monitoring..."
    
    local monitor_script="${OUTPUT_DIR}/resource_monitor.sh"
    cat > "$monitor_script" << 'EOF'
#!/bin/bash
INTERVAL=5
OUTPUT_FILE="$1"

echo "timestamp,cpu_percent,memory_percent,disk_io_read,disk_io_write,network_rx,network_tx" > "$OUTPUT_FILE"

while true; do
    timestamp=$(date -Iseconds)
    cpu=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | sed 's/%us,//')
    memory=$(free | grep Mem | awk '{printf "%.1f", $3/$2 * 100.0}')
    disk_io=$(iostat -d 1 1 | awk '/^[a-z]/ {read+=$3; write+=$4} END {print read","write}')
    network=$(cat /proc/net/dev | awk 'BEGIN{rx=0;tx=0} NR>2{rx+=$2;tx+=$10} END{print rx","tx}')
    
    echo "$timestamp,$cpu,$memory,$disk_io,$network" >> "$OUTPUT_FILE"
    sleep $INTERVAL
done
EOF

    chmod +x "$monitor_script"
    
    # Start monitoring in background
    "$monitor_script" "${OUTPUT_DIR}/resource_usage.csv" &
    echo $! > "${OUTPUT_DIR}/monitor.pid"
    
    log INFO "Resource monitoring started (PID: $(cat ${OUTPUT_DIR}/monitor.pid))"
}

# Stop resource monitoring
stop_resource_monitoring() {
    if [[ -f "${OUTPUT_DIR}/monitor.pid" ]]; then
        local pid=$(cat "${OUTPUT_DIR}/monitor.pid")
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid"
            rm "${OUTPUT_DIR}/monitor.pid"
            log INFO "Resource monitoring stopped"
        fi
    fi
}

# Generate test data for various endpoints
generate_test_data() {
    log INFO "Generating test data..."
    
    # Create sample payloads for different services
    mkdir -p "${OUTPUT_DIR}/test_data"
    
    # Auth service login payload
    cat > "${OUTPUT_DIR}/test_data/auth_login.json" << EOF
{
    "username": "test@example.com",
    "password": "testpassword"
}
EOF

    # Document upload payload (small document)
    cat > "${OUTPUT_DIR}/test_data/document_upload.json" << EOF
{
    "title": "Performance Test Document",
    "content": "This is a test document for performance benchmarking. It contains some sample text to test document processing capabilities of the RAG system.",
    "metadata": {
        "source": "performance_test",
        "category": "benchmark"
    }
}
EOF

    # Embedding generation payload
    cat > "${OUTPUT_DIR}/test_data/embedding_request.json" << EOF
{
    "texts": [
        "This is a sample text for embedding generation",
        "Another sample text for performance testing",
        "RAG system performance benchmark text"
    ],
    "modelName": "openai-text-embedding-3-small"
}
EOF

    # Search query payload
    cat > "${OUTPUT_DIR}/test_data/search_request.json" << EOF
{
    "query": "sample search query for performance testing",
    "topK": 10,
    "threshold": 0.7,
    "includeContent": true
}
EOF

    # RAG query payload
    cat > "${OUTPUT_DIR}/test_data/rag_query.json" << EOF
{
    "question": "What is the performance of the RAG system?",
    "context": {
        "maxResults": 5,
        "includeMetadata": true
    }
}
EOF
}

# Run performance test for a specific service
run_service_benchmark() {
    local service=$1
    local endpoint="${service_endpoints[$service]}"
    
    log INFO "Running benchmark for $service..."
    
    local service_output="${OUTPUT_DIR}/${service}"
    mkdir -p "$service_output"
    
    # Determine test scenarios based on service
    case $service in
        "rag-auth-service")
            run_auth_service_benchmark "$endpoint" "$service_output"
            ;;
        "rag-admin-service")
            run_admin_service_benchmark "$endpoint" "$service_output"
            ;;
        "rag-document-service")
            run_document_service_benchmark "$endpoint" "$service_output"
            ;;
        "rag-embedding-service")
            run_embedding_service_benchmark "$endpoint" "$service_output"
            ;;
        "rag-core-service")
            run_core_service_benchmark "$endpoint" "$service_output"
            ;;
        "rag-gateway")
            run_gateway_benchmark "$endpoint" "$service_output"
            ;;
        *)
            run_generic_benchmark "$endpoint" "$service_output"
            ;;
    esac
}

# Auth service specific benchmark
run_auth_service_benchmark() {
    local endpoint=$1
    local output_dir=$2
    
    log INFO "Running auth service performance tests..."
    
    # Health check performance
    run_load_test "Health Check" \
        "GET" \
        "$endpoint/actuator/health" \
        "" \
        "$output_dir/health_check.json"
    
    # Login performance
    run_load_test "User Login" \
        "POST" \
        "$endpoint/auth/login" \
        "${OUTPUT_DIR}/test_data/auth_login.json" \
        "$output_dir/login.json"
}

# Embedding service specific benchmark
run_embedding_service_benchmark() {
    local endpoint=$1
    local output_dir=$2
    
    log INFO "Running embedding service performance tests..."
    
    # Health check
    run_load_test "Health Check" \
        "GET" \
        "$endpoint/actuator/health" \
        "" \
        "$output_dir/health_check.json"
    
    # Embedding generation
    run_load_test "Embedding Generation" \
        "POST" \
        "$endpoint/api/v1/embeddings/generate" \
        "${OUTPUT_DIR}/test_data/embedding_request.json" \
        "$output_dir/embedding_generation.json"
    
    # Similarity search
    run_load_test "Similarity Search" \
        "POST" \
        "$endpoint/api/v1/embeddings/search" \
        "${OUTPUT_DIR}/test_data/search_request.json" \
        "$output_dir/similarity_search.json"
}

# Document service specific benchmark
run_document_service_benchmark() {
    local endpoint=$1
    local output_dir=$2
    
    log INFO "Running document service performance tests..."
    
    # Health check
    run_load_test "Health Check" \
        "GET" \
        "$endpoint/actuator/health" \
        "" \
        "$output_dir/health_check.json"
    
    # Document list
    run_load_test "Document List" \
        "GET" \
        "$endpoint/api/v1/documents" \
        "" \
        "$output_dir/document_list.json"
}

# Core service specific benchmark
run_core_service_benchmark() {
    local endpoint=$1
    local output_dir=$2
    
    log INFO "Running core service performance tests..."
    
    # Health check
    run_load_test "Health Check" \
        "GET" \
        "$endpoint/actuator/health" \
        "" \
        "$output_dir/health_check.json"
    
    # RAG query
    run_load_test "RAG Query" \
        "POST" \
        "$endpoint/api/v1/rag/query" \
        "${OUTPUT_DIR}/test_data/rag_query.json" \
        "$output_dir/rag_query.json"
}

# Admin service specific benchmark
run_admin_service_benchmark() {
    local endpoint=$1
    local output_dir=$2
    
    log INFO "Running admin service performance tests..."
    
    # Health check
    run_load_test "Health Check" \
        "GET" \
        "$endpoint/actuator/health" \
        "" \
        "$output_dir/health_check.json"
}

# Gateway benchmark
run_gateway_benchmark() {
    local endpoint=$1
    local output_dir=$2
    
    log INFO "Running gateway performance tests..."
    
    # Health check
    run_load_test "Health Check" \
        "GET" \
        "$endpoint/actuator/health" \
        "" \
        "$output_dir/health_check.json"
}

# Generic benchmark for any service
run_generic_benchmark() {
    local endpoint=$1
    local output_dir=$2
    
    log INFO "Running generic performance tests..."
    
    # Health check
    run_load_test "Health Check" \
        "GET" \
        "$endpoint/actuator/health" \
        "" \
        "$output_dir/health_check.json"
}

# Run load test with specified parameters
run_load_test() {
    local test_name=$1
    local method=$2
    local url=$3
    local payload_file=$4
    local output_file=$5
    
    log INFO "Running test: $test_name"
    
    local start_time=$(date +%s)
    local test_results_file="${output_file%.json}_raw.txt"
    
    # Prepare curl command
    local curl_cmd="curl -s -w '@${OUTPUT_DIR}/curl_format.txt'"
    
    # Create curl format file for detailed timing
    cat > "${OUTPUT_DIR}/curl_format.txt" << 'EOF'
{
    "time_namelookup": %{time_namelookup},
    "time_connect": %{time_connect},
    "time_appconnect": %{time_appconnect},
    "time_pretransfer": %{time_pretransfer},
    "time_redirect": %{time_redirect},
    "time_starttransfer": %{time_starttransfer},
    "time_total": %{time_total},
    "http_code": %{http_code},
    "size_download": %{size_download},
    "size_upload": %{size_upload},
    "content_type": "%{content_type}"
}
EOF

    if [[ "$method" == "POST" && -n "$payload_file" ]]; then
        curl_cmd="$curl_cmd -X POST -H 'Content-Type: application/json' -d @$payload_file"
    fi
    
    curl_cmd="$curl_cmd $url"
    
    # Run the load test
    local success_count=0
    local error_count=0
    local total_requests=$((USERS * DURATION / 10))  # Approximate requests
    
    echo "Test: $test_name" > "$test_results_file"
    echo "Started: $(date)" >> "$test_results_file"
    echo "URL: $url" >> "$test_results_file"
    echo "Method: $method" >> "$test_results_file"
    echo "" >> "$test_results_file"
    
    # Simple load test implementation
    for ((i=1; i<=total_requests; i++)); do
        local request_start=$(date +%s.%3N)
        
        if eval "$curl_cmd" >> "$test_results_file" 2>&1; then
            ((success_count++))
        else
            ((error_count++))
        fi
        
        local request_end=$(date +%s.%3N)
        local request_time=$(echo "$request_end - $request_start" | bc 2>/dev/null || echo "0")
        
        echo "Request $i: ${request_time}s" >> "$test_results_file"
        
        # Add small delay to simulate realistic load
        sleep 0.1
    done
    
    local end_time=$(date +%s)
    local total_time=$((end_time - start_time))
    
    # Generate test summary
    cat > "$output_file" << EOF
{
    "test_name": "$test_name",
    "method": "$method",
    "url": "$url",
    "timestamp": "$(date -Iseconds)",
    "duration_seconds": $total_time,
    "total_requests": $total_requests,
    "successful_requests": $success_count,
    "failed_requests": $error_count,
    "success_rate": $(echo "scale=2; $success_count * 100 / $total_requests" | bc 2>/dev/null || echo "0"),
    "requests_per_second": $(echo "scale=2; $total_requests / $total_time" | bc 2>/dev/null || echo "0"),
    "average_response_time": "calculated_from_raw_data"
}
EOF

    log INFO "Test '$test_name' completed: $success_count/$total_requests successful (${total_time}s)"
}

# Generate comprehensive benchmark report
generate_benchmark_report() {
    log INFO "Generating benchmark report..."
    
    local report_file="${OUTPUT_DIR}/benchmark_report.html"
    local summary_file="${OUTPUT_DIR}/benchmark_summary.json"
    
    # Create HTML report
    cat > "$report_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>RAG System Performance Benchmark Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #f0f0f0; padding: 20px; border-radius: 5px; }
        .summary { margin: 20px 0; }
        .service { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
        .metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin: 15px 0; }
        .metric { background-color: #f8f9fa; padding: 10px; border-radius: 3px; text-align: center; }
        .metric-value { font-size: 1.5em; font-weight: bold; color: #007bff; }
        .metric-label { font-size: 0.9em; color: #666; }
        table { width: 100%; border-collapse: collapse; margin: 15px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
        .footer { margin-top: 30px; font-size: 0.9em; color: #666; }
    </style>
</head>
<body>
    <div class="header">
        <h1>RAG System Performance Benchmark Report</h1>
        <p>Generated: $(date)</p>
        <p>Profile: $PROFILE | Duration: ${DURATION}s | Users: $USERS | Ramp-up: ${RAMP_UP}s</p>
    </div>
    
    <div class="summary">
        <h2>Benchmark Summary</h2>
        <div class="metrics">
            <div class="metric">
                <div class="metric-value">$(find "$OUTPUT_DIR" -name "*.json" | wc -l)</div>
                <div class="metric-label">Tests Executed</div>
            </div>
            <div class="metric">
                <div class="metric-value">$DURATION</div>
                <div class="metric-label">Duration (seconds)</div>
            </div>
            <div class="metric">
                <div class="metric-value">$USERS</div>
                <div class="metric-label">Concurrent Users</div>
            </div>
        </div>
    </div>
EOF

    # Add service results to report
    for service in "${!service_endpoints[@]}"; do
        if [[ -d "${OUTPUT_DIR}/${service}" ]]; then
            echo "    <div class=\"service\">" >> "$report_file"
            echo "        <h3>$service</h3>" >> "$report_file"
            echo "        <p>Endpoint: ${service_endpoints[$service]}</p>" >> "$report_file"
            
            # Add test results table
            echo "        <table>" >> "$report_file"
            echo "            <tr><th>Test</th><th>Success Rate</th><th>Requests/sec</th><th>Duration</th></tr>" >> "$report_file"
            
            for result_file in "${OUTPUT_DIR}/${service}"/*.json; do
                if [[ -f "$result_file" ]]; then
                    local test_name=$(basename "$result_file" .json)
                    echo "            <tr>" >> "$report_file"
                    echo "                <td>$test_name</td>" >> "$report_file"
                    echo "                <td>-</td>" >> "$report_file"
                    echo "                <td>-</td>" >> "$report_file"
                    echo "                <td>-</td>" >> "$report_file"
                    echo "            </tr>" >> "$report_file"
                fi
            done
            
            echo "        </table>" >> "$report_file"
            echo "    </div>" >> "$report_file"
        fi
    done

    cat >> "$report_file" << EOF
    
    <div class="footer">
        <p>Detailed results available in: ${OUTPUT_DIR}/</p>
        <p>Raw test data available in individual service directories</p>
    </div>
</body>
</html>
EOF

    # Create JSON summary
    cat > "$summary_file" << EOF
{
    "benchmark_id": "$(basename "$OUTPUT_DIR")",
    "timestamp": "$(date -Iseconds)",
    "configuration": {
        "profile": "$PROFILE",
        "duration": $DURATION,
        "users": $USERS,
        "ramp_up": $RAMP_UP,
        "specific_service": "$SPECIFIC_SERVICE"
    },
    "results_location": "$OUTPUT_DIR",
    "reports": {
        "html_report": "$report_file",
        "summary": "$summary_file"
    }
}
EOF

    log SUCCESS "Benchmark report generated: $report_file"
}

# Cleanup function
cleanup() {
    log INFO "Cleaning up benchmark environment..."
    
    # Stop resource monitoring
    if [[ "$MONITOR_RESOURCES" == "true" ]]; then
        stop_resource_monitoring
    fi
    
    # Clean up temporary files
    rm -f "${OUTPUT_DIR}/curl_format.txt" 2>/dev/null || true
    rm -f "${OUTPUT_DIR}/.benchmark_config" 2>/dev/null || true
}

# Main function
main() {
    echo "📊 Enterprise RAG System - Performance Benchmark Suite"
    echo "===================================================="
    
    parse_args "$@"
    
    log INFO "Benchmark started with profile '$PROFILE': duration=${DURATION}s, users=${USERS}, ramp-up=${RAMP_UP}s"
    
    # Setup trap for cleanup
    trap cleanup EXIT
    
    check_prerequisites
    setup_benchmark_environment
    generate_test_data
    
    # Run benchmarks
    if [[ -n "$SPECIFIC_SERVICE" ]]; then
        if [[ -n "${service_endpoints[$SPECIFIC_SERVICE]:-}" ]]; then
            run_service_benchmark "$SPECIFIC_SERVICE"
        else
            error_exit "Unknown service: $SPECIFIC_SERVICE"
        fi
    else
        for service in "${!service_endpoints[@]}"; do
            run_service_benchmark "$service"
        done
    fi
    
    generate_benchmark_report
    
    log SUCCESS "Performance benchmark completed! 🎉"
    echo ""
    echo "Results available in: $OUTPUT_DIR"
    echo "HTML Report: ${OUTPUT_DIR}/benchmark_report.html"
    echo "Summary: ${OUTPUT_DIR}/benchmark_summary.json"
    
    if [[ "$MONITOR_RESOURCES" == "true" ]]; then
        echo "Resource Usage: ${OUTPUT_DIR}/resource_usage.csv"
    fi
}

# Run main function with all arguments
main "$@"