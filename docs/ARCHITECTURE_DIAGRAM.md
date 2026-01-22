# Auth-Service Architecture with Redis Cache

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        Frontend (React)                      │
│                       Port: 8080 (nginx)                     │
└────────────────────────────┬────────────────────────────────┘
                             │ HTTP/HTTPS
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                      Auth Service                           │
│                   Spring Boot 3.4.2                         │
│                      Port: 8081                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           AuthController (REST API)                 │  │
│  │                                                      │  │
│  │  • POST /auth/login      → Authenticate user       │  │
│  │  • POST /auth/register   → Create user account      │  │
│  │  • POST /auth/logout     → Revoke token & session  │  │
│  │  • POST /auth/validate   → Check token validity    │  │
│  │  • POST /auth/refresh    → Issue new JWT token     │  │
│  └──────────────────────────────────────────────────────┘  │
│                          │                                  │
│                          ▼                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │            AuthService (Business Logic)             │  │
│  │                                                      │  │
│  │  • login() - Authenticate & create session          │  │
│  │  • register() - Create user with password hash      │  │
│  │  • logout() - Revoke token & invalidate session     │  │
│  │  • validateToken() - Check JWT validity            │  │
│  │  • refreshAccessToken() - Issue new JWT            │  │
│  │  • isLoginRateLimited() - Check rate limit          │  │
│  └──────────────────────────────────────────────────────┘  │
│       │                                    │                 │
│       ▼                                    ▼                 │
│  ┌──────────────────────┐    ┌─────────────────────────┐  │
│  │ TokenCacheService    │    │ UserRepository (JPA)    │  │
│  │ (Redis operations)   │    │ (PostgreSQL)            │  │
│  │                      │    │                         │  │
│  │ • blacklistToken()   │    │ • findByEmail()         │  │
│  │ • storeRefreshToken()│    │ • existsByEmail()       │  │
│  │ • createSession()    │    │ • save()                │  │
│  │ • invalidateSession()│    │                         │  │
│  │ • incrementRateLimit│    │                         │  │
│  │ • isRateLimitExceed()│    │                         │  │
│  └──────────────────────┘    └─────────────────────────┘  │
│       │                                                      │
│       │ Spring Data Redis                                   │
│       │                                                      │
│  ┌────────────────────────────────────────────────────────┐│
│  │ RateLimitFilter (Servlet Filter)                     ││
│  │ • Intercepts POST /auth/login                        ││
│  │ • Checks Redis rate limit counter                    ││
│  │ • Returns 429 if exceeded                            ││
│  └────────────────────────────────────────────────────────┘│
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ SecurityConfig (Spring Security)                    │  │
│  │ • JWT authentication                                │  │
│  │ • BCrypt password encoding                          │  │
│  │ • Role-based access control                         │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
└──────────────────────────┬──────────────────────────────────┘
          │                │                │
          │ JDBC           │ Spring Data    │ Redis Client
          │                │ Redis          │
          ▼                ▼                ▼
    ┌──────────┐    ┌────────────────┐   ┌──────────────┐
    │PostgreSQL│    │  Auth-Cache    │   │   Network    │
    │Database  │    │  (Redis 7)     │   │              │
    │          │    │                │   │  Port 6380   │
    │ Users    │    │ Port: 6379     │   │              │
    │ Roles    │    │ (internal)     │   │  Keys:       │
    │          │    │                │   │  • blacklist │
    │          │    │ Keys:          │   │  • refresh   │
    │          │    │ • blacklist:   │   │  • session   │
    │          │    │ • refresh:     │   │  • ratelimit │
    │          │    │ • session:     │   │              │
    │          │    │ • ratelimit:   │   │              │
    │          │    │                │   │              │
    │          │    │ TTL: Auto      │   │              │
    │          │    │ Expiration     │   │              │
    │          │    │                │   │              │
    │          │    │ Memory: 256MB  │   │              │
    │          │    │ Policy: LRU    │   │              │
    │          │    │                │   │              │
    │ Port:    │    │ Health Check   │   │              │
    │ 5432     │    │ PING -> PONG   │   │              │
    │          │    │                │   │              │
    └──────────┘    └────────────────┘   └──────────────┘
```

---

## Request/Response Flow

### Login Flow
```
┌─────────┐
│ Client  │
└────┬────┘
     │ POST /auth/login
     │ {"email": "user@ex.com", "password": "..."}
     ▼
┌─────────────────────────────────────────┐
│      AuthController.login()             │
│  • Validate request                     │
│  • Check rate limit                     │
└────┬────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────┐
│      AuthService.login()                │
│  • Authenticate user (password check)   │
│  • Generate JWT token                   │
│  • Create session in Redis              │
│  • Store refresh token in Redis         │
│  • Reset rate limit counter             │
└────┬────────────────────────────────────┘
     │
     ▼ AuthResponse
┌─────────────────────────────────────────┐
│         Response (200 OK)               │
│                                         │
│ {                                       │
│   "token": "eyJhbGc...",               │
│   "username": "user",                   │
│   "email": "user@ex.com",              │
│   "role": "USER"                       │
│ }                                       │
│                                         │
│ Cookies:                               │
│ token=eyJhbGc...; HttpOnly; Path=/    │
└─────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────┐
│ Redis State After Login                 │
│                                         │
│ refresh:user → uuid-token (TTL: 7d)   │
│ session:user → email (TTL: 1h)        │
│ ratelimit:IP → 1 (TTL: 60s)          │
└─────────────────────────────────────────┘
```

### Logout Flow
```
┌─────────┐
│ Client  │
└────┬────┘
     │ POST /auth/logout
     │ Cookie: token=eyJhbGc...
     ▼
┌─────────────────────────────────────────┐
│      AuthController.logout()            │
│  • Extract token from cookie            │
│  • Get username from token              │
│  • Call authService.logout()            │
└────┬────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────┐
│      AuthService.logout()               │
│  • Blacklist token                      │
│  • Invalidate session                   │
│  • Revoke refresh token                 │
└────┬────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────┐
│      TokenCacheService operations       │
│  • blacklistToken(token)                │
│  • invalidateSession(user)              │
│  • revokeRefreshToken(user)             │
└────┬────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────┐
│       Redis Operations                  │
│                                         │
│ SET blacklist:{token} "revoked"        │
│  EXP TTL={jwt-exp-time}                │
│                                         │
│ DEL refresh:user                       │
│ DEL session:user                       │
└────┬────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────┐
│    Response (200 OK)                    │
│    (Empty body)                         │
│                                         │
│ Cookies:                               │
│ token=; HttpOnly; Path=/; Max-Age=0   │
└─────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────┐
│ Redis State After Logout                │
│                                         │
│ blacklist:eyJhbGc... → "revoked"      │
│ (TTL: 10h - auto-expires)              │
│                                         │
│ ✓ refresh:user deleted                 │
│ ✓ session:user deleted                 │
└─────────────────────────────────────────┘
```

### Token Validation Flow
```
┌─────────┐
│ Client  │
└────┬────┘
     │ POST /api/articles
     │ Authorization: Bearer eyJhbGc...
     │ Cookie: token=eyJhbGc...
     ▼
┌─────────────────────────────────────────┐
│    JwtAuthenticationFilter              │
│  • Extract JWT from header/cookie       │
│  • Validate JWT signature               │
│  • Parse claims                         │
└────┬────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────┐
│  TokenCacheService.isTokenBlacklisted() │
│  • Check Redis for blacklist key       │
│  • Redis lookup: O(1) operation        │
└────┬────────────────────────────────────┘
     │
     ├─ YES (blacklisted)
     │  └─> Return 401 Unauthorized
     │
     └─ NO (valid)
        └─> Continue to endpoint
            └─> Process request with
                principal information
```

### Rate Limiting Flow
```
┌─────────┐
│ Client  │
└────┬────┘
     │ POST /auth/login
     ▼
┌─────────────────────────────────────────┐
│      RateLimitFilter                    │
│  • Extract client IP                    │
│  • Check if login endpoint              │
└────┬────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────┐
│ TokenCacheService                       │
│ .isRateLimitExceeded(ip, maxAttempts)  │
│                                         │
│ Redis GET ratelimit:192.168.1.1        │
└────┬────────────────────────────────────┘
     │
     ├─ Count >= 5
     │  └─> Return 429 Too Many Requests
     │
     └─ Count < 5
        └─> Increment counter
            INCREMENT ratelimit:192.168.1.1
            EXPIRE 60
            └─> Continue to AuthController
```

---

## Redis Key Lifecycle

```
┌──────────────────────────────────────────────────────────┐
│              LOGIN - Redis State Created                 │
├──────────────────────────────────────────────────────────┤
│ Timestamp: 14:30:00                                      │
│                                                          │
│ blacklist:eyJhbGc...  ─┐  (Not created yet)             │
│ refresh:user          ─┼─ CREATED (stored)              │
│ session:user          ─┼─ CREATED (stored)              │
│ ratelimit:192.168.1.1 ─┼─ CREATED/INCREMENTED           │
│                        └─ All values auto-expire        │
└──────────────────────────────────────────────────────────┘

        ⏳ 1 hour passes ⏳

┌──────────────────────────────────────────────────────────┐
│       SESSION ACTIVE - Redis State Maintained            │
├──────────────────────────────────────────────────────────┤
│ Timestamp: 15:30:00                                      │
│                                                          │
│ refresh:user     ─ STILL VALID (6d 23h remaining)       │
│ session:user     ─ EXPIRES SOON (0s remaining)          │
│ ratelimit:192... ─ ALREADY EXPIRED (recreated if retry) │
│                                                          │
│ Note: Session TTL can be extended on activity          │
└──────────────────────────────────────────────────────────┘

        ⏳ User logs out ⏳

┌──────────────────────────────────────────────────────────┐
│          LOGOUT - Redis State Modified                   │
├──────────────────────────────────────────────────────────┤
│ Timestamp: 15:35:00                                      │
│                                                          │
│ blacklist:eyJhbGc...  ─ CREATED (will expire in 9h 55m) │
│ refresh:user          ─ DELETED (immediate)             │
│ session:user          ─ DELETED (immediate)             │
│ ratelimit:192.168.1.1 ─ UNCHANGED (already expired)     │
│                                                          │
│ Note: Token is revoked and can't be reused             │
└──────────────────────────────────────────────────────────┘

        ⏳ 9 hours 55 minutes pass ⏳

┌──────────────────────────────────────────────────────────┐
│        TOKEN EXPIRED - Redis State Cleaned               │
├──────────────────────────────────────────────────────────┤
│ Timestamp: 01:30:00 (next day)                           │
│                                                          │
│ blacklist:eyJhbGc...  ─ AUTO-DELETED (TTL expired)     │
│ refresh:user          ─ ALREADY DELETED                 │
│ session:user          ─ ALREADY DELETED                 │
│ ratelimit:192.168... ─ ALREADY DELETED                  │
│                                                          │
│ Result: Clean Redis state, ready for next cycle         │
└──────────────────────────────────────────────────────────┘
```

---

## Data Flow Summary

```
Authentication Flow:
  Client → Controller → Service → Database
           ↓
         Redis (Session, Refresh Token, Rate Limit)

Logout Flow:
  Client → Controller → Service → Redis
           ↓
         (Blacklist, Invalidate, Revoke)

Validation Flow:
  Request → Filter → Redis (Check Blacklist)
           ↓
         JWT Validation → Proceed or Reject

Rate Limiting:
  Request → Filter → Redis (Increment/Check)
           ↓
         429 or Continue
```

---

## Component Interactions

```
┌──────────────┐
│ Client       │ (Browser, Mobile App)
└──────┬───────┘
       │
       ├──────────────────────────┬──────────────────┐
       │                          │                  │
       ▼                          ▼                  ▼
   ┌─────────┐             ┌──────────┐        ┌────────┐
   │Login    │             │Logout    │        │Refresh │
   │Request  │             │Request   │        │Request │
   └────┬────┘             └────┬─────┘        └───┬────┘
        │                       │                  │
        ▼                       ▼                  ▼
    ┌──────────────────────────────────────────────────┐
    │              Auth Service                        │
    │  (Spring Boot 3.4.2, Java 21)                   │
    └──────────────────────────────────────────────────┘
        │                       │                  │
        ├─ AuthService ────┐    │                 │
        │  • login()       │    │                 │
        │  • register()    │    │                 │
        │  • logout()      │    │                 │
        │  • validate()    │    │                 │
        ├─ JwtUtil ───────┐    │                 │
        │  • generate     │    │                 │
        │  • validate     │    │                 │
        │  • extract      │    │                 │
        │                 │    │                 │
        ├─ TokenCache ────┼────┴────────────┬────┤
        │  • blacklist()  │                 │    │
        │  • session()    │     ┌───────────┤    │
        │  • refresh()    │     │   Redis   │    │
        │  • rateLimit()  │     │   Cache   │    │
        │                 │     │           │    │
        ├─ RateLimitFilter┼─────┤           │    │
        │  • checkLimit() │     │           │    │
        │  • increment()  │     │           │    │
        │                 │     │           │    │
        └─ UserRepository────────────┐      │    │
           • queries DB              │      │    │
                                     │      │    │
                                     ▼      ▼    ▼
                            ┌──────────────────────┐
                            │ PostgreSQL │ Redis  │
                            │ Database   │ Cache  │
                            └──────────────────────┘
```
