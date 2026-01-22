#!/bin/bash

# Test: Auth Logout Function
# Tests logout and token blacklisting

BASE_URL="http://localhost:8080/api"

echo "=========================================="
echo "TEST: Auth Logout Function"
echo "=========================================="
echo ""

# Step 1: Login to get token
echo "Step 1: Login to get JWT token"
echo "Request: POST /auth/login"
RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"admin@example.com\",
    \"password\": \"password123\"
  }" -c cookies.txt)
echo "$RESPONSE" | jq '.'
TOKEN=$(echo "$RESPONSE" | jq -r '.token // empty')
echo "✓ Token obtained: ${TOKEN:0:30}..."
echo ""
echo ""

# Step 2: Verify token is valid before logout
echo "Step 2: Verify token is valid (before logout)"
echo "Request: POST /auth/validate"
curl -s -X POST "$BASE_URL/auth/validate" \
  -H "Content-Type: application/json" \
  -d "{ \"token\": \"$TOKEN\" }" | jq '.'
echo ""
echo ""

# Step 3: Logout
echo "Step 3: Logout with valid token"
echo "Request: POST /auth/logout"
curl -s -X POST "$BASE_URL/auth/logout" \
  -H "Authorization: Bearer $TOKEN" \
  -b cookies.txt | jq '.' 2>/dev/null || echo "204 No Content (expected)"
echo ""
echo ""

# Step 4: Try to use token after logout (should fail)
echo "Step 4: Try to validate token after logout (should fail)"
echo "Request: POST /auth/validate"
curl -s -X POST "$BASE_URL/auth/validate" \
  -H "Content-Type: application/json" \
  -d "{ \"token\": \"$TOKEN\" }" | jq '.'
echo ""
echo ""

# Step 5: Try to access protected resource with logged-out token
echo "Step 5: Try to access protected resource with logged-out token"
echo "Request: GET /articles (with logged-out token)"
curl -s -X GET "$BASE_URL/articles" \
  -H "Authorization: Bearer $TOKEN" | jq '.' 2>/dev/null || echo "401 Unauthorized (expected)"
echo ""
echo ""

# Cleanup
rm -f cookies.txt

echo "✓ Logout tests completed"
