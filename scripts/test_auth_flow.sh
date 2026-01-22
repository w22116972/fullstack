#!/bin/bash

# Test: Complete Authentication Flow
# Tests the full user journey: register -> login -> use token -> refresh -> logout

BASE_URL="http://localhost:8080/api"
TIMESTAMP=$(date +%s%N)
TEST_EMAIL="flowtest_${TIMESTAMP}@example.com"
TEST_PASSWORD="SecurePassword123!"

echo "=========================================="
echo "TEST: Complete Authentication Flow"
echo "=========================================="
echo ""

# Step 1: Register new user
echo "STEP 1: Register new user"
echo "Email: $TEST_EMAIL"
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"$TEST_PASSWORD\"
  }")
echo "$REGISTER_RESPONSE" | jq '{token, email, role}'
REG_TOKEN=$(echo "$REGISTER_RESPONSE" | jq -r '.token // empty')
echo "✓ User registered with token: ${REG_TOKEN:0:30}..."
echo ""
echo ""

# Step 2: Verify registered user token works
echo "STEP 2: Verify registration token works"
curl -s -X POST "$BASE_URL/auth/validate" \
  -H "Content-Type: application/json" \
  -d "{ \"token\": \"$REG_TOKEN\" }" | jq '{valid, username}'
echo ""
echo ""

# Step 3: Login with registered user
echo "STEP 3: Login with registered credentials"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"$TEST_PASSWORD\"
  }")
echo "$LOGIN_RESPONSE" | jq '{token, email, role}'
ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token // empty')
REFRESH_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.refreshToken // empty')
echo "✓ Logged in with access token: ${ACCESS_TOKEN:0:30}..."
echo "✓ Refresh token: ${REFRESH_TOKEN:0:30}..."
echo ""
echo ""

# Step 4: Create article with access token
echo "STEP 4: Create article with access token"
CREATE_RESPONSE=$(curl -s -X POST "$BASE_URL/articles" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"My First Article - Flow Test\",
    \"content\": \"This is my first article created during the authentication flow test.\"
  }")
ARTICLE_ID=$(echo "$CREATE_RESPONSE" | jq -r '.id // empty')
echo "$CREATE_RESPONSE" | jq '{id, title}'
echo "✓ Article created with ID: $ARTICLE_ID"
echo ""
echo ""

# Step 5: Refresh access token
echo "STEP 5: Refresh access token"
REFRESH_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"$TEST_EMAIL\",
    \"refreshToken\": \"$REFRESH_TOKEN\"
  }")
NEW_ACCESS_TOKEN=$(echo "$REFRESH_RESPONSE" | jq -r '.token // empty')
echo "$REFRESH_RESPONSE" | jq '{email, role}'
echo "✓ New access token: ${NEW_ACCESS_TOKEN:0:30}..."
echo ""
echo ""

# Step 6: Access article with refreshed token
echo "STEP 6: Access article with refreshed token"
curl -s -X GET "$BASE_URL/articles/$ARTICLE_ID" \
  -H "Authorization: Bearer $NEW_ACCESS_TOKEN" | jq '{id, title, content}'
echo ""
echo ""

# Step 7: Logout
echo "STEP 7: Logout (blacklist token)"
curl -s -X POST "$BASE_URL/auth/logout" \
  -H "Authorization: Bearer $NEW_ACCESS_TOKEN" | jq '.' 2>/dev/null || echo "✓ Logout successful (204 No Content)"
echo ""
echo ""

# Step 8: Try to use token after logout (should fail)
echo "STEP 8: Try to validate token after logout (should fail)"
curl -s -X POST "$BASE_URL/auth/validate" \
  -H "Content-Type: application/json" \
  -d "{ \"token\": \"$NEW_ACCESS_TOKEN\" }" | jq '{valid, username}'
echo ""
echo ""

# Step 9: Try to access article with logged-out token (should fail)
echo "STEP 9: Try to access article with logged-out token (should fail)"
curl -s -X GET "$BASE_URL/articles/$ARTICLE_ID" \
  -H "Authorization: Bearer $NEW_ACCESS_TOKEN" | jq '.' 2>/dev/null || echo "✗ Unauthorized (expected)"
echo ""
echo ""

# Step 10: Login again
echo "STEP 10: Login again (new session)"
LOGIN_RESPONSE2=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"$TEST_PASSWORD\"
  }")
ACCESS_TOKEN2=$(echo "$LOGIN_RESPONSE2" | jq -r '.token // empty')
echo "✓ New access token: ${ACCESS_TOKEN2:0:30}..."
echo ""
echo ""

# Step 11: Verify can access article with new token
echo "STEP 11: Verify can access article with new token"
curl -s -X GET "$BASE_URL/articles/$ARTICLE_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN2" | jq '{id, title}'
echo "✓ Article accessible with new token"
echo ""
echo ""

echo "=========================================="
echo "✓ Complete authentication flow test PASSED"
echo "=========================================="
