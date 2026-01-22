#!/bin/bash

# Master Test Script
# Runs all test scripts in sequence

SCRIPTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║           FULLSTACK BLOG APP - COMPLETE TEST SUITE         ║"
echo "║                                                            ║"
echo "║  Testing Auth Service and Blog Service APIs               ║"
echo "║  Base URL: http://localhost:8080/api                      ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Check if docker services are running
echo "Checking if services are running..."
if curl -s http://localhost:8080 > /dev/null 2>&1; then
    echo "✓ Services are running"
else
    echo "✗ Services are not running. Please start docker-compose:"
    echo "  docker-compose up -d"
    exit 1
fi
echo ""
echo ""

# Define tests
TESTS=(
    "1. Authentication - Register"
    "2. Authentication - Login"
    "3. Authentication - Logout"
    "4. Authentication - Refresh Token"
    "5. Authentication - Token Validation"
    "6. Rate Limiting - Login Attempts"
    "7. Article CRUD Operations"
    "8. Complete Authentication Flow"
)

echo "Available Tests:"
for i in "${!TESTS[@]}"; do
    echo "  $((i+1)). ${TESTS[$i]}"
done
echo ""

# Default: run all
if [ "$1" == "" ] || [ "$1" == "all" ]; then
    echo "Running ALL tests..."
    echo ""
    
    tests_to_run=(
        "test_auth_register.sh"
        "test_auth_login.sh"
        "test_auth_logout.sh"
        "test_auth_refresh.sh"
        "test_auth_validation.sh"
        "test_rate_limit.sh"
        "test_articles_crud.sh"
        "test_auth_flow.sh"
    )
else
    # Run specific test
    test_num=$1
    case $test_num in
        1) tests_to_run=("test_auth_register.sh") ;;
        2) tests_to_run=("test_auth_login.sh") ;;
        3) tests_to_run=("test_auth_logout.sh") ;;
        4) tests_to_run=("test_auth_refresh.sh") ;;
        5) tests_to_run=("test_auth_validation.sh") ;;
        6) tests_to_run=("test_rate_limit.sh") ;;
        7) tests_to_run=("test_articles_crud.sh") ;;
        8) tests_to_run=("test_auth_flow.sh") ;;
        *)
            echo "Invalid test number. Usage: $0 [1-8 or 'all']"
            exit 1
            ;;
    esac
    echo "Running test $test_num: ${TESTS[$((test_num-1))]}"
    echo ""
fi

# Run tests
test_count=0
passed_count=0

for test_script in "${tests_to_run[@]}"; do
    test_count=$((test_count + 1))
    test_file="$SCRIPTS_DIR/$test_script"
    
    if [ -f "$test_file" ]; then
        echo ""
        echo "═══════════════════════════════════════════════════════════"
        bash "$test_file"
        echo "═══════════════════════════════════════════════════════════"
        echo ""
        passed_count=$((passed_count + 1))
        
        # Ask to continue if running all tests
        if [ ${#tests_to_run[@]} -gt 1 ] && [ $test_count -lt ${#tests_to_run[@]} ]; then
            echo ""
            echo "Press Enter to continue to next test or Ctrl+C to stop..."
            read -r
        fi
    else
        echo "✗ Test file not found: $test_file"
    fi
done

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║                    TEST SUMMARY                            ║"
echo "║                                                            ║"
echo "║  Total Tests: $test_count"
echo "║  Passed: $passed_count"
echo "║  Failed: $((test_count - passed_count))"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
