# Blog Service

Blog microservice providing article CRUD operations with caching and role-based access control.

## Technology Stack

- Java 21
- Spring Boot 3.4.2
- Spring Security
- Spring Data JPA
- Spring Cache with Redis
- PostgreSQL 16
- Redis

## API Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | /articles | List articles (paginated), validates token JTI | Yes |
| GET | /articles/{id} | Get article by ID, validates token JTI | Yes |
| POST | /articles | Create article, validates token JTI | Yes |
| PUT | /articles/{id} | Update article, validates token JTI | Yes (owner/admin) |
| DELETE | /articles/{id} | Delete article, validates token JTI | Yes (owner/admin) |

### Query Parameters for GET /articles

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number (0-indexed) |
| size | int | 10 | Page size (max 100) |
| title | string | null | Filter by title (case-insensitive) |

### Request/Response Examples

**List Articles**
```bash
curl -X GET "http://localhost:8082/articles?page=0&size=10" \
  -H "Authorization: Bearer <token>"
```

**Create Article**
```bash
curl -X POST http://localhost:8082/articles \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "My Article",
    "content": "Article content here",
    "tags": "java,spring",
    "publishStatus": "DRAFT"
  }'
```

## Build and Run

### With Docker (Recommended)

```bash
# From project root
docker-compose up --build blog-service blog-db blog-cache
```

### Local Development

Requires PostgreSQL and Redis running locally.

```bash
# Build
./mvnw clean package -DskipTests

# Run
./mvnw spring-boot:run

# Run without database/redis (lazy mode)
LAZY_INIT=true DDL_AUTO=none CACHE_TYPE=none ./mvnw spring-boot:run
```

### Run Tests

```bash
./mvnw test
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| SERVER_PORT | 8082 | Server port |
| DB_HOST | localhost | Database host |
| DB_PORT | 5432 | Database port |
| DB_NAME | blogdb | Database name |
| DB_USERNAME | postgres | Database username |
| DB_PASSWORD | password | Database password |
| JWT_SECRET | (default key) | JWT signing secret (must match auth-service) |
| SPRING_REDIS_HOST | auth-cache | **Redis host for token JTI validation** (connect to auth-cache, not blog-cache) |
| SPRING_REDIS_PORT | 6379 | Redis port for token validation |
| SPRING_REDIS_PASSWORD | | Redis password for token validation |
| CACHE_TYPE | redis | Cache type for articles (redis/none) |
| RATE_LIMIT_API | 100 | Max API requests per window |
| RATE_LIMIT_API_WINDOW | 60 | Rate limit window in seconds |
| LAZY_INIT | false | Enable lazy initialization |
| DDL_AUTO | update | JPA schema generation mode |

## API Documentation

Swagger UI available at: http://localhost:8082/swagger-ui.html

## Project Structure

```
src/main/java/com/example/blog/
├── BlogApplication.java        # Application entry point
├── config/
│   ├── SecurityConfig.java     # Spring Security configuration
│   ├── RedisConfig.java        # Cache configuration
│   ├── RateLimitFilter.java    # API rate limiting
│   └── CacheKeyConfig.java     # Cache key generation
├── controller/
│   └── ArticleController.java  # REST endpoints
├── dto/
│   ├── ArticleDto.java
│   └── ArticleSummaryDto.java
├── exception/
│   └── ResourceNotFoundException.java
├── mapper/
│   └── ArticleMapper.java      # Entity to DTO mapping
├── model/
│   ├── Article.java            # JPA entity
│   ├── User.java               # JPA entity
│   └── Role.java               # USER, ADMIN enum
├── repository/
│   ├── ArticleRepository.java
│   └── UserRepository.java
├── security/
│   ├── JwtAuthFilter.java      # JWT authentication filter
│   ├── JwtUtil.java            # JWT validation
│   └── ArticleSecurity.java    # Ownership checks
└── service/
    └── ArticleService.java     # Business logic with caching
```

## Security Features

- **JWT Token Authentication**: Token validation with signature verification and JTI blacklist checking
- **JTI Blacklist Validation**: Every request checks if token's JTI is blacklisted in auth-cache (ensures logged-out tokens are rejected)
- **TokenValidationFilter**: Early-stage filter checks JTI blacklist before processing requests
- **Role-Based Access Control (RBAC)**: USER and ADMIN roles with different permissions
- **Ownership-Based Authorization**: Users can only modify their own articles unless they are admins
- **Rate Limiting**: API rate limiting via Redis integration
- **Input Validation**: Article data validation on create/update

## Caching

Articles are cached in Redis (blog-cache) with the following behavior:
- `@Cacheable` on getArticle() - caches individual articles
- `@CacheEvict` on deleteArticle() - removes from cache on delete
- Cache TTL: 1 hour (configurable)

Additionally, the service validates tokens against the **auth-cache** Redis instance:
- Checks if token's JTI is in the blacklist (via TokenValidationFilter)
- Ensures logged-out tokens are immediately rejected across all services
- Redis connection to auth-cache is shared with auth-service for distributed token state

Cache can be disabled by setting `CACHE_TYPE=none`.

## Database Schema

The schema is auto-generated by JPA. Main tables:

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE articles (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    tags VARCHAR(500),
    publish_status VARCHAR(50) DEFAULT 'DRAFT',
    author_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

## Authorization Rules

| Role | List | Read | Create | Update | Delete |
|------|------|------|--------|--------|--------|
| USER | Own only | Own only | Yes | Own only | Own only |
| ADMIN | All | All | Yes | All | All |
