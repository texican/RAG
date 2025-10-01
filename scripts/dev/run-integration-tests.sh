#!/bin/bash

# Enterprise RAG System - Comprehensive Integration Test Runner
# This script runs all integration tests across all services with detailed reporting
#
# Usage: ./scripts/dev/run-integration-tests.sh [options]
#
# Options:
#   --service <name>     Run tests for specific service only
#   --parallel           Run service tests in parallel
#   --coverage           Generate test coverage reports
#   --verbose            Enable verbose output
#   --fail-fast          Stop on first test failure
#   --skip-build         Skip compilation before testing

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
LOG_DIR="${PROJECT_ROOT}/logs/tests"
RESULTS_DIR="${PROJECT_ROOT}/test-results"
COVERAGE_DIR="${PROJECT_ROOT}/coverage-reports"

# Default options
SPECIFIC_SERVICE=""
PARALLEL=false
COVERAGE=false
VERBOSE=false
FAIL_FAST=false
SKIP_BUILD=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Test results tracking
declare -A test_results
declare -A test_durations
start_time=$(date +%s)

# Services to test
services=(
    "rag-shared"
    "rag-auth-service"
    "rag-admin-service"
    "rag-document-service"
    "rag-embedding-service"
    "rag-core-service"
    "rag-integration-tests"
)

# Logging function
log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    mkdir -p "${LOG_DIR}"
    
    case $level in
        INFO)
            echo -e "${GREEN}[INFO]${NC} $message"
            echo "[$timestamp] [INFO] $message" >> "${LOG_DIR}/test-runner.log"
            ;;
        WARN)
            echo -e "${YELLOW}[WARN]${NC} $message"
            echo "[$timestamp] [WARN] $message" >> "${LOG_DIR}/test-runner.log"
            ;;
        ERROR)
            echo -e "${RED}[ERROR]${NC} $message"
            echo "[$timestamp] [ERROR] $message" >> "${LOG_DIR}/test-runner.log"
            ;;
        DEBUG)
            if [[ "$VERBOSE" == "true" ]]; then
                echo -e "${BLUE}[DEBUG]${NC} $message"
            fi
            echo "[$timestamp] [DEBUG] $message" >> "${LOG_DIR}/test-runner.log"
            ;;
        SUCCESS)
            echo -e "${GREEN}[SUCCESS]${NC} $message"
            echo "[$timestamp] [SUCCESS] $message" >> "${LOG_DIR}/test-runner.log"
            ;;
    esac
}

# Error handler
error_exit() {
    log ERROR "$1"
    generate_test_report
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
            --parallel)
                PARALLEL=true
                shift
                ;;
            --coverage)
                COVERAGE=true
                shift
                ;;
            --verbose)
                VERBOSE=true
                shift
                ;;
            --fail-fast)
                FAIL_FAST=true
                shift
                ;;
            --skip-build)
                SKIP_BUILD=true
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
}

# Show help message
show_help() {
    cat << EOF
Enterprise RAG System - Integration Test Runner

Usage: $0 [options]

Options:
  --service <name>     Run tests for specific service only
  --parallel           Run service tests in parallel (faster but less readable)
  --coverage           Generate test coverage reports
  --verbose            Enable verbose output
  --fail-fast          Stop on first test failure
  --skip-build         Skip compilation before testing
  -h, --help           Show this help message

Available services:
$(printf "  - %s\n" "${services[@]}")

Examples:
  $0                                    # Run all tests
  $0 --service rag-embedding-service    # Test specific service
  $0 --parallel --coverage              # Fast parallel tests with coverage
  $0 --verbose --fail-fast               # Verbose output, stop on failure

Test results are saved to: ${RESULTS_DIR}/
Coverage reports are saved to: ${COVERAGE_DIR}/
Logs are saved to: ${LOG_DIR}/
EOF
}

# Setup test environment
setup_test_environment() {
    log INFO "Setting up test environment..."
    
    # Create directories
    mkdir -p "${LOG_DIR}" "${RESULTS_DIR}" "${COVERAGE_DIR}"
    
    # Check if Docker infrastructure is running
    if ! docker-compose ps | grep -q "Up"; then
        log WARN "Docker infrastructure not running. Starting required services..."
        cd "${PROJECT_ROOT}"
        docker-compose up -d postgres redis kafka
        
        # Wait for services to be ready
        local max_attempts=30
        local attempt=0
        
        while [[ $attempt -lt $max_attempts ]]; do
            if docker-compose exec -T postgres pg_isready -U rag_user -d rag_enterprise &> /dev/null && \
               docker-compose exec -T redis redis-cli ping &> /dev/null; then
                log INFO "Infrastructure services are ready"
                break
            fi
            
            ((attempt++))
            log DEBUG "Waiting for infrastructure... (attempt $attempt/$max_attempts)"
            sleep 2
        done
        
        if [[ $attempt -eq $max_attempts ]]; then
            error_exit "Infrastructure services failed to start within 60 seconds"
        fi
    fi
    
    # Build projects if not skipped
    if [[ "$SKIP_BUILD" == "false" ]]; then
        log INFO "Building projects before testing..."
        cd "${PROJECT_ROOT}"
        
        if [[ "$VERBOSE" == "true" ]]; then
            mvn clean compile test-compile
        else
            mvn clean compile test-compile &> "${LOG_DIR}/build.log"
        fi
        
        if [[ $? -ne 0 ]]; then
            error_exit "Build failed. Check ${LOG_DIR}/build.log for details"
        fi
        log SUCCESS "Build completed successfully"
    fi
}

# Run tests for a single service
run_service_tests() {
    local service=$1
    local service_start_time=$(date +%s)
    
    log INFO "Running tests for $service..."
    
    # Check if service directory exists
    if [[ ! -d "${PROJECT_ROOT}/${service}" ]]; then
        log WARN "Service directory not found: $service"
        test_results[$service]="SKIPPED"
        test_durations[$service]=0
        return 0
    fi
    
    cd "${PROJECT_ROOT}/${service}"
    
    # Prepare Maven command
    local mvn_cmd="mvn test"
    local log_file="${LOG_DIR}/${service}-test.log"
    
    # Add coverage if requested
    if [[ "$COVERAGE" == "true" ]]; then
        mvn_cmd="$mvn_cmd jacoco:prepare-agent jacoco:report"
    fi
    
    # Add surefire report generation
    mvn_cmd="$mvn_cmd surefire-report:report"
    
    # Run tests
    if [[ "$VERBOSE" == "true" ]]; then
        $mvn_cmd | tee "$log_file"
        local exit_code=${PIPESTATUS[0]}
    else
        $mvn_cmd &> "$log_file"
        local exit_code=$?
    fi
    
    # Calculate duration
    local service_end_time=$(date +%s)
    local duration=$((service_end_time - service_start_time))
    test_durations[$service]=$duration
    
    # Process results
    if [[ $exit_code -eq 0 ]]; then
        test_results[$service]="PASSED"
        log SUCCESS "$service tests completed successfully (${duration}s)"
        
        # Copy test reports
        if [[ -d "target/surefire-reports" ]]; then
            mkdir -p "${RESULTS_DIR}/${service}"
            cp -r target/surefire-reports/* "${RESULTS_DIR}/${service}/" 2>/dev/null || true
        fi
        
        # Copy coverage reports
        if [[ "$COVERAGE" == "true" && -d "target/site/jacoco" ]]; then
            mkdir -p "${COVERAGE_DIR}/${service}"
            cp -r target/site/jacoco/* "${COVERAGE_DIR}/${service}/" 2>/dev/null || true
        fi
        
    else
        test_results[$service]="FAILED"
        log ERROR "$service tests failed (${duration}s). Check $log_file for details"
        
        # Show last few lines of failure
        if [[ "$VERBOSE" == "false" ]]; then
            echo -e "${RED}Last 10 lines of test output:${NC}"
            tail -n 10 "$log_file"
        fi
        
        if [[ "$FAIL_FAST" == "true" ]]; then
            error_exit "Stopping due to test failure in $service (--fail-fast enabled)"
        fi
    fi
}

# Run all tests
run_all_tests() {
    local services_to_test=()
    
    # Determine which services to test
    if [[ -n "$SPECIFIC_SERVICE" ]]; then
        if [[ " ${services[*]} " =~ " ${SPECIFIC_SERVICE} " ]]; then
            services_to_test=("$SPECIFIC_SERVICE")
        else
            error_exit "Unknown service: $SPECIFIC_SERVICE"
        fi
    else
        services_to_test=("${services[@]}")
    fi
    
    log INFO "Running tests for ${#services_to_test[@]} service(s)..."
    
    if [[ "$PARALLEL" == "true" && ${#services_to_test[@]} -gt 1 ]]; then
        log INFO "Running tests in parallel..."
        
        # Run tests in parallel
        local pids=()
        for service in "${services_to_test[@]}"; do
            (run_service_tests "$service") &
            pids+=($!)
        done
        
        # Wait for all tests to complete
        for pid in "${pids[@]}"; do
            wait $pid
        done
        
    else
        # Run tests sequentially
        for service in "${services_to_test[@]}"; do
            run_service_tests "$service"
        done
    fi
}

# Generate test report
generate_test_report() {
    local end_time=$(date +%s)
    local total_duration=$((end_time - start_time))
    
    log INFO "Generating test report..."
    
    # Create HTML report
    local report_file="${RESULTS_DIR}/test-summary.html"
    cat > "$report_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>RAG System Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #f0f0f0; padding: 20px; border-radius: 5px; }
        .summary { margin: 20px 0; }
        .service { margin: 10px 0; padding: 10px; border-radius: 3px; }
        .passed { background-color: #d4edda; color: #155724; }
        .failed { background-color: #f8d7da; color: #721c24; }
        .skipped { background-color: #fff3cd; color: #856404; }
        table { width: 100%; border-collapse: collapse; margin: 20px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
        .footer { margin-top: 30px; font-size: 0.9em; color: #666; }
    </style>
</head>
<body>
    <div class="header">
        <h1>RAG System Integration Test Report</h1>
        <p>Generated: $(date)</p>
        <p>Total Duration: ${total_duration} seconds</p>
    </div>
    
    <div class="summary">
        <h2>Test Summary</h2>
        <table>
            <tr>
                <th>Service</th>
                <th>Status</th>
                <th>Duration (seconds)</th>
            </tr>
EOF

    # Add service results to HTML report
    local total_services=0
    local passed_count=0
    local failed_count=0
    local skipped_count=0
    
    for service in "${services[@]}"; do
        if [[ -n "${test_results[$service]:-}" ]]; then
            local status="${test_results[$service]}"
            local duration="${test_durations[$service]:-0}"
            local css_class=""
            
            case $status in
                PASSED)
                    css_class="passed"
                    ((passed_count++))
                    ;;
                FAILED)
                    css_class="failed"
                    ((failed_count++))
                    ;;
                SKIPPED)
                    css_class="skipped"
                    ((skipped_count++))
                    ;;
            esac
            
            echo "            <tr class=\"$css_class\">" >> "$report_file"
            echo "                <td>$service</td>" >> "$report_file"
            echo "                <td>$status</td>" >> "$report_file"
            echo "                <td>$duration</td>" >> "$report_file"
            echo "            </tr>" >> "$report_file"
            
            ((total_services++))
        fi
    done

    cat >> "$report_file" << EOF
        </table>
        
        <h3>Statistics</h3>
        <ul>
            <li>Total Services: $total_services</li>
            <li>Passed: $passed_count</li>
            <li>Failed: $failed_count</li>
            <li>Skipped: $skipped_count</li>
        </ul>
    </div>
    
    <div class="footer">
        <p>Test logs available in: ${LOG_DIR}/</p>
        <p>Detailed reports available in: ${RESULTS_DIR}/</p>
EOF

    if [[ "$COVERAGE" == "true" ]]; then
        echo "        <p>Coverage reports available in: ${COVERAGE_DIR}/</p>" >> "$report_file"
    fi

    cat >> "$report_file" << EOF
    </div>
</body>
</html>
EOF

    # Create text summary
    local summary_file="${RESULTS_DIR}/test-summary.txt"
    cat > "$summary_file" << EOF
RAG System Integration Test Summary
==================================

Generated: $(date)
Total Duration: ${total_duration} seconds

Test Results:
EOF

    for service in "${services[@]}"; do
        if [[ -n "${test_results[$service]:-}" ]]; then
            local status="${test_results[$service]}"
            local duration="${test_durations[$service]:-0}"
            printf "%-30s %10s %8s seconds\n" "$service" "$status" "$duration" >> "$summary_file"
        fi
    done

    cat >> "$summary_file" << EOF

Statistics:
- Total Services: $total_services
- Passed: $passed_count
- Failed: $failed_count
- Skipped: $skipped_count

Files:
- Test logs: ${LOG_DIR}/
- Detailed reports: ${RESULTS_DIR}/
EOF

    if [[ "$COVERAGE" == "true" ]]; then
        echo "- Coverage reports: ${COVERAGE_DIR}/" >> "$summary_file"
    fi

    # Console summary
    echo ""
    echo "========================================"
    echo "          Test Results Summary"
    echo "========================================"
    echo ""
    
    for service in "${services[@]}"; do
        if [[ -n "${test_results[$service]:-}" ]]; then
            local status="${test_results[$service]}"
            local duration="${test_durations[$service]:-0}"
            
            case $status in
                PASSED)
                    printf "✅ %-30s ${GREEN}%10s${NC} %8s seconds\n" "$service" "$status" "$duration"
                    ;;
                FAILED)
                    printf "❌ %-30s ${RED}%10s${NC} %8s seconds\n" "$service" "$status" "$duration"
                    ;;
                SKIPPED)
                    printf "⏭️  %-30s ${YELLOW}%10s${NC} %8s seconds\n" "$service" "$status" "$duration"
                    ;;
            esac
        fi
    done
    
    echo ""
    echo "Statistics:"
    echo "- Total Services: $total_services"
    echo "- Passed: $passed_count"
    echo "- Failed: $failed_count"
    echo "- Skipped: $skipped_count"
    echo "- Total Duration: ${total_duration} seconds"
    echo ""
    echo "Reports generated:"
    echo "- HTML Report: $report_file"
    echo "- Text Summary: $summary_file"
    echo "- Test Logs: ${LOG_DIR}/"
    
    if [[ "$COVERAGE" == "true" ]]; then
        echo "- Coverage Reports: ${COVERAGE_DIR}/"
    fi
}

# Clean up test artifacts
cleanup_test_artifacts() {
    log INFO "Cleaning up test artifacts..."
    
    # Clean up old reports (keep last 5)
    find "${RESULTS_DIR}" -name "test-summary-*.html" -type f | sort -r | tail -n +6 | xargs rm -f
    find "${LOG_DIR}" -name "*.log" -mtime +7 -delete 2>/dev/null || true
    
    log DEBUG "Test artifact cleanup completed"
}

# Main function
main() {
    echo "🧪 Enterprise RAG System - Integration Test Runner"
    echo "=================================================="
    
    parse_args "$@"
    
    log INFO "Test run started with options: service=$SPECIFIC_SERVICE, parallel=$PARALLEL, coverage=$COVERAGE, verbose=$VERBOSE, fail-fast=$FAIL_FAST, skip-build=$SKIP_BUILD"
    
    cleanup_test_artifacts
    setup_test_environment
    run_all_tests
    generate_test_report
    
    # Exit with appropriate code
    local failed_count=0
    for service in "${services[@]}"; do
        if [[ "${test_results[$service]:-}" == "FAILED" ]]; then
            ((failed_count++))
        fi
    done
    
    if [[ $failed_count -eq 0 ]]; then
        log SUCCESS "All tests completed successfully! 🎉"
        exit 0
    else
        log ERROR "$failed_count service(s) failed tests"
        exit 1
    fi
}

# Run main function with all arguments
main "$@"