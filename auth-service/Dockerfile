# Build stage
FROM maven:3.9.4-eclipse-temurin-21 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Environment variables
ENV SERVER_PORT=8081
ENV DB_HOST=auth-db
ENV DB_PORT=5432
ENV DB_NAME=authdb
ENV DB_USERNAME=postgres
ENV DB_PASSWORD=password
ENV JWT_SECRET=NTZhNDcyZTVkNDJhNDc2OTJkNzA3MjY4NzE1NjZkNDk0ZWIyNTQxNzYxNDc0NTZjNzA3MjY4NzE1NjZkNDk1OA==
ENV JWT_EXPIRATION=86400000
ENV SPRING_PROFILES_ACTIVE=dev
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://auth-db:5432/auth-db
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=password

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
