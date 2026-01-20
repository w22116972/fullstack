# Frontend

React single-page application for the blog administration system.

## Technology Stack

- React 19
- TypeScript
- React Router 7
- React Hook Form
- Axios
- Nginx (production serving and API proxy)

## Features

- User authentication (login/register)
- Article management (list, create, edit, delete)
- Pagination and search
- Role-based UI (admin sees all articles, users see own articles)
- Responsive design

## Pages

| Route | Component | Description |
|-------|-----------|-------------|
| /login | Login | User authentication |
| /register | Register | New user registration |
| /articles | ArticleList | Paginated article list |
| /articles/new | ArticleForm | Create new article |
| /articles/:id/edit | ArticleForm | Edit existing article |

## Build and Run

### With Docker (Recommended)

```bash
# From project root
docker-compose up --build frontend
```

Access at http://localhost:8080

### Local Development

```bash
# Install dependencies
npm install

# Start development server (port 3000)
npm start

# Run tests
npm test

# Run tests once (CI mode)
npm test -- --watchAll=false

# Production build
npm run build
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| REACT_APP_API_URL | /api | API base URL |

For local development with backend services:
```bash
REACT_APP_API_URL=http://localhost:8081 npm start
```

## Project Structure

```
src/
├── App.tsx                    # Main app with routing
├── index.tsx                  # Entry point
├── components/
│   ├── ErrorBoundary.tsx      # Error handling wrapper
│   ├── PrivateRoute.tsx       # Auth-protected route
│   └── Skeleton.tsx           # Loading placeholders
├── context/
│   └── AuthContext.tsx        # Authentication state
├── pages/
│   ├── Login.tsx              # Login form
│   ├── Register.tsx           # Registration form
│   ├── ArticleList.tsx        # Article listing
│   └── ArticleForm.tsx        # Create/edit article
├── services/
│   └── api.ts                 # Axios client with interceptors
├── types/
│   └── index.ts               # TypeScript interfaces
└── utils/
    └── errorHandler.ts        # Error message extraction
```

## API Integration

The frontend communicates with backend services through Nginx reverse proxy:

- `/api/auth/*` routes to auth-service
- `/api/articles/*` routes to blog-service

API client (`services/api.ts`) automatically:
- Attaches JWT token from cookies to requests
- Handles 401 responses by redirecting to login

## Authentication Flow

1. User submits login form
2. Auth service validates credentials and returns JWT
3. JWT stored in HTTP-only cookie (set by server)
4. AuthContext tracks authentication state
5. PrivateRoute component protects authenticated routes
6. API client includes credentials with each request

## Nginx Configuration

Production build is served by Nginx which also handles API proxying:

```nginx
server {
    listen 80;

    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }

    location /api/auth/ {
        proxy_pass http://auth-service:8081/auth/;
    }

    location /api/articles {
        proxy_pass http://blog-service:8082/articles;
    }
}
```

## Testing

```bash
# Run tests in watch mode
npm test

# Run tests once
npm test -- --watchAll=false

# Run specific test file
npm test -- Login.test.tsx
```

## Build Output

Production build creates optimized static files in `build/` directory:
- Minified JavaScript bundles
- Optimized CSS
- Static assets

The Docker image copies these to Nginx's html directory.

## Docker Build

The Dockerfile uses multi-stage build:

1. Node image builds the React app
2. Nginx image serves the static files

```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG REACT_APP_API_URL=/api
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
```
