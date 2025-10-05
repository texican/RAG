#!/bin/bash

################################################################################
# Comprehensive RAG E2E Test Suite Runner
#
# This script:
# 1. Validates system prerequisites
# 2. Checks service health
# 3. Compiles tests
# 4. Runs comprehensive end-to-end tests
# 5. Generates report summary
################################################################################

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

################################################################################
# Functions
################################################################################

print_header() {
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo ""
}

print_section() {
    echo -e "\n${BLUE}▶ $1${NC}"
}

print_ok() {
    echo -e "${GREEN}✓${NC} $1"
}

print_fail() {
    echo -e "${RED}✗${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${CYAN}ℹ${NC} $1"
}

check_service() {
    local name=$1
    local port=$2

    if curl -sf "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
        print_ok "$name (port $port)"
        return 0
    else
        print_fail "$name (port $port) - not responding"
        return 1
    fi
}

################################################################################
# Main Script
################################################################################

print_header "RAG System - Comprehensive E2E Test Suite"

# Step 1: Prerequisites
print_section "Step 1: Checking Prerequisites"

if docker info > /dev/null 2>&1; then
    print_ok "Docker is running"
else
    print_fail "Docker is not running"
    exit 1
fi

if command -v mvn > /dev/null 2>&1; then
    print_ok "Maven is installed"
else
    print_fail "Maven is not installed"
    exit 1
fi

if java -version 2>&1 | grep -q "version \"17\|version \"2"; then
    print_ok "Java 17+ detected"
else
    print_warn "Java version check inconclusive"
fi

# Step 2: Service Health
print_section "Step 2: Verifying Service Health"

SERVICES_OK=true

check_service "Auth Service" 8081 || SERVICES_OK=false
check_service "Document Service" 8082 || SERVICES_OK=false
check_service "Embedding Service" 8083 || SERVICES_OK=false
check_service "Core Service" 8084 || SERVICES_OK=false

if [ "$SERVICES_OK" = false ]; then
    print_warn "Some services are not healthy - tests may fail"
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Step 3: Infrastructure
print_section "Step 3: Checking Infrastructure"

if docker exec rag-postgres pg_isready -U rag_user > /dev/null 2>&1; then
    print_ok "PostgreSQL is ready"
else
    print_fail "PostgreSQL is not ready"
    exit 1
fi

if docker exec rag-redis redis-cli -a redis_password ping 2>&1 | grep -q PONG; then
    print_ok "Redis is ready"
else
    print_fail "Redis is not ready"
    exit 1
fi

if curl -sf http://localhost:11434/api/tags > /dev/null 2>&1; then
    print_ok "Ollama is responding"

    if curl -sf http://localhost:11434/api/tags | grep -q "llama3.2:1b"; then
        print_ok "Ollama model llama3.2:1b is available"
    else
        print_warn "Ollama model llama3.2:1b not found - downloading may be needed"
    fi
else
    print_fail "Ollama is not responding"
    exit 1
fi

# Step 4: Test Resources
print_section "Step 4: Verifying Test Resources"

TEST_DOCS=(
    "rag-integration-tests/src/test/resources/test-documents/company-policy.md"
    "rag-integration-tests/src/test/resources/test-documents/product-specification.md"
    "rag-integration-tests/src/test/resources/test-documents/api-documentation.md"
)

for doc in "${TEST_DOCS[@]}"; do
    if [ -f "$doc" ]; then
        filename=$(basename "$doc")
        print_ok "Test document: $filename"
    else
        print_fail "Missing: $(basename "$doc")"
        exit 1
    fi
done

if [ -f "rag-integration-tests/src/test/java/com/byo/rag/integration/endtoend/ComprehensiveRagEndToEndIT.java" ]; then
    print_ok "E2E test class found"
else
    print_fail "E2E test class not found"
    exit 1
fi

# Step 5: Compile Tests
print_section "Step 5: Compiling Tests"

print_info "Compiling integration tests..."
if mvn test-compile -pl rag-integration-tests -q > /tmp/compile-output.log 2>&1; then
    print_ok "Tests compiled successfully"
else
    print_fail "Compilation failed"
    echo "Compilation errors:"
    cat /tmp/compile-output.log
    exit 1
fi

# Step 6: Run Tests
print_header "Running Comprehensive End-to-End Tests"

print_info "This may take 5-10 minutes depending on your system..."
print_info "The tests will:"
print_info "  • Upload 3 real-world documents (security policy, product spec, API docs)"
print_info "  • Process documents through the RAG pipeline"
print_info "  • Execute semantic search queries"
print_info "  • Validate response quality and citations"
print_info "  • Test multi-document context assembly"
echo ""

# Run all E2E tests
mvn test -pl rag-integration-tests \
    -Dtest=ComprehensiveRagEndToEndIT \
    -DfailIfNoTests=false \
    2>&1 | tee /tmp/e2e-test-output.log

TEST_RESULT=${PIPESTATUS[0]}

# Step 7: Results
print_header "Test Results Summary"

if [ $TEST_RESULT -eq 0 ]; then
    print_ok "ALL E2E TESTS PASSED!"

    echo ""
    print_info "Test execution summary:"

    # Extract test counts from log
    if grep -q "Tests run:" /tmp/e2e-test-output.log; then
        grep "Tests run:" /tmp/e2e-test-output.log | tail -1
    fi

    echo ""
    print_info "What was tested:"
    print_info "  ✓ E2E-001: Complete RAG pipeline (upload → process → query)"
    print_info "  ✓ E2E-002: Semantic search quality with query variations"
    print_info "  ✓ E2E-003: Multi-document context assembly"
    print_info "  ✓ E2E-004: Response quality and citation accuracy"
    print_info "  ✓ E2E-005: System performance under load"

else
    print_fail "SOME TESTS FAILED"

    echo ""
    print_info "Checking for failures..."

    # Extract failures from log
    if grep -A 10 "FAILURE" /tmp/e2e-test-output.log > /tmp/failures.log 2>&1; then
        echo ""
        echo "Failed tests:"
        cat /tmp/failures.log
    fi
fi

echo ""
print_info "Detailed reports available at:"
print_info "  • Surefire Reports: rag-integration-tests/target/surefire-reports/"
print_info "  • Full Output Log: /tmp/e2e-test-output.log"

echo ""
print_header "Test Execution Complete"

exit $TEST_RESULT
