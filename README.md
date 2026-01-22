
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
├── blog-cache/            # Redis Docker config
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


### Security Features

- **JWT with JTI Claims**: Every token includes a unique JWT ID (JTI) for secure revocation
- **Token Blacklisting**: Logged-out tokens are immediately rejected across all services via JTI blacklist in auth-cache
- **Distributed Token Validation**: Both auth-service and blog-service check JTI blacklist for security
- **Refresh Tokens**: 7-day refresh tokens enable long sessions with short-lived access tokens
- **Refresh Token Lifecycle**: When an access token expires (after 10 hours), the client can use a refresh token (valid for 7 days) to obtain a new access token without re-authenticating. Refresh tokens are stored in Redis (auth-cache) and are revoked on logout or after expiration. This enables secure, seamless session extension while allowing immediate revocation on logout.
- **Memory-Efficient**: JTI-based approach uses only 36 bytes per token vs 500+ bytes with full-token storage

Nginx in the frontend container acts as a reverse proxy:
- `/api/auth/*` routes to auth-service
- `/api/articles/*` routes to blog-service

## Quick Start

### Prerequisites
- Docker
- Docker Compose

### Run with Docker Compose

```bash
docker-compose up --build
```

Access the application at http://localhost:8080

### Default Test Account

After first startup, the system automatically creates an admin account:

- **Email**: `admin@example.com`
- **Password**: `password123`

You can customize these credentials using environment variables before first run:

```bash
ADMIN_EMAIL=your@email.com ADMIN_PASSWORD=yourpassword docker-compose up --build
```

**Note**: Admin credentials are only set during initial database creation. To change them after first run, you need to:
1. Stop all services: `docker-compose down -v` (⚠️ This deletes all data)
2. Start with new credentials: `ADMIN_EMAIL=new@email.com ADMIN_PASSWORD=newpass docker-compose up --build`

### Environment Variables

Admin account credentials can be configured via environment variables:

```bash
ADMIN_EMAIL=admin@example.com ADMIN_PASSWORD=password123 docker-compose up --build
```

### Run Services Individually

Each service can run standalone for development:

```bash
# auth-service (without database)
cd auth-service
LAZY_INIT=true DDL_AUTO=none ./mvnw spring-boot:run

# blog-service (without database/redis)
cd blog-service
LAZY_INIT=true DDL_AUTO=none CACHE_TYPE=none ./mvnw spring-boot:run
```

## Build Commands

### Backend

```bash
cd auth-service
./mvnw clean package -DskipTests    # Build JAR
./mvnw test                          # Run tests

cd blog-service
./mvnw clean package -DskipTests    # Build JAR
./mvnw test                          # Run tests
```

### Frontend

```bash
cd frontend
npm install                          # Install dependencies
npm start                            # Development server (port 3000)
npm test                             # Run tests
npm run build                        # Production build
```

## API Documentation

Swagger UI is available at: http://localhost:8082/swagger-ui.html

### API Endpoints


**Authentication (auth-service:8081)**
- `POST /auth/login` - User login, returns JWT access token (with JTI) and refresh token
- `POST /auth/register` - User registration
- `POST /auth/validate` - Token validation
- `POST /auth/logout` - Logout, blacklist access token JTI, and revoke refresh token
- `POST /auth/refresh` - Exchange refresh token for new access token (rotates refresh token)
## Refresh Token Lifecycle

The system uses a dual-token approach for secure, scalable authentication:

- **Access Token**: Short-lived (10 hours), used for API requests. Contains JTI claim for revocation.
- **Refresh Token**: Long-lived (7 days), used to obtain new access tokens without re-authenticating.

**Lifecycle Steps:**

1. **Login:**
	- User logs in via `/auth/login`.
	- Receives both access and refresh tokens.
	- Refresh token is stored in Redis (keyed by user email, TTL 7 days).

2. **Access Token Expiry:**
	- When the access token expires, client receives 401 Unauthorized.
	- Client sends refresh token to `/auth/refresh`.
	- If valid and not revoked, a new access token (and optionally a new refresh token) is issued.

3. **Logout:**
	- User logs out via `/auth/logout`.
	- Access token JTI is blacklisted in Redis (TTL matches token expiry).
	- Refresh token is deleted from Redis, immediately invalidating further refresh attempts.

4. **Refresh Token Expiry:**
	- After 7 days, refresh token expires in Redis and cannot be used.
	- User must re-authenticate via `/auth/login`.

**Security Notes:**
- Both access and refresh tokens are JWTs, cryptographically signed.
- Refresh tokens are never exposed to blog-service; only auth-service handles them.
- Blacklisting is enforced via JTI for access tokens and Redis key deletion for refresh tokens.

See `authorization-architecture.md` for a detailed diagram and flow.

**Articles (blog-service:8082)**
- `GET /articles` - List articles (paginated), validates token JTI
- `GET /articles/{id}` - Get article by ID, validates token JTI
- `POST /articles` - Create article, validates token JTI
- `PUT /articles/{id}` - Update article, validates token JTI
- `DELETE /articles/{id}` - Delete article, validates token JTI

## Test Account

| Field | Value |
|-------|-------|
| Email | admin@example.com |
| Password | password123 |

These defaults can be changed via `ADMIN_EMAIL` and `ADMIN_PASSWORD` environment variables.

## Design Decisions

### Microservices Architecture
The system is split into separate services (auth, blog) to allow independent scaling and deployment. Each service has its own database following the database-per-service pattern.

### JWT Authentication
Stateless JWT tokens are used for authentication, eliminating server-side session storage and enabling horizontal scaling. Tokens are validated independently by each service using a shared secret.

### Nginx as API Gateway
Instead of a dedicated API gateway service, Nginx serves both static files and proxies API requests. This reduces complexity and resource usage for smaller deployments.

### JPA Schema Management
Database schemas are managed by JPA with `ddl-auto=update`, simplifying development and eliminating the need for migration tools in this context.

### In-Memory Rate Limiting
Simple in-memory rate limiting is implemented for login endpoints to prevent brute-force attacks. For distributed deployments, this could be extended to use Redis.

### Lazy Initialization
Services support lazy initialization mode, allowing them to start without database connections for testing and development purposes.

## Challenges and Solutions

### Service Independence
Challenge: Services should be able to run independently without failing due to missing dependencies.
Solution: Implemented lazy initialization, configurable cache types, and graceful degradation when Redis or databases are unavailable.

### CORS and API Routing
Challenge: Frontend needs to communicate with multiple backend services.
Solution: Nginx reverse proxy handles routing, eliminating CORS issues by serving everything from a single origin.

### Database Initialization
Challenge: Ensuring database schema and admin account exist on first startup.
Solution: JPA auto-creates schema, and AdminInitializer component creates the admin account on startup with configurable credentials.

### Rate Limiting Without External Dependencies
Challenge: Protect login endpoints from brute-force attacks without adding Redis dependency to auth-service.
Solution: Implemented simple in-memory rate limiter using ConcurrentHashMap with sliding window algorithm.



## Future Improvements

To further enhance scalability, security, maintainability, and user experience, consider the following improvements:

### Infrastructure & Deployment

- **Separate Databases for Each Service**
	- Use dedicated databases for `auth-service` and `blog-service` (e.g., `auth-db` and `blog-db`) to enforce true service isolation, improve security, and enable independent scaling and backup strategies per service.

- **External Distributed Cache for Rate Limiting**
	- Move rate limit tracking from in-memory to an external cache like Redis. This enables consistent rate limiting across multiple instances, supports horizontal scaling, and prevents bypassing limits after service restarts.

- **API Gateway Service**
	- Introduce a dedicated API gateway (e.g., Spring Cloud Gateway, Kong, or Nginx with Lua) to centralize authentication, rate limiting, logging, request validation, and routing. This decouples cross-cutting concerns from backend services and simplifies frontend integration.

- **Service Discovery and Load Balancing**
	- Integrate service discovery (e.g., Eureka, Consul) and load balancing to support dynamic scaling and resilience in distributed environments.

- **Container Orchestration**
	- Deploy with Kubernetes or similar platforms for automated scaling, self-healing, rolling updates, and health checks.

- **Database Migration Tools**
	- Adopt migration tools (e.g., Flyway, Liquibase) for versioned, repeatable schema changes, especially for production deployments and safer rollback capabilities.

### Security Enhancements

- **Enhanced Authentication & Authorization**
	- Implement multi-factor authentication (MFA) with TOTP or email verification.
	- Add password policies (complexity, history, expiration) and account lockout mechanisms after failed login attempts.
	- Support OAuth 2.0 / SSO (Google, GitHub, Azure AD) for easier user onboarding.
	- Implement email verification for user registration to reduce spam accounts.

- **Centralized Configuration & Secrets Management**
	- Use tools like Spring Cloud Config, HashiCorp Vault, or AWS Secrets Manager to manage configuration and secrets centrally, improving security and simplifying environment management.

- **Advanced Rate Limiting**
	- Extend rate limiting beyond IP-based to include per-user, per-endpoint, and adaptive rate limiting based on behavior patterns.
	- Implement exponential backoff for failed authentication attempts.

- **Security Audit & Compliance**
	- Add request/response auditing and logging for compliance requirements (PCI-DSS, GDPR).
	- Implement API request signing for sensitive operations.
	- Add CORS policy enforcement and CSRF protection enhancements.
	- Use HTTPS everywhere, including internal service communication.

### Monitoring, Logging & Observability

- **Centralized Logging & Monitoring**
	- Add centralized logging (e.g., ELK stack, Datadog, or CloudWatch) to aggregate logs from all services.
	- Set up distributed tracing (e.g., OpenTelemetry, Jaeger) to track requests across services.
	- Collect metrics (e.g., Prometheus, Grafana) for performance monitoring and alerting.

- **Application Performance Monitoring (APM)**
	- Integrate APM tools (e.g., New Relic, Datadog, Elastic APM) to monitor application health, performance bottlenecks, and error rates.

- **Health Checks & Readiness Probes**
	- Implement comprehensive health check endpoints for database, cache, and external dependencies.


### Data Management & Scalability

- **Rich Blog Search Features**
	- Expand blog search beyond title-only to support full-text search on article content, tags, author, and date.
	- Integrate search engines like Elasticsearch for advanced filtering, faceting, and relevance ranking.
	- Add autocomplete, typo tolerance, and suggestions for improved user experience.

- **Advanced Caching Strategy**
	- Implement cache warming strategies to preload frequently accessed data on startup.
	- Add cache invalidation strategies (TTL, event-based) for consistency between services.
	- Consider multi-level caching (local + distributed) for optimal performance.

- **Full-Text Search & Indexing**
	- Integrate Elasticsearch or similar for full-text search on articles, enabling advanced search, filtering, and faceting.

- **Data Backup & Disaster Recovery**
	- Implement automated backups with versioning for databases and persistent data.
	- Set up tested recovery procedures and failover mechanisms.
	- Consider point-in-time recovery for critical data.

- **Content Versioning & Audit Trail**
	- Add content versioning for articles to track changes and enable rollback.
	- Implement audit trails logging who created/modified/deleted content and when.

### Feature Enhancements

- **Blog Content Features**
	- Add article drafts with publish scheduling capability.
	- Implement tags, categories, and hierarchical organization for better content discovery.
	- Support media/file uploads and rich content editor (e.g., Markdown, WYSIWYG).
	- Add article comments and discussion threads for community engagement.

- **User Experience**
	- Implement pagination standards and cursor-based pagination for large datasets.
	- Add infinite scroll or virtual scrolling for better performance on large lists.
	- Support dark mode and theme customization for improved accessibility.
	- Add internationalization (i18n) for multi-language support.
	- Implement loading states, skeletons, and proper error boundaries throughout the frontend.

- **Real-Time Features**
	- Add WebSocket support for real-time notifications (e.g., article published, comments added).
	- Implement Server-Sent Events (SSE) as an alternative for pushing updates to clients.

### API & Integration

- **API Enhancements**
	- Introduce API versioning to support backward compatibility and smoother API evolution.
	- Provide comprehensive API documentation with Swagger/OpenAPI, including code examples.
	- Implement standardized request/response pagination and sorting parameters.
	- Add GraphQL as an alternative to REST API for flexible querying.
	- Consider gRPC for internal service-to-service communication for better performance.

- **Webhook Support**
	- Add webhook support to allow external systems to subscribe to events (article created, user registered, etc.).
	- Implement webhook retry logic and delivery guarantees.

- **Async Job Processing**
	- Integrate a message queue (e.g., RabbitMQ, Apache Kafka) for asynchronous processing of long-running tasks.
	- Implement background jobs for email notifications, report generation, and batch operations.

### Testing & Quality

- **Comprehensive Testing**
	- Expand test coverage with unit tests, integration tests, and end-to-end (E2E) tests.
	- Add contract testing between services for better integration reliability.
	- Implement performance and load testing to identify bottlenecks.

- **Continuous Integration & Deployment (CI/CD)**
	- Set up automated CI/CD pipelines (GitHub Actions, GitLab CI, Jenkins) for building, testing, and deploying.
	- Implement automated code quality checks (SonarQube, CodeClimate) and security scanning (SAST, DAST).
	- Add automated deployment approval workflows and canary releases.

### Advanced Features

- **Feature Flags & A/B Testing**
	- Implement feature flags (e.g., LaunchDarkly, Unleash) to control feature rollouts and A/B testing.

- **Analytics & Usage Insights**
	- Add analytics to track user behavior, article engagement, and usage patterns.
	- Implement custom dashboards for admin insights.

- **Email Notifications**
	- Add email service (e.g., SendGrid, AWS SES) for user notifications, password resets, and alerts.

- **Resilience & Fault Tolerance**
	- Add circuit breakers, retries with exponential backoff, and fallback mechanisms for handling service failures.
	- Implement bulkheads to isolate critical resources and prevent cascading failures.

- **Data Privacy & GDPR Compliance**
	- Implement data export and deletion capabilities (right to be forgotten).
	- Add consent management for cookies and tracking.
	- Ensure PII (Personally Identifiable Information) is properly encrypted and handled.

### Performance Optimization

- **Frontend Optimizations**
	- Implement code splitting and lazy loading for faster initial page loads.
	- Add service workers and PWA features for offline support and faster repeated access.
	- Optimize bundle size using tree-shaking and minification.
	- Implement image optimization and CDN integration for faster asset delivery.

- **Backend Optimizations**
	- Add database query optimization and connection pooling.
	- Implement database indexing strategies for frequently queried fields.
	- Consider sharding strategies for handling large datasets.
