# Redis container for auth-service token blacklist
FROM redis:7-alpine

# Set working directory
WORKDIR /usr/local/etc/redis

# Copy custom redis configuration
COPY redis.conf .

# Expose Redis port
EXPOSE 6379

# Health check
HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
    CMD redis-cli ping || exit 1

# Run Redis with the custom configuration
CMD ["redis-server", "/usr/local/etc/redis/redis.conf"]
