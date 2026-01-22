#!/bin/bash

# Test: Token Validation
# Tests token validation endpoint and various token states

BASE_URL="http://localhost:8080/api"

echo "=========================================="
echo "TEST: Token Validation"
echo "=========================================="
echo ""

# Step 1: Get a valid token
echo "Step 1: Get valid token from login"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"admin@example.com\",
    \"password\": \"password123\"
  }")
TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token // empty')
echo "✓ Token: ${TOKEN:0:30}..."
echo ""
echo ""

# Step 2: Validate valid token
echo "Step 2: Validate valid token"
echo "Request: POST /auth/validate"
VALIDATE_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/validate" \
  -H "Content-Type: application/json" \
  -d "{ \"token\": \"$TOKEN\" }")
echo "$VALIDATE_RESPONSE" | jq '.'
echo ""
echo ""

# Step 3: Validate with invalid token format
echo "Step 3: Validate with invalid token format"
echo "Request: POST /auth/validate"
curl -s -X POST "$BASE_URL/auth/validate" \
  -H "Content-Type: application/json" \
  -d "{ \"token\": \"invalid.token.format\" }" | jq '.'
echo ""
echo ""

# Step 4: Validate with random string
echo "Step 4: Validate with random string (not a JWT)"
echo "Request: POST /auth/validate"
curl -s -X POST "$BASE_URL/auth/validate" \
  -H "Content-Type: application/json" \
  -d "{ \"token\": \"randomstring12345\" }" | jq '.'
echo ""
echo ""

# Step 5: Logout then validate (token should be blacklisted)
echo "Step 5: Logout and then validate same token (should be blacklisted)"
echo "Logging out..."
curl -s -X POST "$BASE_URL/auth/logout" \
  -H "Authorization: Bearer $TOKEN" > /dev/null
echo "✓ Logged out"
echo ""
echo "Validating blacklisted token..."
curl -s -X POST "$BASE_URL/auth/validate" \
  -H "Content-Type: application/json" \
  -d "{ \"token\": \"$TOKEN\" }" | jq '.'
echo ""
echo ""

# Step 6: Validate with empty token
echo "Step 6: Validate with empty token"
echo "Request: POST /auth/validate"
curl -s -X POST "$BASE_URL/auth/validate" \
  -H "Content-Type: application/json" \
  -d "{ \"token\": \"\" }" | jq '.'
echo ""
echo ""

# Step 7: Validate with null/missing token
echo "Step 7: Validate with missing token field"
echo "Request: POST /auth/validate"
curl -s -X POST "$BASE_URL/auth/validate" \
  -H "Content-Type: application/json" \
  -d "{ }" | jq '.'
echo ""
echo ""

echo "✓ Token validation tests completed"
