# Build stage
FROM node:20-alpine AS build
WORKDIR /app
# Copy package files first to leverage Docker cache for dependencies
COPY package.json package-lock.json ./
# Use npm install for resilience (with cache optimization)
RUN npm install
COPY . .
ARG REACT_APP_API_URL
ENV REACT_APP_API_URL=${REACT_APP_API_URL:-http://localhost:8080/api}
RUN npm run build

# Run stage - using unprivileged nginx for security
FROM nginx:alpine

# Create a non-root user (nginx runs as root by default, which is a security risk)
RUN addgroup -g 101 -S nginx || true && \
    adduser -S -D -H -u 101 -h /var/cache/nginx -s /sbin/nologin -G nginx -g nginx nginx || true

# Adjust nginx to run as non-root
RUN touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    chown -R nginx:nginx /etc/nginx/conf.d

# Clean default nginx static assets to prevent conflicts
RUN rm -rf /usr/share/nginx/html/*
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Set ownership of static files
RUN chown -R nginx:nginx /usr/share/nginx/html

# Switch to non-root user
USER nginx

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
