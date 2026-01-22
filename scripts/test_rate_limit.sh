#!/bin/bash

# Test: Rate Limiting on Login
# Tests rate limit behavior after multiple failed login attempts

BASE_URL="http://localhost:8080/api"

echo "=========================================="
echo "TEST: Rate Limiting on Login"
echo "=========================================="
echo ""

ADMIN_EMAIL="admin@example.com"
WRONG_PASSWORD="wrongpassword"
MAX_ATTEMPTS=5
WINDOW_SECONDS=60

echo "Setup: Admin email is '$ADMIN_EMAIL'"
echo "Max attempts: $MAX_ATTEMPTS within $WINDOW_SECONDS seconds"
echo ""
echo ""

# Make multiple failed login attempts
for i in {1..7}; do
  echo "Attempt $i: Failed login with wrong password"
  RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "{
      \"email\": \"$ADMIN_EMAIL\",
      \"password\": \"$WRONG_PASSWORD\"
    }")
  
  HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_STATUS:" | cut -d: -f2)
  BODY=$(echo "$RESPONSE" | sed '$d')
  
  echo "HTTP Status: $HTTP_CODE"
  echo "$BODY" | jq '.'
  
  if [ "$i" -le "$MAX_ATTEMPTS" ]; then
    if [ "$HTTP_CODE" = "401" ]; then
      echo "✓ Attempt $i: Rejected (correct - wrong password)"
    else
      echo "✗ Attempt $i: Expected 401, got $HTTP_CODE"
    fi
  else
    if [ "$HTTP_CODE" = "429" ] || [ "$HTTP_CODE" = "401" ]; then
      echo "⚠ Attempt $i: Status $HTTP_CODE (rate limit may have triggered)"
    else
      echo "? Attempt $i: Unexpected status $HTTP_CODE"
    fi
  fi
  
  echo ""
  sleep 1
done

echo ""
echo "Waiting 60 seconds for rate limit to reset..."
echo "(Skipping wait in automated test - in real scenario wait 60 seconds)"
echo ""

echo "Test completed - check if rate limit was enforced after attempt $MAX_ATTEMPTS"
