# Build stage
FROM maven:3.9.9-amazoncorretto-21 AS build
WORKDIR /app
# Copy pom.xml first to leverage Docker cache for dependencies
COPY pom.xml .
# Download dependencies (cached as long as pom.xml doesn't change)
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM amazoncorretto:21-alpine
# Create a non-root user for security (Best Practice)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Environment variables
ENV SERVER_PORT=8082
ENV DB_HOST=blog-db
ENV DB_PORT=5432
ENV DB_NAME=blog-db
ENV DB_USERNAME=postgres
ENV DB_PASSWORD=password
ENV JWT_SECRET=NTZhNDcyZTVkNDJhNDc2OTJkNzA3MjY4NzE1NjZkNDk0ZWIyNTQxNzYxNDc0NTZjNzA3MjY4NzE1NjZkNDk1OA==
ENV JWT_EXPIRATION=86400000
ENV SPRING_PROFILES_ACTIVE=dev
ENV SPRING_REDIS_HOST=blog-cache
ENV SPRING_REDIS_PORT=6379
ENV SPRING_REDIS_TIMEOUT=60000ms
ENV SPRING_REDIS_JEDIS_POOL_MAX_ACTIVE=8
ENV SPRING_REDIS_JEDIS_POOL_MAX_IDLE=8
ENV SPRING_REDIS_JEDIS_POOL_MIN_IDLE=0
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://blog-db:5432/blog-db
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=password

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
