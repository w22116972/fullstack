# Shared PostgreSQL Database
FROM postgres:16-alpine

ENV PGDATA=/var/lib/postgresql/data
ENV POSTGRES_USER=postgres
ENV POSTGRES_PASSWORD=password
ENV POSTGRES_DB=appdb

# Copy init script
COPY init.sql /docker-entrypoint-initdb.d/

HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
    CMD pg_isready -U postgres -d appdb

EXPOSE 5432
