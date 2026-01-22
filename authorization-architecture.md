# Authorization Architecture

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend (React)                         │
│                      Port 8080 (nginx)                          │
└────────────┬────────────────────────────────────────┬───────────┘
             │                                        │
      /auth  │                                  /blog │
      routes │                                  routes│
             ▼                                        ▼
      ┌─────────────────┐                    ┌──────────────────┐
      │ Auth-Service    │                    │  Blog-Service    │
      │   Port 8081     │                    │    Port 8082     │
      │                 │                    │                  │
      │ • Login         │                    │ • Get Articles   │
      │ • Register      │                    │ • Create Article │
      │ • Logout        │                    │ • Update Article │
      │ • Validate      │                    │ • Delete Article │
      │ • Refresh Token │                    │                  │
      └────────┬────────┘                    └────────┬─────────┘
               │                                      │
               │         Redis Lookup (6380)          │
               │         "blacklist_jti:{uuid}"       │
               └──────────────────┬───────────────────┘
                                  │
                    ┌─────────────▼────────────┐
                    │  Auth-Cache (Redis)      │
                    │     Port 6380            │
                    │                          │
                    │ • JTI Blacklist          │
                    │ • Refresh Tokens         │
                    │ • Sessions               │
                    │ • Rate Limits            │
                    └──────────────────────────┘
```

---

## Token Lifecycle

```
STEP 1: LOGIN
┌─────────────────────────────────────────────────────────────┐
│ User: POST /auth/login                                      │
│ Credentials: email + password                               │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────┐
        │ Auth-Service: login()      │
        ├────────────────────────────┤
        │ • Verify credentials       │
        │ • Generate JWT with JTI    │
        │ • Create session           │
        │ • Store refresh token      │
        │ • Return token             │
        └────────────────────────────┘
                     │
         JWT Token: {
           "jti": "550e8400-e29b...",  ← UNIQUE ID
           "sub": "user@example.com",
           "role": "USER",
           "iat": 1705821600,
           "exp": 1705857600
         }
                     │
                     ▼
        └────────────────────────────┐
        │ Client stores token        │
        │ (localStorage/cookie)      │
        └────────────────────────────┘

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

STEP 2: USING TOKEN
┌─────────────────────────────────────────────────────────────┐
│ User: GET /blog/articles                                    │
│ Header: Authorization: Bearer {token}                       │
└────────────────────┬────────────────────────────────────────┘
                     │
            ┌────────┴────────┐
            │                 │
            ▼                 ▼
     ┌─────────────┐  ┌──────────────┐
     │Auth-Service │  │Blog-Service  │
     └─────────────┘  └──────────────┘
     (if called)      (main path)
            │                 │
            │                 ▼
            │         TokenValidationFilter
            │         ├─ Extract JTI from token
            │         ├─ Query Redis: blacklist_jti:{jti}?
            │         └─ Continue (not blacklisted)
            │                 │
            │                 ▼
            │         JwtAuthFilter
            │         ├─ Verify signature
            │         └─ Check expiration
            │                 │
            │                 ▼
            │         Controller → Return data
            │                 │
            └─────────────────┘

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

STEP 3: LOGOUT
┌─────────────────────────────────────────────────────────────┐
│ User: POST /auth/logout                                     │
│ Cookie: token={same token from login}                       │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────┐
        │ Auth-Service: logout()     │
        ├────────────────────────────┤
        │ 1. Extract JTI from token  │
        │ 2. Redis: SET              │
        │    "blacklist_jti:{jti}"   │
        │    value="revoked"         │
        │    TTL=86400 seconds       │
        │ 3. Invalidate session      │
        │ 4. Revoke refresh token    │
        │ 5. Clear cookie            │
        └────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────┐
        │ Redis Auth-Cache:          │
        │ blacklist_jti:{jti}="revoked"
        │ TTL: 86400 (auto-delete)   │
        └────────────────────────────┘
                     │
                     ▼
        Return 200 OK to client

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

STEP 4: USING LOGGED-OUT TOKEN (BLOCKED!)
┌─────────────────────────────────────────────────────────────┐
│ User: GET /blog/articles                                    │
│ Header: Authorization: Bearer {SAME LOGGED-OUT TOKEN}       │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────┐
        │ Blog-Service               │
        ├────────────────────────────┤
        │ TokenValidationFilter      │
        │ ├─ Extract JTI             │
        │ ├─ Query Redis:            │
        │ │  blacklist_jti:{jti}?    │
        │ └─ FOUND! ❌               │
        └────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────┐
        │ Return 401 UNAUTHORIZED    │
        │ "Token has been revoked"   │
        └────────────────────────────┘
```

---

## Refresh Token Lifecycle

```
STEP 1: OBTAIN REFRESH TOKEN (During Login)
┌─────────────────────────────────────────────────────────────┐
│ User: POST /auth/login                                      │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │ Auth-Service: login()          │
        ├────────────────────────────────┤
        │ • Generate access token (JWT)  │
        │ • Generate refresh token (JWT) │
        │ • Store refresh token in Redis │
        └────────────────────────────────┘
                     │
        Response includes:
        {
          "token": "eyJhbGci...",     ← Access token (10 hours)
          "refreshToken": "eyJhbGci...",  ← Refresh token (7 days)
          "email": "user@example.com"
        }
                     │
                     ▼
        ┌────────────────────────────────┐
        │ Redis Auth-Cache:              │
        │ refresh:user@example.com       │
        │ = {refresh_token_jwt}          │
        │ TTL: 604800 seconds (7 days)   │
        └────────────────────────────────┘
                     │
                     ▼
        Client stores both tokens
        (localStorage/secure storage)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

STEP 2: ACCESS TOKEN EXPIRES
┌─────────────────────────────────────────────────────────────┐
│ Time passes: 10 hours later                                 │
│ User: GET /blog/articles                                    │
│ Header: Authorization: Bearer {EXPIRED access token}        │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────┐
        │ Blog-Service               │
        ├────────────────────────────┤
        │ JwtAuthFilter              │
        │ ├─ Check expiration        │
        │ └─ ❌ Token EXPIRED        │
        └────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────┐
        │ Return 401 UNAUTHORIZED    │
        │ "Token expired"            │
        └────────────────────────────┘
                     │
                     ▼
        Client detects 401 error
        → Tries to refresh token

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

STEP 3: REFRESH ACCESS TOKEN
┌─────────────────────────────────────────────────────────────┐
│ User: POST /auth/refresh                                    │
│ Body: {"refreshToken": "eyJhbGci..."}                       │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │ Auth-Service: refreshToken()   │
        ├────────────────────────────────┤
        │ 1. Validate refresh token      │
        │    ├─ Check signature          │
        │    ├─ Check expiration         │
        │    └─ Check if NOT blacklisted │
        │ 2. Retrieve from Redis         │
        │    GET refresh:user@example... │
        │ 3. If valid, generate NEW      │
        │    access token (JWT)          │
        │ 4. Return new access token     │
        └────────────────────────────────┘
                     │
        New Token: {
          "jti": "660e9501-f30c-51e5-...",  ← NEW JTI!
          "sub": "user@example.com",
          "role": "USER",
          "iat": 1705908000,                 ← NEW timestamp
          "exp": 1705944000                  ← 10 more hours
        }
                     │
                     ▼
        ┌────────────────────────────────┐
        │ Return 200 OK                  │
        │ {                              │
        │   "token": "{new access token}"│
        │ }                              │
        └────────────────────────────────┘
                     │
                     ▼
        Client stores new access token
        → Can now access /blog/articles

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

STEP 4: REFRESH TOKEN EXPIRES (After 7 days)
┌─────────────────────────────────────────────────────────────┐
│ Time passes: 7 days later                                   │
│ User: POST /auth/refresh                                    │
│ Body: {"refreshToken": "eyJhbGci..."} ← EXPIRED             │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │ Auth-Service: refreshToken()   │
        ├────────────────────────────────┤
        │ • Check expiration             │
        │ └─ ❌ Refresh token EXPIRED    │
        └────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │ Return 401 UNAUTHORIZED        │
        │ "Refresh token expired"        │
        └────────────────────────────────┘
                     │
                     ▼
        Client must re-login
        POST /auth/login with credentials

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

STEP 5: LOGOUT (Revokes Refresh Token)
┌─────────────────────────────────────────────────────────────┐
│ User: POST /auth/logout                                     │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │ Auth-Service: logout()         │
        ├────────────────────────────────┤
        │ 1. Blacklist access token JTI  │
        │    SET blacklist_jti:{jti}     │
        │ 2. DELETE refresh token        │
        │    DEL refresh:user@example... │
        │ 3. Invalidate session          │
        │    DEL session:user@example... │
        └────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │ Redis Auth-Cache:              │
        │ • blacklist_jti = "revoked"    │
        │ • refresh token deleted        │
        │ • session deleted              │
        └────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │ Return 200 OK                  │
        │ User logged out                │
        └────────────────────────────────┘
                     │
                     ▼
        Client clears tokens
        (localStorage)

        Future requests with:
        • Old access token → 401 (blacklisted JTI)
        • Old refresh token → 401 (deleted from Redis)
        → Must re-login
```


---

## Data Model

### Redis Data Structures

```
┌─────────────────────────────────────────────────────┐
│              Auth-Cache (Redis)                     │
├─────────────────────────────────────────────────────┤
│                                                     │
│  JTI BLACKLIST (Token Revocation)                   │
│  ─────────────────────────────────────              │
│  Key: blacklist_jti:550e8400-e29b-41d4...         │
│  Value: "revoked"                                 │
│  TTL: 86400 seconds (matches JWT expiration)      │
│  Size: ~36 bytes (UUID only, not full token!)     │
│  Entries: One per logged-out token                │
│                                                     │
│  EXAMPLE:                                          │
│  ┌─────────────────────────────────────────────┐  │
│  │ Key: blacklist_jti:550e8400-e29b-41d4...   │  │
│  │ Value: "revoked"                            │  │
│  │ TTL: 86400 → 86399 → 86398 → ... → 0       │  │
│  │ (auto-delete when TTL reaches 0)            │  │
│  └─────────────────────────────────────────────┘  │
│                                                     │
│  REFRESH TOKENS                                    │
│  ─────────────────────────────────────             │
│  Key: refresh:user@example.com                    │
│  Value: {refresh_token_jwt}                       │
│  TTL: 604800 seconds (7 days)                     │
│  Size: ~200-300 bytes                             │
│                                                     │
│  SESSIONS                                          │
│  ─────────────────────────────────────             │
│  Key: session:user@example.com                    │
│  Value: {"email": "...", "loginTime": ...}        │
│  TTL: 3600 seconds (1 hour)                       │
│  Size: ~100-200 bytes                             │
│                                                     │
│  RATE LIMITS                                       │
│  ─────────────────────────────────────             │
│  Key: ratelimit:user@example.com                  │
│  Value: 5 (attempt count)                         │
│  TTL: 60 seconds (reset window)                   │
│  Size: ~10 bytes                                   │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## Filter Chain Order (Blog-Service)

```
HTTP Request
    │
    ├─→ CORS/Security Headers
    │
    ├─→ TokenValidationFilter (NEW! ✅)
    │   ├─ Extract Authorization header
    │   ├─ Parse JWT payload for JTI
    │   ├─ Redis query: Is JTI blacklisted?
    │   │  ├─ YES → Return 401 "Token revoked"
    │   │  └─ NO → Continue
    │   └─ (Gracefully handle Redis errors)
    │
    ├─→ JwtAuthFilter
    │   ├─ Parse JWT signature
    │   ├─ Verify using secret
    │   ├─ Check expiration date
    │   └─ Set SecurityContext
    │
    ├─→ @PreAuthorize / @Secured checks
    │
    └─→ Controller
        ├─ Business logic
        └─ Response
```

---

## Deployment Topology

```
┌────────────────────────────────────────────────────────────┐
│                    Docker Network                          │
│                   (app-network)                            │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ┌─────────────┐    ┌──────────────┐    ┌──────────────┐   │
│  │   Database  │    │ Auth-Service │    │Blog-Service  │   │
│  │  PostgreSQL │    │   (Java)     │    │   (Java)     │   │
│  │  Port 5432  │    │  Port 8081   │    │  Port 8082   │   │
│  │             │    │              │    │              │   │
│  │ • Users     │    │ • Login      │    │ • Articles   │   │
│  │ • Articles  │    │ • Logout     │    │ • Comments   │   │
│  │ • Comments  │    │ • Validate   │    │ • Likes      │   │
│  └─────────────┘    └───────┬──────┘    └────────┬─────┘   │
│                             │                    │         │
│        ┌────────────────────┴────────────────────┘         │
│        │   Shared Redis Connection                         │
│        ▼                                                   │
│  ┌──────────────────────────────────┐                      │
│  │    Auth-Cache (Redis)            │                      │
│  │   Port 6380 (external)           │                      │
│  │   Port 6379 (internal docker)    │                      │
│  │                                  │                      │
│  │  • JTI Blacklist                 │                      │
│  │  • Refresh Tokens                │                      │
│  │  • Sessions                      │                      │
│  │  • Rate Limits                   │                      │
│  │                                  │                      │
│  │  Persistent Storage:             │                      │
│  │  • 256MB max memory              │                      │
│  │  • LRU eviction policy           │                      │
│  │  • No disk persistence           │                      │
│  └──────────────────────────────────┘                      │
│                                                            │
│  ┌──────────────────────────────────┐                      │
│  │    Blog-Cache (Redis)            │                      │
│  │   Port 6379 (external)           │                      │
│  │   Port 6379 (internal docker)    │                      │
│  │                                  │                      │
│  │  Local caching for:              │                      │
│  │  • Article fragments             │                      │
│  │  • Comment counts                │                      │
│  │  • Category listings             │                      │
│  │                                  │                      │
│  │  Storage: 128MB max              │                      │
│  └──────────────────────────────────┘                      │
│                                                            │
│  ┌──────────────────────────────────┐                      │
│  │  Frontend (nginx)                │                      │
│  │  Port 8080 (external)            │                      │
│  │  Port 80 (internal docker)       │                      │
│  │                                  │                      │
│  │  Routes:                         │                      │
│  │  /api/auth/* → auth-service      │                      │
│  │  /api/blog/* → blog-service      │                      │
│  │  / → React SPA                   │                      │
│  └──────────────────────────────────┘                      │
│                                                            │
└────────────────────────────────────────────────────────────┘

Note: All services communicate via Docker DNS resolution:
      - auth-service: http://auth-service:8081
      - blog-service: http://blog-service:8082
      - auth-cache: redis://auth-cache:6379
```

---

## Security Model

```
┌─────────────────────────────────────────┐
│       Token Security Layers             │
├─────────────────────────────────────────┤
│                                         │
│  Layer 1: Cryptographic Signature       │
│  ─────────────────────────────────────  │
│  • HMAC-SHA256 signature                │
│  • Secret key: 256-bit (base64 encoded) │
│  • Prevents token tampering             │
│  • Verified by: JwtAuthFilter           │
│                                         │
│  Layer 2: Expiration Time               │
│  ─────────────────────────────────────  │
│  • iat (Issued At): 1705821600          │
│  • exp (Expiration): 1705857600         │
│  • Validity: ~10 hours                  │
│  • Prevents old tokens from being used  │
│  • Verified by: JwtAuthFilter           │
│                                         │
│  Layer 3: JTI (Token ID) Blacklist      │
│  ─────────────────────────────────────  │
│  • JTI: UUID unique to this token       │
│  • Stored in Redis: blacklist_jti:jti   │
│  • Set on logout                        │
│  • Prevents logged-out tokens           │
│  • Verified by: TokenValidationFilter   │
│                                         │
│  Layer 4: Service-Level Validation      │
│  ─────────────────────────────────────  │
│  • Each service independently checks    │
│  • No single point of failure           │
│  • Graceful degradation if Redis down   │
│                                         │
└─────────────────────────────────────────┘

Multi-layer approach = Defense in Depth
```

