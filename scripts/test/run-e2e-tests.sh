#!/bin/bash

################################################################################
# RAG System End-to-End Test Runner
#
# This script:
# 1. Verifies all required services are running
# 2. Checks service health endpoints
# 3. Validates infrastructure (Postgres, Redis, Kafka, Ollama)
# 4. Runs comprehensive end-to-end tests
# 5. Generates test report
#
# Usage:
#   ./run-e2e-tests.sh [options]
#
# Options:
#   --skip-health-check    Skip service health checks
#   --skip-build          Skip Maven build before testing
#   --verbose             Enable verbose output
#   --test=<name>         Run specific test class or method
################################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SKIP_HEALTH_CHECK=false
SKIP_BUILD=false
VERBOSE=false
SPECIFIC_TEST=""

# Service URLs
AUTH_SERVICE_URL="http://localhost:8081"
DOCUMENT_SERVICE_URL="http://localhost:8082"
EMBEDDING_SERVICE_URL="http://localhost:8083"
CORE_SERVICE_URL="http://localhost:8084"
ADMIN_SERVICE_URL="http://localhost:8085"
POSTGRES_HOST="localhost"
POSTGRES_PORT="5432"
REDIS_HOST="localhost"
REDIS_PORT="6379"
KAFKA_HOST="localhost"
KAFKA_PORT="9092"
OLLAMA_URL="http://localhost:11434"

# Test results
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0

################################################################################
# Helper Functions
################################################################################

print_header() {
    echo -e "\n${CYAN}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}\n"
}

print_section() {
    echo -e "\n${BLUE}▶ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
    ((PASSED_CHECKS++))
    ((TOTAL_CHECKS++))
}

print_error() {
    echo -e "${RED}✗${NC} $1"
    ((FAILED_CHECKS++))
    ((TOTAL_CHECKS++))
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${CYAN}ℹ${NC} $1"
}

################################################################################
# Parse Arguments
################################################################################

parse_arguments() {
    for arg in "$@"; do
        case $arg in
            --skip-health-check)
                SKIP_HEALTH_CHECK=true
                shift
                ;;
            --skip-build)
                SKIP_BUILD=true
                shift
                ;;
            --verbose)
                VERBOSE=true
                shift
                ;;
            --test=*)
                SPECIFIC_TEST="${arg#*=}"
                shift
                ;;
            --help)
                echo "Usage: $0 [options]"
                echo ""
                echo "Options:"
                echo "  --skip-health-check    Skip service health checks"
                echo "  --skip-build          Skip Maven build before testing"
                echo "  --verbose             Enable verbose output"
                echo "  --test=<name>         Run specific test (e.g., ComprehensiveRagEndToEndIT)"
                echo "  --help                Show this help message"
                exit 0
                ;;
            *)
                print_error "Unknown option: $arg"
                exit 1
                ;;
        esac
    done
}

################################################################################
# Service Health Checks
################################################################################

check_service_http() {
    local service_name=$1
    local url=$2
    local max_retries=${3:-3}
    local retry_delay=${4:-2}

    for i in $(seq 1 $max_retries); do
        if curl -sf "$url" > /dev/null 2>&1; then
            print_success "$service_name is responding at $url"
            return 0
        fi
        if [ $i -lt $max_retries ]; then
            sleep $retry_delay
        fi
    done

    print_error "$service_name is not responding at $url"
    return 1
}

check_service_health_endpoint() {
    local service_name=$1
    local health_url=$2

    if response=$(curl -sf "$health_url" 2>&1); then
        status=$(echo "$response" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        if [ "$status" = "UP" ]; then
            print_success "$service_name health check: UP"
            return 0
        else
            print_error "$service_name health check: $status"
            return 1
        fi
    else
        print_error "$service_name health endpoint unreachable: $health_url"
        return 1
    fi
}

check_postgres() {
    print_section "Checking PostgreSQL"

    if command -v pg_isready &> /dev/null; then
        if pg_isready -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" > /dev/null 2>&1; then
            print_success "PostgreSQL is ready at $POSTGRES_HOST:$POSTGRES_PORT"
        else
            print_error "PostgreSQL is not ready"
            return 1
        fi
    else
        # Fallback: try to connect via Docker
        if docker exec rag-postgres pg_isready -U rag_user > /dev/null 2>&1; then
            print_success "PostgreSQL is ready (via Docker)"
        else
            print_error "PostgreSQL is not ready"
            return 1
        fi
    fi

    # Check if database exists
    if docker exec rag-postgres psql -U rag_user -d rag_enterprise -c "SELECT 1" > /dev/null 2>&1; then
        print_success "Database 'rag_enterprise' is accessible"
    else
        print_error "Database 'rag_enterprise' is not accessible"
        return 1
    fi
}

check_redis() {
    print_section "Checking Redis"

    if command -v redis-cli &> /dev/null; then
        if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a redis_password ping > /dev/null 2>&1; then
            print_success "Redis is responding at $REDIS_HOST:$REDIS_PORT"
        else
            print_error "Redis is not responding"
            return 1
        fi
    else
        # Fallback: try via Docker
        if docker exec rag-redis redis-cli -a redis_password ping > /dev/null 2>&1; then
            print_success "Redis is responding (via Docker)"
        else
            print_error "Redis is not responding"
            return 1
        fi
    fi
}

check_kafka() {
    print_section "Checking Kafka"

    # Check if Kafka broker is listening
    if nc -z "$KAFKA_HOST" "$KAFKA_PORT" 2>/dev/null; then
        print_success "Kafka broker is listening at $KAFKA_HOST:$KAFKA_PORT"
    else
        print_error "Kafka broker is not listening"
        return 1
    fi

    # Check via Docker
    if docker exec rag-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
        print_success "Kafka broker API is responding"
    else
        print_warning "Kafka broker API check inconclusive"
    fi
}

check_ollama() {
    print_section "Checking Ollama"

    if check_service_http "Ollama" "$OLLAMA_URL" 3 2; then
        # Check if required model is available
        if models=$(curl -sf "$OLLAMA_URL/api/tags" 2>&1); then
            if echo "$models" | grep -q "llama3.2:1b\|nomic-embed-text"; then
                print_success "Ollama has required models installed"
            else
                print_warning "Ollama is running but required models may not be installed"
                print_info "Required models: llama3.2:1b, nomic-embed-text"
            fi
        fi
    else
        return 1
    fi
}

check_microservices() {
    print_section "Checking Microservices"

    local all_healthy=true

    # Auth Service
    if check_service_health_endpoint "Auth Service" "$AUTH_SERVICE_URL/actuator/health"; then
        :
    else
        all_healthy=false
    fi

    # Document Service
    if check_service_health_endpoint "Document Service" "$DOCUMENT_SERVICE_URL/actuator/health"; then
        :
    else
        all_healthy=false
    fi

    # Embedding Service
    if check_service_health_endpoint "Embedding Service" "$EMBEDDING_SERVICE_URL/actuator/health"; then
        :
    else
        all_healthy=false
    fi

    # Core Service
    if check_service_health_endpoint "Core Service" "$CORE_SERVICE_URL/actuator/health"; then
        :
    else
        all_healthy=false
    fi

    # Admin Service
    if check_service_health_endpoint "Admin Service" "$ADMIN_SERVICE_URL/actuator/health"; then
        :
    else
        all_healthy=false
    fi

    if [ "$all_healthy" = true ]; then
        return 0
    else
        return 1
    fi
}

check_docker_containers() {
    print_section "Checking Docker Containers"

    required_containers=(
        "rag-postgres"
        "rag-redis"
        "rag-kafka"
        "rag-zookeeper"
        "rag-ollama"
        "rag-auth"
        "rag-document"
        "rag-embedding"
        "rag-core"
        "rag-admin"
    )

    local all_running=true

    for container in "${required_containers[@]}"; do
        if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
            status=$(docker inspect -f '{{.State.Status}}' "$container")
            if [ "$status" = "running" ]; then
                print_success "Container $container is running"
            else
                print_error "Container $container is $status"
                all_running=false
            fi
        else
            print_error "Container $container is not found"
            all_running=false
        fi
    done

    if [ "$all_running" = true ]; then
        return 0
    else
        print_warning "Some containers are not running. Try: docker-compose up -d"
        return 1
    fi
}

################################################################################
# Test Execution
################################################################################

run_tests() {
    print_section "Running End-to-End Tests"

    cd "$PROJECT_ROOT"

    # Build if not skipped
    if [ "$SKIP_BUILD" = false ]; then
        print_info "Building project..."
        if [ "$VERBOSE" = true ]; then
            ./mvnw clean install -DskipTests
        else
            ./mvnw clean install -DskipTests > /dev/null 2>&1
        fi
        print_success "Build completed"
    fi

    # Construct test command
    local test_cmd="./mvnw test -pl rag-integration-tests"

    if [ -n "$SPECIFIC_TEST" ]; then
        test_cmd="$test_cmd -Dtest=$SPECIFIC_TEST"
        print_info "Running specific test: $SPECIFIC_TEST"
    else
        test_cmd="$test_cmd -Dtest=ComprehensiveRagEndToEndIT"
        print_info "Running comprehensive E2E tests"
    fi

    if [ "$VERBOSE" = true ]; then
        test_cmd="$test_cmd -X"
    fi

    # Run tests
    print_info "Executing: $test_cmd"
    echo ""

    if $test_cmd; then
        print_success "All tests passed!"
        return 0
    else
        print_error "Some tests failed"
        return 1
    fi
}

################################################################################
# Main Execution
################################################################################

main() {
    print_header "RAG System End-to-End Test Runner"

    parse_arguments "$@"

    # Check if we're in the right directory
    if [ ! -f "$PROJECT_ROOT/pom.xml" ]; then
        print_error "Not in RAG project root directory"
        exit 1
    fi

    print_info "Project root: $PROJECT_ROOT"
    print_info "Test configuration:"
    print_info "  - Skip health check: $SKIP_HEALTH_CHECK"
    print_info "  - Skip build: $SKIP_BUILD"
    print_info "  - Verbose: $VERBOSE"
    if [ -n "$SPECIFIC_TEST" ]; then
        print_info "  - Specific test: $SPECIFIC_TEST"
    fi

    # Health checks (unless skipped)
    if [ "$SKIP_HEALTH_CHECK" = false ]; then
        print_header "Service Health Checks"

        check_docker_containers || true
        check_postgres || true
        check_redis || true
        check_kafka || true
        check_ollama || true
        check_microservices || true

        # Summary
        print_section "Health Check Summary"
        echo -e "Total checks: ${TOTAL_CHECKS}"
        echo -e "${GREEN}Passed: ${PASSED_CHECKS}${NC}"
        if [ $FAILED_CHECKS -gt 0 ]; then
            echo -e "${RED}Failed: ${FAILED_CHECKS}${NC}"
            print_warning "Some health checks failed, but continuing with tests..."
            print_warning "Tests may fail if critical services are unavailable"
        fi

        # Wait a moment for services to settle
        print_info "Waiting 5 seconds for services to stabilize..."
        sleep 5
    fi

    # Run tests
    print_header "Test Execution"

    if run_tests; then
        print_header "Test Execution Complete"
        print_success "All end-to-end tests passed successfully!"

        # Show test report location
        echo ""
        print_info "Test reports available at:"
        print_info "  - Surefire: $PROJECT_ROOT/rag-integration-tests/target/surefire-reports"
        print_info "  - HTML Report: $PROJECT_ROOT/rag-integration-tests/target/surefire-reports/index.html"

        exit 0
    else
        print_header "Test Execution Failed"
        print_error "Some tests failed. Check the output above for details."

        echo ""
        print_info "Test reports available at:"
        print_info "  - Surefire: $PROJECT_ROOT/rag-integration-tests/target/surefire-reports"

        echo ""
        print_info "Troubleshooting tips:"
        print_info "  1. Check service logs: docker-compose logs [service-name]"
        print_info "  2. Verify all services are healthy: ./scripts/utils/health-check.sh"
        print_info "  3. Check database migrations: docker exec rag-postgres psql -U rag_user -d rag_enterprise"
        print_info "  4. Review test logs in target/surefire-reports"

        exit 1
    fi
}

# Trap errors
trap 'echo -e "\n${RED}Error occurred at line $LINENO${NC}"; exit 1' ERR

# Run main function
main "$@"
