# Spring Security Architecture & JWT Implementation

## Overview

Spring Security is a powerful authentication and authorization framework for Java applications. In this auth-service, it's used to secure login/registration endpoints and validate JWT tokens.

---

## Spring Security Flow (High Level)

```
┌─────────────────────────────────────────────────────────────┐
│                     HTTP Request                            │
│              (e.g., POST /auth/login)                       │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  SecurityFilterChain           │
        │  (Spring Security's Gatekeeper)│
        └────────────────────────────────┘
                     │
        ┌────────────┴───────────────┐
        │                            │
        ▼                            ▼
   ┌─────────────┐          ┌──────────────┐
   │  Filters    │          │ Controllers  │
   │  (many!)    │          │              │
   └──────┬──────┘          └──────────────┘
          │
          ├─→ CorsFilter (Allow cross-origin?)
          │
          ├─→ RateLimitFilter (Too many attempts?)
          │
          ├─→ JwtAuthFilter (JWT valid?)
          │
          ├─→ AuthenticationFilter (Need login?)
          │
          └─→ ... more filters
```

---

## Spring Security Components

### 1. SecurityFilterChain

The central component that applies all security filters to every HTTP request. It's like a security checkpoint that every request must pass through.

```
User Request
    │
    ├─ Filter 1 (CORS)
    │  │
    │  └─ PASS? ──→ Continue
    │
    ├─ Filter 2 (Rate Limit)
    │  │
    │  └─ PASS? ──→ Continue
    │
    ├─ Filter 3 (JWT Auth)
    │  │
    │  └─ PASS? ──→ Continue
    │
    └─ [Request reaches controller if all filters pass]
```

### 2. AuthenticationManager

The "orchestrator" of authentication. It coordinates the authentication process by delegating to AuthenticationProviders.

```
                   AuthenticationManager
                           │
                           │ (coordinates)
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
   ┌──────────┐      ┌──────────┐      ┌──────────┐
   │Provider 1│      │Provider 2│      │Provider N│
   │(Username)│      │(JWT)     │      │(OAuth)   │
   └──────────┘      └──────────┘      └──────────┘
        │                  │                  │
        └──────────────────┼──────────────────┘
                           │
                    [One provider matches!]
                           │
                           ▼
                  Authentication Token
```

### 3. AuthenticationProvider

Implements the actual authentication logic for a specific authentication method.

In auth-service, we have:
- **DaoAuthenticationProvider** (Spring default): Authenticates via username/password lookup in database
- **JwtAuthenticationProvider** (custom, if needed): Validates JWT tokens

```
AuthenticationProvider
    │
    ├─ Receive: AuthenticationRequest
    │  (contains credentials)
    │
    ├─ Authenticate:
    │  ├─ Load user from database/cache
    │  ├─ Check if user exists
    │  ├─ Validate password (BCrypt comparison)
    │  └─ Generate authorities (roles/permissions)
    │
    └─ Return: Authentication Token
       (user info + authorities + credentials)
```

### 4. Filter

A Spring component that intercepts HTTP requests before they reach your controller. Filters run in a defined order.

```
HTTP Request
    │
    ▼
┌──────────────────┐
│  Filter 1        │ ← Runs first
│  ├─ Check CORS   │
│  └─ Pass? Continue
└──────────────────┘
    │
    ▼
┌──────────────────┐
│  Filter 2        │ ← Runs second
│  ├─ Check JWT    │
│  └─ Pass? Continue
└──────────────────┘
    │
    ▼
┌──────────────────┐
│  Controller      │ ← Only if all filters passed
│  POST /auth/login
└──────────────────┘
```

---

## Auth-Service Authentication Flow (Login)

```
USER LOGIN REQUEST
┌─────────────────────────────────────────────────────────────┐
│ Client: POST /auth/login                                    │
│ Body: {"email": "user@example.com", "password": "secret"}   │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  SecurityFilterChain           │
        │  (Runs all filters first)      │
        └────────────┬───────────────────┘
                     │
                     ├─→ CorsFilter        PASS
                     │
                     ├─→ RateLimitFilter   PASS (not too many attempts)
                     │
                     └─→ AuthenticationFilter (public endpoint, SKIP)
                     │
                     ▼
        ┌────────────────────────────────┐
        │  AuthController.login()        │
        │  (Request reaches controller)  │
        └────────────┬───────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  AuthService.login()           │
        │  1. Validate input format      │
        │  2. Create UsernamePassword    │
        │     AuthenticationRequest      │
        └────────────┬───────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  AuthenticationManager         │
        │  .authenticate()               │
        └────────────┬───────────────────┘
                     │
       ┌─────────────┴─────────────┐
       │                           │
       ▼                           ▼
    [Try Provider 1]        [Try Provider 2]
    DaoAuthenticationProvider  ...
       │
       ├─→ UserRepository.findByEmail()
       │
       ├─→ User found? YES
       │
       ├─→ BCrypt.matches(inputPassword, hashedPassword)
       │   YES (passwords match)
       │
       ├─→ Load authorities (ROLE_USER, ROLE_ADMIN, etc.)
       │
       └─→ Return: Authentication { user, authorities }
              (marked as "authenticated")
                     │
                     ▼
        ┌────────────────────────────────┐
        │  AuthService.login() continues │
        │                                │
        │  1. Extract user info          │
        │  2. Generate JWT with:         │
        │     • sub (subject/email)      │
        │     • iat (issued at)          │
        │     • exp (expiration)         │
        │     • jti (unique ID)          │
        │     • roles (authorities)      │
        │  3. Generate refresh token     │
        │  4. Store refresh in Redis     │
        │  5. Create session in Redis    │
        └────────────┬───────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  Return 200 OK                 │
        │  {                             │
        │    "token": "eyJhbGci...",     │
        │    "refreshToken": "...",      │
        │    "expiresIn": 36000,         │
        │    "email": "user@example..."  │
        │  }                             │
        └────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  Client stores JWT             │
        │  (localStorage/cookie)         │
        └────────────────────────────────┘
```

---

## JWT Token Structure & Usage

### JWT Composition

A JWT consists of 3 parts separated by dots (`.`):

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNzA1ODIxNjAwLCJleHAiOjE3MDU4NTc2MDAsImp0aSI6IjU1MGU4NDAwIn0.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c

│                         │                                              │                              │
└────────── ────────────┘ └────────────────── ────────────────────────┘ └──────────── ────────────────┘
   HEADER                                   PAYLOAD                                 SIGNATURE
   (Algorithm)                        (Claims/User Data)            (HMAC-SHA256 using secret)
```

### JWT Payload (Claims)

```json
{
  "sub": "user@example.com",        // Subject (who the token is for)
  "email": "user@example.com",      // Email claim
  "iat": 1705821600,                // Issued At (when token was created)
  "exp": 1705857600,                // Expiration (when token expires)
  "jti": "550e8400-e29b-41d4-...",  // JWT ID (unique identifier for revocation)
  "roles": ["ROLE_USER"],           // User authorities/permissions
  "iss": "auth-service",            // Issuer
  "aud": "blog-service"             // Audience
}
```

### JwtUtil Class (JWT Generation & Extraction)

In `auth-service/src/main/java/com/example/auth/security/JwtUtil.java`:

```
JwtUtil Methods:

1. generateToken(User user)
   ├─ Create UUID for JTI
   ├─ Build JWT with claims:
   │  ├─ sub = user.getEmail()
   │  ├─ iat = System.currentTimeMillis()
   │  ├─ exp = iat + 36000000 (10 hours)
   │  ├─ jti = UUID
   │  └─ roles = user.getAuthorities()
   ├─ Sign with HMAC-SHA256 + secret key
   └─ Return JWT string

2. validateToken(String token)
   ├─ Parse token (verify signature)
   ├─ Check expiration
   ├─ Return true if valid, false otherwise

3. extractEmail(String token)
   ├─ Parse token
   ├─ Extract "sub" claim
   └─ Return email

4. extractJti(String token)
   ├─ Parse token
   ├─ Extract "jti" claim
   └─ Return unique token ID

5. extractExpiration(String token)
   ├─ Parse token
   ├─ Extract "exp" claim
   └─ Return expiration time
```

---

## JWT Validation Flow (Token Usage)

When a client sends a request with JWT:

```
USER API REQUEST
┌─────────────────────────────────────────────────────────────┐
│ Client: GET /blog/articles                                  │
│ Header: Authorization: Bearer eyJhbGci...                   │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │  SecurityFilterChain           │
        └────────────┬───────────────────┘
                     │
                     ├─→ CorsFilter           PASS
                     │
                     ├─→ RateLimitFilter      PASS
                     │
                     └─→ JwtAuthFilter        ← VALIDATE JWT
                        │
                        ├─ Extract Authorization header
                        │  "Bearer eyJhbGci..."
                        │
                        ├─ Remove "Bearer " prefix
                        │
                        ├─ Parse JWT token
                        │
                        ├─ Verify signature using secret key
                        │  Yes, Signature valid? Continue
                        │  No, Signature invalid? Return 401
                        │
                        ├─ Check expiration time
                        │  Yes, Not expired? Continue
                        │  No, Expired? Return 401
                        │
                        ├─ Extract email from "sub" claim
                        │
                        ├─ Create Authentication object
                        │  (email + authorities from token)
                        │
                        └─ Set in SecurityContext
                           (current user available to controller)
                     │
                     ▼
        ┌────────────────────────────────┐
        │  Controller/Service            │
        │  @GetMapping("/articles")      │
        │                                │
        │  SecurityContext.getAuth...()  │
        │  → Returns authenticated user  │
        └────────────────────────────────┘
```

---

## Filter Chain Order in Auth-Service

```
HTTP Request comes in
│
├─ [1] CorsFilter
│      ├─ Check origin header
│      └─ Add CORS headers if allowed
│
├─ [2] RateLimitFilter
│      ├─ Check IP address (or user)
│      ├─ Check attempt count in Redis
│      └─ Block if > 5 attempts in 60 seconds
│
├─ [3] SecurityFilterChain's internal filters
│      ├─ AuthenticationFilter (for /auth/login, /auth/register)
│      │  └─ Intercepts login/register to create Authentication
│      │
│      └─ JwtAuthFilter (for protected endpoints)
│         ├─ Extract JWT from header
│         ├─ Validate JWT signature & expiration
│         └─ Set Authentication in SecurityContext
│
└─ [Request reaches your @RestController]
   ├─ @PostMapping("/login") → public (no auth needed)
   ├─ @PostMapping("/register") → public (no auth needed)
   ├─ @PostMapping("/logout") → protected (needs valid JWT)
   └─ @PostMapping("/refresh") → protected (needs valid JWT)
```

---

## AuthenticationManager & AuthenticationProvider Details

### DaoAuthenticationProvider (Default for Username/Password)

```
DaoAuthenticationProvider
│
├─ Input: UsernamePasswordAuthenticationToken
│  {
│    username: "user@example.com",
│    password: "plaintext_password"
│  }
│
├─ Step 1: Load user from database
│  ├─ Call UserRepository.findByEmail(username)
│  └─ User found? Yes, continue; No, return error
│
├─ Step 2: Validate password
│  ├─ Get stored hashedPassword from DB
│  ├─ Use BCryptPasswordEncoder.matches()
│  │  └─ Compare plaintext with hash
│  └─ Passwords match? Yes, continue; No, return error
│
├─ Step 3: Check account status
│  ├─ isEnabled() → true?
│  ├─ isAccountNonLocked() → true?
│  ├─ isAccountNonExpired() → true?
│  ├─ isCredentialsNonExpired() → true?
│  └─ All true? Yes, continue; No, return error
│
├─ Step 4: Load authorities
│  ├─ Get user.getAuthorities()
│  └─ Typically: [ROLE_USER, ROLE_ADMIN]
│
└─ Output: UsernamePasswordAuthenticationToken
   {
     principal: User object,
     credentials: password (may be cleared),
     authorities: [ROLE_USER, ROLE_ADMIN],
     authenticated: true
   }
```

### How It's Used in AuthService.login()

```java
// In AuthService.login(LoginRequest request)

// Step 1: Create authentication request token (not yet authenticated)
UsernamePasswordAuthenticationToken authRequest = 
    new UsernamePasswordAuthenticationToken(
        request.getEmail(),      // username
        request.getPassword()    // password (plaintext)
    );
// At this point: authenticated = false

// Step 2: Pass to AuthenticationManager
Authentication authentication = 
    authenticationManager.authenticate(authRequest);
// AuthenticationManager finds the right provider (DaoAuthenticationProvider)
// Provider validates email/password
// At this point: authenticated = true (if credentials valid)

// Step 3: Extract authenticated user
User user = (User) authentication.getPrincipal();

// Step 4: Generate JWT with user info
String token = jwtUtil.generateToken(user);

// Step 5: Return response
return new AuthResponse(token, ...);
```

---

## Security Config (SecurityFilterChain Configuration)

The `SecurityConfig.java` file configures:

```
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    // [1] Define which endpoints are public vs protected
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            // Public endpoints (no auth required)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/auth/login", "/auth/register").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Protected endpoints (auth required)
                .requestMatchers("/auth/logout", "/auth/refresh").authenticated()
                .requestMatchers("/auth/validate").authenticated()
                // Any other request needs authentication
                .anyRequest().authenticated()
            )
            
            // [2] Add custom filters
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            
            // [3] Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // [4] Disable CSRF (stateless API, so CSRF not needed)
            .csrf(csrf -> csrf.disable())
            
            // [5] Configure exception handling
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
            );
        
        return http.build();
    }
    
    // [6] Define AuthenticationManager
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    // [7] Configure password encoder (BCrypt)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## Data Flow: Complete User Session

```
┌─────────────────────────────────────────────────────────────┐
│ 1. USER REGISTRATION                                        │
├─────────────────────────────────────────────────────────────┤
│ POST /auth/register                                         │
│ Body: {email, password}                                     │
│ ↓                                                           │
│ AuthService.register()                                      │
│ ├─ Validate email format                                    │
│ ├─ Check if email already exists                            │
│ ├─ Hash password using BCryptPasswordEncoder                │
│ ├─ Create new User entity                                   │
│ ├─ UserRepository.save(user) → Save to DB                   │
│ └─ Return 201 Created                                       │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 2. USER LOGIN                                               │
├─────────────────────────────────────────────────────────────┤
│ POST /auth/login                                            │
│ Body: {email, password}                                     │
│ ↓                                                           │
│ SecurityFilterChain                                         │
│ ├─ RateLimitFilter: Check attempt count                     │
│ └─ Pass through (not protected endpoint)                    │
│ ↓                                                           │
│ AuthService.login()                                         │
│ ├─ Create UsernamePasswordAuthenticationToken               │
│ ├─ AuthenticationManager.authenticate()                     │
│ │  └─ DaoAuthenticationProvider validates                   │
│ │     ├─ Load user from DB by email                        │
│ │     ├─ BCrypt compare plaintext vs hashed password       │
│ │     └─ Return authenticated token                        │
│ ├─ JwtUtil.generateToken(user)                              │
│ │  └─ Create JWT with JTI, exp, roles                      │
│ ├─ Generate refresh token                                  │
│ ├─ TokenCacheService.saveRefreshToken()                     │
│ │  └─ Redis: SET refresh:email = refreshToken              │
│ ├─ Create session in Redis                                 │
│ └─ Return {token, refreshToken, expiresIn}                 │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 3. USER API REQUEST (Using JWT)                             │
├─────────────────────────────────────────────────────────────┤
│ GET /blog/articles                                          │
│ Header: Authorization: Bearer eyJhbGci...                   │
│ ↓                                                           │
│ SecurityFilterChain                                         │
│ ├─ JwtAuthFilter: Extract & validate JWT                    │
│ │  ├─ Get Authorization header                             │
│ │  ├─ JwtUtil.validateToken(token)                          │
│ │  │  ├─ Verify HMAC-SHA256 signature                       │
│ │  │  └─ Check expiration time                              │
│ │  ├─ JwtUtil.extractEmail(token)                           │
│ │  ├─ Create Authentication object                         │
│ │  └─ Set in SecurityContext                               │
│ └─ Pass to controller                                       │
│ ↓                                                           │
│ Controller can now access:                                  │
│ ├─ SecurityContext.getAuthentication()                      │
│ ├─ user = authentication.getPrincipal()                     │
│ └─ authorities = authentication.getAuthorities()            │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 4. TOKEN REFRESH (When Access Token Expires)                │
├─────────────────────────────────────────────────────────────┤
│ POST /auth/refresh                                          │
│ Body: {refreshToken: "eyJhbGci..."}                         │
│ ↓                                                           │
│ AuthService.refreshToken()                                  │
│ ├─ JwtUtil.validateToken(refreshToken)                      │
│ │  ├─ Verify signature                                      │
│ │  └─ Check expiration (7 days)                             │
│ ├─ JwtUtil.extractEmail(refreshToken)                       │
│ ├─ TokenCacheService.getRefreshToken(email)                 │
│ │  └─ Redis: GET refresh:email                             │
│ │     └─ Compare stored token with provided token          │
│ ├─ Generate NEW access token                               │
│ │  └─ New JTI, new exp time, same email                    │
│ └─ Return {token: newAccessToken}                           │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 5. USER LOGOUT                                              │
├─────────────────────────────────────────────────────────────┤
│ POST /auth/logout                                           │
│ Header: Authorization: Bearer eyJhbGci... (access token)    │
│ ↓                                                           │
│ AuthService.logout()                                        │
│ ├─ Extract access token JTI                                │
│ ├─ TokenCacheService.blacklistTokenByJti(jti)               │
│ │  └─ Redis: SET blacklist_jti:jti = "revoked"             │
│ │     TTL: 86400 (match token exp time)                     │
│ ├─ TokenCacheService.deleteRefreshToken(email)              │
│ │  └─ Redis: DEL refresh:email                             │
│ ├─ Delete session from Redis                               │
│ └─ Return 200 OK                                            │
└─────────────────────────────────────────────────────────────┘
```

---

## Key Security Concepts

### 1. Password Storage (BCrypt)

```
User enters: "password123"
                │
                ▼
        BCryptPasswordEncoder
                │
                ├─ Generate random salt
                ├─ Hash password using salt
                └─ Hash = "$2a$10$slEQmDKJ..."
                │
                ▼
        Store in Database
        users.password = "$2a$10$slEQmDKJ..."
                │
        (Later, during login)
                │
                ▼
        User enters: "password123" again
                │
                ▼
        BCrypt.matches("password123", "$2a$10$...")
                │
                ├─ Use stored salt to hash input
                ├─ Compare with stored hash
                └─ Match? YES → Login success
```

### 2. JWT Signature Verification

```
Token Creation:
  Header + Payload + Secret Key
         │
         ▼
  HMAC-SHA256(Header.Payload, secret)
         │
         ▼
  Signature (Base64 encoded)
         │
         ▼
  Final JWT: Header.Payload.Signature

Token Validation:
  Received JWT: Header.Payload.Signature
         │
         ├─ Extract Header, Payload, Signature
         │
         ├─ Recalculate: HMAC-SHA256(Header.Payload, secret)
         │
         ├─ Compare new signature with received signature
         │
         └─ Match? Yes, Token is authentic (not tampered)
            No, Token was modified or forged
```

### 3. JTI-Based Token Revocation

```
During Logout:
  1. Extract JTI from token
  2. Store in Redis: blacklist_jti:550e8400... = "revoked"
  3. Set TTL = token expiration time

On Next Request with Same Token:
  1. Extract JTI
  2. Check Redis: EXISTS blacklist_jti:550e8400?
  3. YES? → Return 401 (token revoked)
  4. NO? → Continue (token still valid)

Why JTI instead of full token?
  • JTI = UUID (36 bytes)
  • Full token = 500+ bytes
  • Redis storage: 36 bytes vs 500+ bytes
  • Saves 93% memory!
```

---

## Summary: How It All Works Together

```
1. USER REGISTERS
   └─ Password hashed with BCrypt
   └─ Stored in database

2. USER LOGS IN
   └─ AuthenticationManager + DaoAuthenticationProvider
   └─ Validate email/password using BCrypt
   └─ JwtUtil generates JWT with JTI claim
   └─ RefreshToken stored in Redis

3. USER MAKES API REQUEST
   └─ SecurityFilterChain intercepts
   └─ JwtAuthFilter validates JWT signature & expiration
   └─ JTI blacklist checked (on blog-service)
   └─ Request passed to controller if all checks pass

4. USER LOGS OUT
   └─ JTI blacklisted in Redis
   └─ Refresh token deleted
   └─ Future requests with same JWT = 401

5. ACCESS TOKEN EXPIRES
   └─ Client sends refresh token to /auth/refresh
   └─ Auth-service validates refresh token
   └─ Issues new access token (with new JTI)
   └─ Client continues using API
```

This multi-layered approach ensures:
- Passwords are never stored plaintext (BCrypt)
- Tokens are cryptographically signed (can't be forged)
- Tokens can be revoked even before expiration (JTI blacklist)
- Sessions can be extended without re-authentication (refresh tokens)
- Distributed validation across services (blog-service also checks JTI)
