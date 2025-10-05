#!/bin/bash

################################################################################
# Simple E2E Test Runner
################################################################################

set -e

cd "$(dirname "$0")/../.."

echo "======================================================================"
echo "  RAG System - End-to-End Test Execution"
echo "======================================================================"
echo ""

# Check critical services
echo "Checking critical services..."
for port in 8081 8082 8083 8084; do
    if curl -sf http://localhost:$port/actuator/health > /dev/null 2>&1; then
        echo "  ✓ Service on port $port is UP"
    else
        echo "  ✗ Service on port $port is DOWN - tests may fail"
    fi
done
echo ""

# Check test files exist
echo "Verifying test resources..."
if [ -f "rag-integration-tests/src/test/resources/test-documents/company-policy.md" ]; then
    echo "  ✓ Test documents found"
else
    echo "  ✗ Test documents missing"
    exit 1
fi

if [ -f "rag-integration-tests/src/test/java/com/byo/rag/integration/endtoend/ComprehensiveRagEndToEndIT.java" ]; then
    echo "  ✓ Test class found"
else
    echo "  ✗ Test class missing"
    exit 1
fi

echo ""
echo "======================================================================"
echo "  Starting Test Execution"
echo "======================================================================"
echo ""

# Run the tests
./mvnw test -pl rag-integration-tests \
    -Dtest=ComprehensiveRagEndToEndIT \
    -DfailIfNoTests=false \
    -Dspring.profiles.active=test

TEST_RESULT=$?

echo ""
echo "======================================================================"
if [ $TEST_RESULT -eq 0 ]; then
    echo "  ✓ ALL TESTS PASSED"
else
    echo "  ✗ SOME TESTS FAILED"
fi
echo "======================================================================"
echo ""
echo "Test reports: rag-integration-tests/target/surefire-reports/"
echo ""

exit $TEST_RESULT
