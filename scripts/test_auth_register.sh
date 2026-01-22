#!/bin/bash

# Test: Auth Register Function
# Tests registration with various inputs and validations

BASE_URL="http://localhost:8080/api"
TIMESTAMP=$(date +%s%N)
TEST_EMAIL="testuser_${TIMESTAMP}@example.com"

echo "=========================================="
echo "TEST: Auth Register Function"
echo "=========================================="
echo ""

# Test 1: Valid registration
echo "Test 1: Valid registration with strong password"
echo "Request: POST /auth/register"
curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"SecurePassword123!\"
  }" | jq '.'
echo ""
echo ""

# Test 2: Invalid email format
echo "Test 2: Invalid email format"
echo "Request: POST /auth/register (invalid email)"
curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"invalid-email\",
    \"password\": \"SecurePassword123!\"
  }" | jq '.'
echo ""
echo ""

# Test 3: Short password
echo "Test 3: Short password (password too weak)"
echo "Request: POST /auth/register"
curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"testuser_weak_${TIMESTAMP}@example.com\",
    \"password\": \"weak\"
  }" | jq '.'
echo ""
echo ""

# Test 4: Duplicate email
echo "Test 4: Duplicate email (registering same email twice)"
echo "Request: POST /auth/register"
echo "First registration..."
curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"duplicate_${TIMESTAMP}@example.com\",
    \"password\": \"SecurePassword123!\"
  }" > /dev/null
echo "Second registration with same email (should fail)..."
curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"duplicate_${TIMESTAMP}@example.com\",
    \"password\": \"SecurePassword123!\"
  }" | jq '.'
echo ""
echo ""

# Test 5: Empty email
echo "Test 5: Empty email field"
echo "Request: POST /auth/register"
curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"\",
    \"password\": \"SecurePassword123!\"
  }" | jq '.'
echo ""
echo ""

# Test 6: Empty password
echo "Test 6: Empty password field"
echo "Request: POST /auth/register"
curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"\"
  }" | jq '.'
echo ""
echo ""

# Test 7: Successfully registered user token
echo "Test 7: Verify registered user receives JWT token"
echo "Request: POST /auth/register"
RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"success_${TIMESTAMP}@example.com\",
    \"password\": \"SecurePassword123!\"
  }")
echo "$RESPONSE" | jq '.'
TOKEN=$(echo "$RESPONSE" | jq -r '.token // empty')
if [ ! -z "$TOKEN" ]; then
  echo "✓ JWT token received: ${TOKEN:0:30}..."
else
  echo "✗ No JWT token in response"
fi
echo ""
