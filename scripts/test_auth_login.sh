#!/bin/bash

# Test: Auth Login Function
# Tests login with valid/invalid credentials

BASE_URL="http://localhost:8080/api"

echo "=========================================="
echo "TEST: Auth Login Function"
echo "=========================================="
echo ""

# Test 1: Valid login with admin account
echo "Test 1: Valid login with admin credentials"
echo "Request: POST /auth/login"
RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"admin@example.com\",
    \"password\": \"password123\"
  }")
echo "$RESPONSE" | jq '.'
TOKEN=$(echo "$RESPONSE" | jq -r '.token // empty')
REFRESH_TOKEN=$(echo "$RESPONSE" | jq -r '.refreshToken // empty')
echo "✓ Access Token: ${TOKEN:0:30}..."
echo "✓ Refresh Token: ${REFRESH_TOKEN:0:30}..."
echo ""
echo ""

# Test 2: Invalid email (account doesn't exist)
echo "Test 2: Invalid email (account doesn't exist)"
echo "Request: POST /auth/login"
curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"nonexistent_$(date +%s)@example.com\",
    \"password\": \"password123\"
  }" | jq '.'
echo ""
echo ""

# Test 3: Invalid password
echo "Test 3: Invalid password for existing account"
echo "Request: POST /auth/login"
curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"admin@example.com\",
    \"password\": \"wrongpassword\"
  }" | jq '.'
echo ""
echo ""

# Test 4: Empty email
echo "Test 4: Empty email field"
echo "Request: POST /auth/login"
curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"\",
    \"password\": \"password123\"
  }" | jq '.'
echo ""
echo ""

# Test 5: Empty password
echo "Test 5: Empty password field"
echo "Request: POST /auth/login"
curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"admin@example.com\",
    \"password\": \"\"
  }" | jq '.'
echo ""
echo ""

# Test 6: Both empty
echo "Test 6: Both email and password empty"
echo "Request: POST /auth/login"
curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"\",
    \"password\": \"\"
  }" | jq '.'
echo ""
echo ""

echo "✓ Login tests completed"
