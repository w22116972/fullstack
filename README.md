
# Blog Admin System

A full-stack blog administration system with microservices architecture.

## Project Structure

```
fullstack/
├── auth-service/          # Authentication microservice
│   └── src/main/java/com/example/auth/
│       ├── config/        # Security, rate limiting
│       ├── controller/    # REST endpoints
│       ├── model/         # JPA entities
│       ├── repository/    # Data access
│       └── service/       # Business logic
├── blog-service/          # Blog microservice
│   └── src/main/java/com/example/blog/
│       ├── config/        # Cache, security
│       ├── controller/    # REST endpoints
│       ├── model/         # JPA entities
│       ├── repository/    # Data access
│       └── service/       # Business logic
├── frontend/              # React application
│   ├── src/
│   │   ├── components/    # Reusable components
│   │   ├── context/       # React context (auth state)
│   │   ├── pages/         # Route components
│   │   └── services/      # API client
│   └── nginx.conf         # Nginx configuration
├── database/               # database Docker config
├── auth-cache/            # Redis Docker config
├── blog-cache/            # Redis Docker config
├── scripts/               # Testing scripts
└── docker-compose.yml     # Service orchestration
```

## Technology Stack

### Backend
- Java 21
- Spring Boot 3.4.2
- Spring Security (JWT authentication)
- Spring Data JPA (Hibernate)
- PostgreSQL 16
- Redis (article caching)

### Frontend
- React 19
- TypeScript
- React Router 7
- React Hook Form
- Axios

### Infrastructure
- Docker and Docker Compose
- Nginx (reverse proxy and static file serving)

## Architecture

The system consists of the following services:

| Service | Port | Description |
|---------|------|-------------|
| frontend | 8080 | React SPA served by Nginx, proxies API calls |
| auth-service | 8081 | Authentication, user management, token validation |
| blog-service | 8082 | Article CRUD operations with token JTI validation |
| database | 5432 | Shared PostgreSQL database (appdb) |
| auth-cache | 6380 | Redis for JTI blacklist, refresh tokens, sessions, rate limits (shared) |
| blog-cache | 6379 | Redis cache for article data (local to blog-service) |

## Quick Start

### Run with Docker Compose

```bash
docker-compose up --build
```

- Application at http://localhost:8080
- Swagger UI at: http://localhost:8082/swagger-ui.html

### Default Test Account

After first startup, the system automatically creates an admin account:

- **Email**: `admin@example.com`
- **Password**: `password123`

## Challenges and decisions

- CORS Handling: add http://localhost:3000 to allowed origins
- User can use expired tokens to login: use refresh tokens
    - use Redis to store JTI blacklist
- Users can use other's articles: 
    - use `@PreAuthorize(@articleSecurity.isOwner(authentication, #id)` to check ownership
    - Admin user can access all articles by `@PreAuthorize("hasRole('ADMIN')")`
- Use blog-cache Redis for article caching to reduce DB load
    - use `@Cacheable(value = "articles", key = "#id")` to cache article by id
    - use `@CacheEvict(value = "articles", key = "#id")` to evict cache on update/delete
- Rate limiting on login endpoint to prevent brute-force attacks
    - use in-memory rate limiter with ConcurrentHashMap
- Password hashing with BCrypt before storing in DB
    - use `BCryptPasswordEncoder` from Spring Security
- Password complexity validation on registration
    - use regex to enforce minimum length, uppercase, lowercase, digit, special character
    ```
     * - At least 8 characters long
     * - At least one uppercase letter (A-Z)
     * - At least one lowercase letter (a-z)
     * - At least one digit (0-9)
     * - At least one special character (@#$%^&+=!)
     * - No whitespace
    ```
- Validations on requests parameters and request bodies by `@Valid`, `@Email`, `@Pattern(regexp = "^(draft|published)$"` etc..


