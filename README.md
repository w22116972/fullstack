# Blog Admin System

A full-stack blog administration system with microservices architecture.

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
| auth-service | 8081 | Authentication and user management |
| blog-service | 8082 | Article CRUD operations |
| database | 5432 | Shared PostgreSQL database (appdb) |
| blog-cache | 6379 | Redis cache for article data |

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
- `POST /auth/login` - User login, returns JWT token
- `POST /auth/register` - User registration
- `POST /auth/validate` - Token validation

**Articles (blog-service:8082)**
- `GET /articles` - List articles (paginated)
- `GET /articles/{id}` - Get article by ID
- `POST /articles` - Create article
- `PUT /articles/{id}` - Update article
- `DELETE /articles/{id}` - Delete article

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
