#!/bin/bash

# Enterprise Quality Validation Script
# MANDATORY execution before claiming any work is "complete"

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

TOTAL_TESTS=0
PASSING_TESTS=0
FAILING_TESTS=0
BLOCKERS=()
BROKEN_FUNCTIONALITY=()
VERIFIED_WORKING=()

echo -e "${BLUE}🛡️  ENTERPRISE QUALITY VALIDATION${NC}"
echo "======================================================="
echo "MANDATORY compliance check before marking work complete"
echo ""

# Function to check test results
check_test_results() {
    local service_name=$1
    local pom_file=$2
    
    echo -e "${YELLOW}Testing ${service_name}...${NC}"
    
    if mvn test -f "$pom_file" -q > "/tmp/test_output_${service_name}.log" 2>&1; then
        local test_count=$(grep -E "Tests run: [0-9]+" "/tmp/test_output_${service_name}.log" | tail -1 | sed 's/.*Tests run: \([0-9]*\).*/\1/')
        local failures=$(grep -E "Failures: [0-9]+" "/tmp/test_output_${service_name}.log" | tail -1 | sed 's/.*Failures: \([0-9]*\).*/\1/')
        local errors=$(grep -E "Errors: [0-9]+" "/tmp/test_output_${service_name}.log" | tail -1 | sed 's/.*Errors: \([0-9]*\).*/\1/')
        
        test_count=${test_count:-0}
        failures=${failures:-0}
        errors=${errors:-0}
        
        local failed=$((failures + errors))
        local passed=$((test_count - failed))
        
        TOTAL_TESTS=$((TOTAL_TESTS + test_count))
        PASSING_TESTS=$((PASSING_TESTS + passed))
        FAILING_TESTS=$((FAILING_TESTS + failed))
        
        if [ $failed -eq 0 ] && [ $test_count -gt 0 ]; then
            echo -e "  ✅ ${service_name}: ${passed}/${test_count} passing (100%)"
            VERIFIED_WORKING+=("${service_name} unit tests: ${test_count} tests passing")
        else
            echo -e "  ❌ ${service_name}: ${passed}/${test_count} passing (${failed} failures)"
            BLOCKERS+=("${service_name}: ${failed} test failures")
            BROKEN_FUNCTIONALITY+=("${service_name} test suite failing")
        fi
    else
        echo -e "  ❌ ${service_name}: BUILD FAILED"
        BLOCKERS+=("${service_name}: Build failure")
        BROKEN_FUNCTIONALITY+=("${service_name} cannot compile")
    fi
}

# Test all services
echo -e "${YELLOW}📋 Running Complete Test Suite${NC}"
echo "This may take several minutes..."
echo ""

check_test_results "Auth Service" "rag-auth-service/pom.xml"
check_test_results "Document Service" "rag-document-service/pom.xml" 
check_test_results "Embedding Service" "rag-embedding-service/pom.xml"
check_test_results "Core Service" "rag-core-service/pom.xml"
check_test_results "Admin Service" "rag-admin-service/pom.xml"

echo ""

# Check if Docker services are running
echo -e "${YELLOW}🐳 Checking Docker Services${NC}"
if docker-compose ps | grep -q "Up"; then
    echo -e "  ✅ Docker services running"
    VERIFIED_WORKING+=("Docker Compose services operational")
else
    echo -e "  ❌ Docker services not running"
    BLOCKERS+=("Docker Compose services not started")
    BROKEN_FUNCTIONALITY+=("Cannot test service endpoints")
fi

# Test critical endpoints if services are running
if docker-compose ps | grep -q "Up"; then
    echo -e "${YELLOW}🔗 Testing Critical Endpoints${NC}"
    
    # Test auth service
    if curl -s -f http://localhost:8081/actuator/health > /dev/null; then
        echo -e "  ✅ Auth service health endpoint"
        VERIFIED_WORKING+=("Auth service HTTP endpoint responding")
    else
        echo -e "  ❌ Auth service health endpoint"
        BROKEN_FUNCTIONALITY+=("Auth service not responding")
    fi
    
    # Test document service  
    if curl -s -f http://localhost:8082/actuator/health > /dev/null; then
        echo -e "  ✅ Document service health endpoint"
        VERIFIED_WORKING+=("Document service HTTP endpoint responding")
    else
        echo -e "  ❌ Document service health endpoint"
        BROKEN_FUNCTIONALITY+=("Document service not responding")
    fi
    
    # Test embedding service
    if curl -s -f http://localhost:8083/actuator/health > /dev/null; then
        echo -e "  ✅ Embedding service health endpoint"
        VERIFIED_WORKING+=("Embedding service HTTP endpoint responding")
    else
        echo -e "  ❌ Embedding service health endpoint"
        BROKEN_FUNCTIONALITY+=("Embedding service not responding")
    fi
    
    # Test core service
    if curl -s -f http://localhost:8084/actuator/health > /dev/null; then
        echo -e "  ✅ Core service health endpoint"
        VERIFIED_WORKING+=("Core service HTTP endpoint responding")
    else
        echo -e "  ❌ Core service health endpoint"
        BROKEN_FUNCTIONALITY+=("Core service not responding")
    fi
    
    # Test admin service
    if curl -s -f http://localhost:8085/admin/api/actuator/health > /dev/null; then
        echo -e "  ✅ Admin service health endpoint"
        VERIFIED_WORKING+=("Admin service HTTP endpoint responding")
    else
        echo -e "  ❌ Admin service health endpoint"
        BROKEN_FUNCTIONALITY+=("Admin service not responding")
    fi
fi

echo ""
echo "======================================================="
echo -e "${BLUE}📊 ENTERPRISE QUALITY REPORT${NC}"
echo "======================================================="

# Calculate pass rate
if [ $TOTAL_TESTS -gt 0 ]; then
    PASS_RATE=$((PASSING_TESTS * 100 / TOTAL_TESTS))
else
    PASS_RATE=0
fi

echo -e "${BLUE}## Test Results Summary${NC}"
if [ $FAILING_TESTS -eq 0 ] && [ $TOTAL_TESTS -gt 0 ]; then
    echo -e "✅ **Passing Tests**: ${PASSING_TESTS}/${TOTAL_TESTS} total (${PASS_RATE}%)"
else
    echo -e "❌ **Passing Tests**: ${PASSING_TESTS}/${TOTAL_TESTS} total (${PASS_RATE}%)"
fi

if [ $FAILING_TESTS -gt 0 ]; then
    echo -e "❌ **Failing Tests**: ${FAILING_TESTS} failures"
fi

echo ""
echo -e "${BLUE}## 🔧 Broken Functionality${NC}"
if [ ${#BROKEN_FUNCTIONALITY[@]} -eq 0 ]; then
    echo -e "✅ No broken functionality detected"
else
    for item in "${BROKEN_FUNCTIONALITY[@]}"; do
        echo -e "- ${item}"
    done
fi

echo ""
echo -e "${BLUE}## 🚫 Production Blockers${NC}"
if [ ${#BLOCKERS[@]} -eq 0 ]; then
    echo -e "✅ No production blockers identified"
else
    for blocker in "${BLOCKERS[@]}"; do
        echo -e "- ${blocker}"
    done
fi

echo ""
echo -e "${BLUE}## ✅ Verified Working${NC}"
if [ ${#VERIFIED_WORKING[@]} -eq 0 ]; then
    echo -e "❌ No functionality verified as working"
else
    for item in "${VERIFIED_WORKING[@]}"; do
        echo -e "- ${item}"
    done
fi

echo ""
echo "======================================================="

# Final quality gate assessment
if [ $FAILING_TESTS -eq 0 ] && [ $TOTAL_TESTS -gt 0 ] && [ ${#BLOCKERS[@]} -eq 0 ]; then
    echo -e "${GREEN}🎉 ENTERPRISE QUALITY GATE: PASSED${NC}"
    echo -e "✅ System meets enterprise production standards"
    exit 0
else
    echo -e "${RED}🚫 ENTERPRISE QUALITY GATE: FAILED${NC}"
    echo -e "❌ System does NOT meet enterprise production standards"
    echo ""
    echo -e "${YELLOW}REQUIRED ACTIONS:${NC}"
    echo "1. Fix all failing tests before proceeding"
    echo "2. Resolve all production blockers"  
    echo "3. Ensure all core functionality is working"
    echo "4. Re-run this validation script"
    echo ""
    echo -e "${RED}DO NOT DEPLOY TO PRODUCTION${NC}"
    exit 1
fi