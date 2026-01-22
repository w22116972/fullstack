# Test Scripts & API Documentation

This folder contains comprehensive test scripts for the fullstack blog application.

## Base URL
```
http://localhost:8080/api
```

---

## 1. Authentication Tests

### 1.1 Register Function
**Endpoint:** `POST /auth/register`

**Test Cases:**
- ✅ Valid registration with valid email and strong password
  - Expected: 201 Created, returns JWT token
  - Email: `testuser@example.com`
  - Password: `SecurePassword123!`

- ❌ Invalid email format (form validation)
  - Expected: 400 Bad Request, validation error
  - Email: `invalid-email`
  - Password: `SecurePassword123!`

- ❌ Short password (form validation)
  - Expected: 400 Bad Request, password too weak
  - Email: `testuser2@example.com`
  - Password: `weak`

- ❌ Duplicate email
  - Expected: 400 Bad Request, "Email already registered"
  - Register same email twice

- ❌ Empty fields (form validation)
  - Expected: 400 Bad Request, field required
  - Email: `""`
  - Password: `""`

### 1.2 Login Function
**Endpoint:** `POST /auth/login`

**Test Cases:**
- ✅ Valid login with admin credentials
  - Expected: 200 OK, returns JWT token + refresh token
  - Email: `admin@example.com`
  - Password: `password123`
  - Response includes: `token`, `refreshToken`, `expiresIn`, `email`, `role`

- ✅ Valid login with user credentials
  - Expected: 200 OK, returns JWT token + refresh token
  - Register a user first, then login

- ❌ Invalid email (account doesn't exist)
  - Expected: 401 Unauthorized, "Invalid email or password"
  - Email: `nonexistent@example.com`
  - Password: `password123`

- ❌ Invalid password
  - Expected: 401 Unauthorized, "Invalid email or password"
  - Email: `admin@example.com`
  - Password: `wrongpassword`

- ❌ Both email and password empty
  - Expected: 400 Bad Request, validation error

### 1.3 Rate Limiting (Login Attempts)
**Endpoint:** `POST /auth/login`

**Test Cases:**
- ✅ First 5 failed login attempts should succeed
  - Make 5 requests with wrong password
  - Expected: 401 Unauthorized on each

- ❌ 6th failed attempt should be rate limited
  - Make 6th request with wrong password
  - Expected: 429 Too Many Requests or custom error

- ✅ Rate limit resets after 60 seconds
  - Wait 60 seconds
  - Make another login attempt
  - Expected: 401 Unauthorized (not rate limited)

### 1.4 Logout Function
**Endpoint:** `POST /auth/logout`

**Test Cases:**
- ✅ Successful logout with valid token
  - Expected: 200 OK
  - Token is blacklisted in Redis (JTI-based)
  - Response: empty body or `{"message": "Logged out successfully"}`

- ❌ Using token after logout should fail
  - Expected: 401 Unauthorized
  - Try to access protected endpoint with logged-out token
  - Message: "Token has been revoked" or "Unauthorized"

- ⚠️ Logout without token
  - Expected: 400 Bad Request or should fail gracefully

### 1.5 Refresh Token Function
**Endpoint:** `POST /auth/refresh`

**Test Cases:**
- ✅ Refresh with valid refresh token
  - Expected: 200 OK
  - Returns new JWT token with new JTI
  - Old refresh token is invalidated (token rotation)
  - Response includes: `token`, `username`, `email`, `role`

- ❌ Refresh with invalid/expired refresh token
  - Expected: 401 Unauthorized
  - Message: "Invalid or expired refresh token"

- ❌ Refresh without username
  - Expected: 400 Bad Request
  - Message: "Username and refresh token are required"

- ❌ Refresh without refresh token
  - Expected: 400 Bad Request
  - Message: "Username and refresh token are required"

- ❌ Refresh with wrong username/token combination
  - Expected: 401 Unauthorized

- ✅ Token rotation (new refresh token issued)
  - Refresh once, store new refresh token
  - Use new token to refresh again
  - Expected: 200 OK with another new token

### 1.6 Token Validation
**Endpoint:** `POST /auth/validate`

**Test Cases:**
- ✅ Validate with valid token
  - Expected: 200 OK
  - Response: `{"valid": true, "username": "user@example.com"}`

- ❌ Validate with invalid/expired token
  - Expected: 401 Unauthorized
  - Response: `{"valid": false, "username": null}`

- ❌ Validate with blacklisted token (after logout)
  - Expected: 401 Unauthorized

---

## 2. Article Management Tests

### 2.1 Create Article
**Endpoint:** `POST /articles`

**Test Cases:**
- ✅ Create article with valid data (authenticated user)
  - Expected: 200 OK, returns created article with ID
  - Title: `"Test Article Title"`
  - Content: `"This is test article content"`
  - Author: Auto-set from authenticated user

- ❌ Create article without authentication
  - Expected: 401 Unauthorized

- ❌ Create article with empty title
  - Expected: 400 Bad Request, validation error

- ❌ Create article with empty content
  - Expected: 400 Bad Request, validation error

- ✅ Create article with special characters
  - Title: `"Test Article with @#$% Special Chars"`
  - Expected: 200 OK, special chars preserved

### 2.2 Get All Articles (List with Pagination)
**Endpoint:** `GET /articles`

**Test Cases:**
- ✅ Get all articles with default pagination (page=0, size=20)
  - Expected: 200 OK, returns Page with articles
  - Response includes: `content[]`, `totalElements`, `totalPages`, `size`, `number`

- ✅ Get articles with custom page size
  - Query: `?page=0&size=5`
  - Expected: 200 OK, returns max 5 articles

- ✅ Get articles with pagination (page 2)
  - Query: `?page=1&size=10`
  - Expected: 200 OK, returns articles 10-19

- ✅ Get articles filtered by title
  - Query: `?title=test`
  - Expected: 200 OK, only articles with "test" in title

- ✅ Get articles with sorting
  - Query: `?sort=createdAt,desc`
  - Expected: 200 OK, sorted by creation date descending

- ❌ Invalid page number (negative)
  - Query: `?page=-1`
  - Expected: 400 Bad Request

- ❌ Invalid page size (0)
  - Query: `?size=0`
  - Expected: 400 Bad Request

- ❌ Invalid page size (>100)
  - Query: `?size=101`
  - Expected: 400 Bad Request, "Page size must be between 1 and 100"

### 2.3 Get Single Article
**Endpoint:** `GET /articles/{id}`

**Test Cases:**
- ✅ Get article with valid ID
  - Expected: 200 OK, returns full article details

- ❌ Get article with invalid ID (not found)
  - ID: `99999`
  - Expected: 404 Not Found

- ❌ Get article with invalid ID format (negative)
  - ID: `-1`
  - Expected: 400 Bad Request, "Article id must be positive"

- ❌ Get article with non-numeric ID
  - ID: `abc`
  - Expected: 400 Bad Request

- ⚠️ Non-admin user gets only their own articles
  - Create article as User A
  - Create article as User B
  - Expected: User A can only see their articles

### 2.4 Update Article
**Endpoint:** `PUT /articles/{id}`

**Test Cases:**
- ✅ Update article with valid data (own article)
  - Expected: 200 OK, returns updated article

- ❌ Update article without authentication
  - Expected: 401 Unauthorized

- ❌ Update article (non-admin user updating other's article)
  - Expected: 403 Forbidden

- ❌ Update article with invalid ID
  - ID: `99999`
  - Expected: 404 Not Found

- ❌ Update article with empty title
  - Expected: 400 Bad Request

- ✅ Admin can update any article
  - Create article as User A
  - Login as admin
  - Update User A's article
  - Expected: 200 OK

### 2.5 Delete Article
**Endpoint:** `DELETE /articles/{id}`

**Test Cases:**
- ✅ Delete own article (authenticated user)
  - Expected: 204 No Content

- ❌ Delete article without authentication
  - Expected: 401 Unauthorized

- ❌ Delete other user's article (non-admin)
  - Expected: 403 Forbidden

- ❌ Delete non-existent article
  - ID: `99999`
  - Expected: 404 Not Found

- ✅ Admin can delete any article
  - Create article as User A
  - Login as admin
  - Delete User A's article
  - Expected: 204 No Content

- ✅ Verify article is deleted
  - Try to GET deleted article
  - Expected: 404 Not Found

---

## 3. Security & Token Tests

### 3.1 JWT Token Lifecycle
**Flow:** Register → Login → Use Token → Refresh → Logout

- ✅ User registration auto-generates JWT
- ✅ JWT contains user info (email, role)
- ✅ JWT has expiration (10 hours)
- ✅ JWT can be used in Authorization header: `Authorization: Bearer {token}`
- ✅ Refresh token extends session (7 days)
- ✅ Old token blacklisted on logout

### 3.2 Token Validation Across Services
**Flow:** Auth-Service validates signature, Blog-Service checks JTI blacklist

- ✅ Blog-service accepts JWT from auth-service
- ✅ Blog-service validates JWT signature
- ✅ Blog-service checks JTI blacklist (checks Redis)
- ✅ Logged-out token is rejected by blog-service

### 3.3 CORS Testing
- ✅ Requests from `http://localhost:8080` are allowed
- ❌ Requests from unauthorized origins are rejected

---

## Test Script Files

1. **test_auth_register.sh** - Register function tests
2. **test_auth_login.sh** - Login and rate limiting tests
3. **test_auth_logout.sh** - Logout and token revocation tests
4. **test_auth_refresh.sh** - Refresh token tests
5. **test_auth_validation.sh** - Token validation tests
6. **test_articles_crud.sh** - CRUD operations tests
7. **test_auth_flow.sh** - Complete authentication flow
8. **test_rate_limit.sh** - Rate limiting tests

---

## Running Tests

### Prerequisites
- Docker Compose is running
- Services are accessible at `http://localhost:8080`

### Run All Tests
```bash
bash test_auth_flow.sh  # Complete flow
bash test_auth_register.sh
bash test_auth_login.sh
bash test_articles_crud.sh
```

### Run Specific Test
```bash
bash test_auth_register.sh
bash test_rate_limit.sh
```

---

## Expected Admin Account
- Email: `admin@example.com`
- Password: `password123`
- Role: `ADMIN`
