#!/bin/bash

# Test: Auth Refresh Token Function
# Tests token refresh and token rotation

BASE_URL="http://localhost:8080/api"

echo "=========================================="
echo "TEST: Auth Refresh Token Function"
echo "=========================================="
echo ""

# Step 1: Login to get access token and refresh token
echo "Step 1: Login to get tokens"
echo "Request: POST /auth/login"
RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"admin@example.com\",
    \"password\": \"password123\"
  }")
echo "$RESPONSE" | jq '.'
ACCESS_TOKEN=$(echo "$RESPONSE" | jq -r '.token // empty')
REFRESH_TOKEN=$(echo "$RESPONSE" | jq -r '.refreshToken // empty')
USERNAME=$(echo "$RESPONSE" | jq -r '.email // empty')
echo "✓ Access Token: ${ACCESS_TOKEN:0:30}..."
echo "✓ Refresh Token: ${REFRESH_TOKEN:0:30}..."
echo "✓ Username: $USERNAME"
echo ""
echo ""

# Step 2: Refresh with valid refresh token
echo "Step 2: Refresh access token with valid refresh token"
echo "Request: POST /auth/refresh"
REFRESH_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"$USERNAME\",
    \"refreshToken\": \"$REFRESH_TOKEN\"
  }")
echo "$REFRESH_RESPONSE" | jq '.'
NEW_ACCESS_TOKEN=$(echo "$REFRESH_RESPONSE" | jq -r '.token // empty')
echo "✓ New Access Token: ${NEW_ACCESS_TOKEN:0:30}..."
echo ""
echo ""

# Step 3: Verify new token is different from old token
echo "Step 3: Verify new token is different (new JTI)"
if [ "$ACCESS_TOKEN" != "$NEW_ACCESS_TOKEN" ]; then
  echo "✓ New token is different (JTI rotated)"
else
  echo "✗ Token is same (token rotation failed)"
fi
echo ""
echo ""

# Step 4: Try to use old refresh token (should fail due to token rotation)
echo "Step 4: Try to refresh with old refresh token (should fail - token rotation)"
echo "Request: POST /auth/refresh"
curl -s -X POST "$BASE_URL/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"$USERNAME\",
    \"refreshToken\": \"$REFRESH_TOKEN\"
  }" | jq '.'
echo ""
echo ""

# Step 5: Refresh without username
echo "Step 5: Refresh without username (should fail)"
echo "Request: POST /auth/refresh"
curl -s -X POST "$BASE_URL/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"\",
    \"refreshToken\": \"$REFRESH_TOKEN\"
  }" | jq '.'
echo ""
echo ""

# Step 6: Refresh without refresh token
echo "Step 6: Refresh without refresh token (should fail)"
echo "Request: POST /auth/refresh"
curl -s -X POST "$BASE_URL/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"$USERNAME\",
    \"refreshToken\": \"\"
  }" | jq '.'
echo ""
echo ""

# Step 7: Refresh with invalid refresh token
echo "Step 7: Refresh with invalid refresh token (should fail)"
echo "Request: POST /auth/refresh"
curl -s -X POST "$BASE_URL/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"$USERNAME\",
    \"refreshToken\": \"invalid-token-12345\"
  }" | jq '.'
echo ""
echo ""

# Step 8: Use new token to access protected resource
echo "Step 8: Use new token to access articles (verify it works)"
echo "Request: GET /articles"
curl -s -X GET "$BASE_URL/articles" \
  -H "Authorization: Bearer $NEW_ACCESS_TOKEN" | jq '.totalElements' 2>/dev/null && echo "✓ New token works"
echo ""
echo ""

echo "✓ Refresh token tests completed"
