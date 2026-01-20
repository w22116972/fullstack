# Blog Cache - Optimized for Article Caching
FROM redis:7-alpine

# Redis configuration environment variables
ENV REDIS_PORT=6379
ENV REDIS_MAXMEMORY=128mb
ENV REDIS_MAXMEMORY_POLICY=allkeys-lru

# Copy configuration file for blog cache (article caching with persistence)
COPY redis.conf /usr/local/etc/redis/redis.conf

EXPOSE 6379

VOLUME ["/data"]

CMD ["redis-server", "/usr/local/etc/redis/redis.conf"]
