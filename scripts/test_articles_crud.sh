#!/bin/bash

# Test: Article CRUD Operations
# Tests create, read, update, delete operations

BASE_URL="http://localhost:8080/api"
TIMESTAMP=$(date +%s%N)

echo "=========================================="
echo "TEST: Article CRUD Operations"
echo "=========================================="
echo ""

# Step 1: Login to get token
echo "Step 1: Login to get JWT token"
RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"admin@example.com\",
    \"password\": \"password123\"
  }")
TOKEN=$(echo "$RESPONSE" | jq -r '.token // empty')
echo "✓ Token: ${TOKEN:0:30}..."
echo ""
echo ""

# Step 2: Create article
echo "Step 2: Create new article"
echo "Request: POST /articles"
CREATE_RESPONSE=$(curl -s -X POST "$BASE_URL/articles" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"Test Article $TIMESTAMP\",
    \"content\": \"This is a test article for CRUD operations. Content with some details.\"
  }")
echo "$CREATE_RESPONSE" | jq '.'
ARTICLE_ID=$(echo "$CREATE_RESPONSE" | jq -r '.id // empty')
echo "✓ Created article ID: $ARTICLE_ID"
echo ""
echo ""

# Step 3: Get single article
echo "Step 3: Get single article by ID"
echo "Request: GET /articles/$ARTICLE_ID"
curl -s -X GET "$BASE_URL/articles/$ARTICLE_ID" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""
echo ""

# Step 4: Get all articles (list with pagination)
echo "Step 4: Get all articles with pagination"
echo "Request: GET /articles?page=0&size=5"
curl -s -X GET "$BASE_URL/articles?page=0&size=5" \
  -H "Authorization: Bearer $TOKEN" | jq '{totalElements, totalPages, size, number}'
echo ""
echo ""

# Step 5: Get articles with title filter
echo "Step 5: Get articles filtered by title"
echo "Request: GET /articles?title=Test"
curl -s -X GET "$BASE_URL/articles?title=Test" \
  -H "Authorization: Bearer $TOKEN" | jq '.content | length' | xargs echo "✓ Found articles:"
echo ""
echo ""

# Step 6: Update article
echo "Step 6: Update article"
echo "Request: PUT /articles/$ARTICLE_ID"
UPDATE_RESPONSE=$(curl -s -X PUT "$BASE_URL/articles/$ARTICLE_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"Updated Article Title $TIMESTAMP\",
    \"content\": \"Updated content with more information and details.\"
  }")
echo "$UPDATE_RESPONSE" | jq '.'
echo ""
echo ""

# Step 7: Verify article was updated
echo "Step 7: Verify article was updated"
echo "Request: GET /articles/$ARTICLE_ID"
curl -s -X GET "$BASE_URL/articles/$ARTICLE_ID" \
  -H "Authorization: Bearer $TOKEN" | jq '{id, title, content}'
echo ""
echo ""

# Step 8: Delete article
echo "Step 8: Delete article"
echo "Request: DELETE /articles/$ARTICLE_ID"
curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X DELETE "$BASE_URL/articles/$ARTICLE_ID" \
  -H "Authorization: Bearer $TOKEN" | grep "HTTP_STATUS:" | cut -d: -f2 | xargs echo "✓ Delete response:"
echo ""
echo ""

# Step 9: Verify article is deleted
echo "Step 9: Verify article is deleted (should return 404)"
echo "Request: GET /articles/$ARTICLE_ID"
curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X GET "$BASE_URL/articles/$ARTICLE_ID" \
  -H "Authorization: Bearer $TOKEN" | tail -1 | cut -d: -f2 | xargs echo "✓ Response status:"
echo ""
echo ""

# Step 10: Create article with empty title (should fail)
echo "Step 10: Try to create article with empty title (should fail)"
echo "Request: POST /articles (empty title)"
curl -s -X POST "$BASE_URL/articles" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"\",
    \"content\": \"Content without title\"
  }" | jq '.'
echo ""
echo ""

# Step 11: Create article without authentication (should fail)
echo "Step 11: Try to create article without authentication (should fail)"
echo "Request: POST /articles (no token)"
curl -s -X POST "$BASE_URL/articles" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"Unauthorized Article\",
    \"content\": \"Content without authorization\"
  }" | jq '.'
echo ""
echo ""

echo "✓ Article CRUD tests completed"
