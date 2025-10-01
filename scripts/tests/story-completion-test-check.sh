#!/bin/bash

# Story Completion Test Verification Script
# This script MUST be run before marking any story as complete

set -e  # Exit on any error

echo "🔍 STORY COMPLETION TEST VERIFICATION"
echo "======================================"
echo

# Check if service parameter provided
if [ -z "$1" ]; then
    echo "❌ ERROR: Service name required"
    echo "Usage: $0 <service-name>"
    echo "Example: $0 rag-auth-service"
    echo
    echo "Available services:"
    ls -1 | grep "^rag-" | grep -v ".md"
    exit 1
fi

SERVICE="$1"
SERVICE_DIR="/Users/stryfe/Projects/RAG/$SERVICE"

# Verify service exists
if [ ! -d "$SERVICE_DIR" ]; then
    echo "❌ ERROR: Service directory not found: $SERVICE_DIR"
    exit 1
fi

echo "📁 Testing service: $SERVICE"
echo "📍 Service directory: $SERVICE_DIR"
echo

# Step 1: Compilation Check
echo "🔨 STEP 1: COMPILATION CHECK"
echo "----------------------------"
cd "$SERVICE_DIR"

echo "Running: mvn compile -q"
if mvn compile -q; then
    echo "✅ Compilation: SUCCESS"
else
    echo "❌ Compilation: FAILED"
    echo "🚫 STORY CANNOT BE MARKED COMPLETE - COMPILATION ERRORS"
    exit 1
fi
echo

# Step 2: Test Execution
echo "🧪 STEP 2: TEST EXECUTION"
echo "-------------------------"

echo "Running: mvn test -q"
TEST_OUTPUT=$(mvn test 2>&1)
TEST_RESULT=$?

if [ $TEST_RESULT -eq 0 ]; then
    # Extract test counts from output
    TESTS_RUN=$(echo "$TEST_OUTPUT" | grep -o "Tests run: [0-9]*" | grep -o "[0-9]*" | head -1)
    FAILURES=$(echo "$TEST_OUTPUT" | grep -o "Failures: [0-9]*" | grep -o "[0-9]*" | head -1)
    ERRORS=$(echo "$TEST_OUTPUT" | grep -o "Errors: [0-9]*" | grep -o "[0-9]*" | head -1)
    SKIPPED=$(echo "$TEST_OUTPUT" | grep -o "Skipped: [0-9]*" | grep -o "[0-9]*" | head -1)
    
    # Default to 0 if not found
    TESTS_RUN=${TESTS_RUN:-0}
    FAILURES=${FAILURES:-0}
    ERRORS=${ERRORS:-0}
    SKIPPED=${SKIPPED:-0}
    
    PASSING=$((TESTS_RUN - FAILURES - ERRORS))
    
    echo "✅ Tests: SUCCESS"
    echo "📊 Test Results:"
    echo "   - Tests Run: $TESTS_RUN"
    echo "   - Passing: $PASSING"
    echo "   - Failures: $FAILURES"
    echo "   - Errors: $ERRORS"
    echo "   - Skipped: $SKIPPED"
    
    if [ "$FAILURES" -eq 0 ] && [ "$ERRORS" -eq 0 ]; then
        echo "🎉 ALL TESTS PASSING - STORY CAN BE MARKED COMPLETE"
        echo
        echo "📋 COPY THIS FOR STORY DOCUMENTATION:"
        echo "Test Results: $PASSING/$TESTS_RUN tests passing (100% success rate)"
    else
        echo "🚫 STORY CANNOT BE MARKED COMPLETE - FAILING TESTS DETECTED"
        echo "   - Failures: $FAILURES"
        echo "   - Errors: $ERRORS"
        exit 1
    fi
else
    # Extract failure details
    TESTS_RUN=$(echo "$TEST_OUTPUT" | grep -o "Tests run: [0-9]*" | grep -o "[0-9]*" | tail -1)
    FAILURES=$(echo "$TEST_OUTPUT" | grep -o "Failures: [0-9]*" | grep -o "[0-9]*" | tail -1)
    ERRORS=$(echo "$TEST_OUTPUT" | grep -o "Errors: [0-9]*" | grep -o "[0-9]*" | tail-1)
    
    echo "❌ Tests: FAILED"
    echo "📊 Test Results:"
    echo "   - Tests Run: ${TESTS_RUN:-unknown}"
    echo "   - Failures: ${FAILURES:-unknown}"
    echo "   - Errors: ${ERRORS:-unknown}"
    echo
    echo "🚫 STORY CANNOT BE MARKED COMPLETE - TESTS ARE FAILING"
    echo
    echo "🔍 Failure Details:"
    echo "$TEST_OUTPUT" | grep -A 5 -B 5 "FAILURE\|ERROR"
    exit 1
fi

echo
echo "✅ VERIFICATION COMPLETE - STORY CAN BE MARKED COMPLETE"
echo "📝 Remember to update story documentation with test results!"
echo
echo "🚨 CRITICAL REMINDER: This story has PASSED all tests and can be marked complete."
echo "🚨 If ANY test had failed, the story MUST remain 'IN PROGRESS' - NO EXCEPTIONS."
echo "🚨 Marking stories complete with failing tests is a critical process violation."