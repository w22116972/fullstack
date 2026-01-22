#!/bin/bash

# Quick Reference & Cheat Sheet for Testing

cat << 'EOF'
╔════════════════════════════════════════════════════════════════════════════╗
║                      TESTING QUICK REFERENCE GUIDE                        ║
║                                                                            ║
║              Blog Admin System - Full Stack Testing Suite                 ║
╚════════════════════════════════════════════════════════════════════════════╝

═══════════════════════════════════════════════════════════════════════════════
1. SETUP & PREREQUISITES
═══════════════════════════════════════════════════════════════════════════════

Start Docker Services:
  $ docker-compose up -d

Stop Docker Services:
  $ docker-compose down

Check Services Status:
  $ docker-compose ps

View Logs:
  $ docker-compose logs auth-service
  $ docker-compose logs blog-service
  $ docker-compose logs -f auth-cache

═══════════════════════════════════════════════════════════════════════════════
2. RUN TESTS
═══════════════════════════════════════════════════════════════════════════════

Run All Tests (Interactive):
  $ bash run_all_tests.sh all

Run Specific Test:
  $ bash run_all_tests.sh 1    # Register test
  $ bash run_all_tests.sh 2    # Login test
  $ bash run_all_tests.sh 3    # Logout test
  $ bash run_all_tests.sh 4    # Refresh token test
  $ bash run_all_tests.sh 5    # Token validation test
  $ bash run_all_tests.sh 6    # Rate limiting test
  $ bash run_all_tests.sh 7    # Article CRUD test
  $ bash run_all_tests.sh 8    # Complete flow test

Run Individual Test Script:
  $ bash test_auth_register.sh
  $ bash test_auth_login.sh
  $ bash test_auth_logout.sh
  $ bash test_auth_refresh.sh
  $ bash test_auth_validation.sh
  $ bash test_rate_limit.sh
  $ bash test_articles_crud.sh
  $ bash test_auth_flow.sh

═══════════════════════════════════════════════════════════════════════════════
3. TEST CREDENTIALS
═══════════════════════════════════════════════════════════════════════════════

Admin Account:
  Email:    admin@example.com
  Password: password123
  Role:     ADMIN

Test User (created during registration tests):
  Email:    testuser_{timestamp}@example.com
  Password: SecurePassword123!
  Role:     USER

═══════════════════════════════════════════════════════════════════════════════
4. API ENDPOINTS
═══════════════════════════════════════════════════════════════════════════════

BASE URL: http://localhost:8080/api

AUTHENTICATION:
  POST   /auth/register         - Register new user
  POST   /auth/login            - Login user
  POST   /auth/logout           - Logout user (blacklist token)
  POST   /auth/refresh          - Refresh access token
  POST   /auth/validate         - Validate token

ARTICLES:
  GET    /articles              - List articles (paginated)
  GET    /articles/{id}         - Get single article
  POST   /articles              - Create article
  PUT    /articles/{id}         - Update article
  DELETE /articles/{id}         - Delete article

═══════════════════════════════════════════════════════════════════════════════
5. CURL EXAMPLES (Manual Testing)
═══════════════════════════════════════════════════════════════════════════════

Register:
  curl -X POST http://localhost:8080/api/auth/register \
    -H "Content-Type: application/json" \
    -d '{"email":"user@example.com","password":"SecurePassword123!"}'

Login:
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@example.com","password":"password123"}'

Get Token from Response:
  TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@example.com","password":"password123"}' | jq -r '.token')
  
  echo "Token: $TOKEN"

Get Articles (with token):
  curl -X GET http://localhost:8080/api/articles \
    -H "Authorization: Bearer $TOKEN"

Create Article (with token):
  curl -X POST http://localhost:8080/api/articles \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"title":"My Article","content":"Article content here"}'

Validate Token:
  curl -X POST http://localhost:8080/api/auth/validate \
    -H "Content-Type: application/json" \
    -d "{\"token\":\"$TOKEN\"}"

Logout:
  curl -X POST http://localhost:8080/api/auth/logout \
    -H "Authorization: Bearer $TOKEN"

Refresh Token:
  curl -X POST http://localhost:8080/api/auth/refresh \
    -H "Content-Type: application/json" \
    -d '{"username":"admin@example.com","refreshToken":"'"$REFRESH_TOKEN"'"}'

═══════════════════════════════════════════════════════════════════════════════
6. TEST COVERAGE
═══════════════════════════════════════════════════════════════════════════════

Authentication:
  ✓ Register (valid/invalid inputs, duplicates)
  ✓ Login (valid/invalid credentials, rate limiting)
  ✓ Logout (token blacklisting, session invalidation)
  ✓ Refresh (token rotation, invalid tokens)
  ✓ Token Validation (valid/invalid/blacklisted tokens)
  ✓ Rate Limiting (5 attempts in 60 seconds)

Article Operations:
  ✓ Create (valid/invalid data, auth required)
  ✓ Read (list, single, pagination, filtering)
  ✓ Update (own articles, admin privileges)
  ✓ Delete (own articles, admin privileges)

Security:
  ✓ JWT validation & expiration
  ✓ JTI blacklist checking
  ✓ Session invalidation
  ✓ CORS validation
  ✓ Form validation

═══════════════════════════════════════════════════════════════════════════════
7. EXPECTED RESPONSES
═══════════════════════════════════════════════════════════════════════════════

Successful Register/Login (200/201):
  {
    "token": "eyJhbGci...",
    "refreshToken": "...",
    "username": "user@example.com",
    "email": "user@example.com",
    "role": "USER"
  }

Successful Article Create (200):
  {
    "id": 123,
    "title": "Article Title",
    "content": "Article content",
    "author": "user@example.com",
    "createdAt": "2024-01-22T10:30:00",
    "updatedAt": "2024-01-22T10:30:00"
  }

Successful List (200):
  {
    "content": [...],
    "totalElements": 42,
    "totalPages": 5,
    "size": 10,
    "number": 0
  }

Error - Invalid Credentials (401):
  {
    "error": "Invalid email or password"
  }

Error - Validation (400):
  {
    "error": "Email already registered"
  }

═══════════════════════════════════════════════════════════════════════════════
8. TROUBLESHOOTING
═══════════════════════════════════════════════════════════════════════════════

Services Not Running:
  → Check: docker-compose ps
  → Restart: docker-compose down && docker-compose up -d

Tests Failing (Connection Refused):
  → Verify Docker services are running
  → Check firewall settings
  → Ensure port 8080 is accessible

Invalid Token Errors:
  → Token may have expired (10 hour expiration)
  → Token may be blacklisted (after logout)
  → Token format may be invalid

Rate Limit Tests:
  → Default: 5 attempts per 60 seconds
  → Window resets after 60 seconds
  → Check auth-cache Redis for ratelimit keys

═══════════════════════════════════════════════════════════════════════════════
9. MONITORING & DEBUGGING
═══════════════════════════════════════════════════════════════════════════════

View Auth Service Logs:
  $ docker-compose logs -f auth-service

View Blog Service Logs:
  $ docker-compose logs -f blog-service

Check Redis (JTI Blacklist):
  $ docker exec -it fullstack-auth-cache-1 redis-cli
  > KEYS blacklist_jti:*
  > KEYS refresh:*
  > KEYS ratelimit:*
  > FLUSHDB  # Clear all data

Check Database:
  $ docker exec -it fullstack-database-1 psql -U postgres -d appdb
  > SELECT * FROM users;
  > SELECT * FROM articles;

═══════════════════════════════════════════════════════════════════════════════
10. DEMO MODE - RECOMMENDED SEQUENCE
═══════════════════════════════════════════════════════════════════════════════

For Live Demo:
  1. Run complete flow test (shows entire lifecycle)
       $ bash test_auth_flow.sh
  
  2. Show registration works (form validation)
       $ bash test_auth_register.sh
  
  3. Show login with rate limiting
       $ bash test_auth_login.sh
       $ bash test_rate_limit.sh
  
  4. Show token refresh
       $ bash test_auth_refresh.sh
  
  5. Show article CRUD operations
       $ bash test_articles_crud.sh
  
  6. Show logout and token blacklisting
       $ bash test_auth_logout.sh

═══════════════════════════════════════════════════════════════════════════════

For more details, see README.md in this directory.

EOF
