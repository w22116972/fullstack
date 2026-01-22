# Auth Cache Service (Redis)

Dedicated Redis instance for the auth-service microservice. This service stores JWT token blacklist entries to implement secure logout functionality.

## Features

- **Token Blacklist Storage**: Maintains a list of revoked JWT tokens
- **Automatic Expiration**: Token entries automatically expire after their JWT expiration time
- **LRU Eviction**: Implements Least Recently Used eviction policy with 256MB memory limit
- **Health Checks**: Docker health checks to ensure Redis availability
- **No Persistence**: Cache-only mode (no disk persistence for faster performance)

## Environment Variables

Configure the following environment variables when running the container:

| Variable | Default | Description |
|----------|---------|-------------|
| `REDIS_PORT` | `6379` | Redis server port |
| `REDIS_PASSWORD` | (empty) | Password for Redis authentication |

## Usage

### Docker Compose

```yaml
auth-cache:
  build: ./auth-cache
  container_name: auth-cache
  ports:
    - "6379:6379"
  environment:
    REDIS_PORT: 6379
    REDIS_PASSWORD: your-secure-password
  networks:
    - microservices
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 10s
    timeout: 3s
    retries: 3
```

### Standalone

```bash
docker build -t auth-cache:latest ./auth-cache
docker run -d \
  --name auth-cache \
  -p 6379:6379 \
  -e REDIS_PASSWORD=your-secure-password \
  auth-cache:latest
```

## Connection Details

- **Host**: `auth-cache` (or `localhost` if running locally)
- **Port**: `6379` (configurable via `REDIS_PORT`)
- **Password**: Set via `REDIS_PASSWORD` environment variable

## Configuration

The `redis.conf` file includes:

- **Memory Management**: 256MB limit with LRU eviction
- **Security**: Password authentication support
- **Performance**: No persistence (cache-only)
- **Logging**: Standard Redis logging

## Integration with Auth Service

The auth-service connects to this Redis instance to:

1. **Store blacklisted tokens**: When a user logs out, their JWT token is added to Redis
2. **Validate tokens**: During each API request, auth-service checks if the token exists in the blacklist
3. **Auto-expiration**: Redis entries are automatically removed after token expiration time

### Configuration in Auth Service

Set these environment variables in the auth-service:

```env
REDIS_HOST=auth-cache
REDIS_PORT=6379
REDIS_PASSWORD=your-secure-password
```

## Monitoring

Check Redis status:

```bash
docker exec auth-cache redis-cli ping
docker exec auth-cache redis-cli INFO
docker exec auth-cache redis-cli DBSIZE
```

Monitor blacklisted tokens:

```bash
docker exec auth-cache redis-cli KEYS "blacklist:*"
docker exec auth-cache redis-cli SCAN 0 MATCH "blacklist:*" COUNT 10
```

## Best Practices

1. **Use Strong Password**: Always set a strong `REDIS_PASSWORD` in production
2. **Network Security**: Run auth-cache on an internal network, not exposed to the internet
3. **Memory Limits**: Monitor memory usage; adjust `maxmemory` if needed
4. **Backup Strategy**: For production, consider adding persistence or regular backups
5. **Monitoring**: Implement Redis monitoring to track usage patterns

## Troubleshooting

### Connection Refused
- Ensure Redis is running and port 6379 is accessible
- Check firewall rules and Docker network configuration

### High Memory Usage
- Monitor token expiration; ensure tokens are being removed
- Check for connection leaks in auth-service
- Reduce `maxmemory` if needed

### Slow Performance
- Check Redis CPU usage and network latency
- Monitor network bandwidth between auth-service and auth-cache
- Use Redis `SLOWLOG` for performance analysis
