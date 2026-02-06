# syntax=docker/dockerfile:1

### Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy Maven wrapper + pom first for better layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (cacheable layer)
RUN ./mvnw -q -DskipTests dependency:go-offline

# Copy sources
COPY src/ src/

# Build
RUN ./mvnw -q -DskipTests package


### Runtime stage
FROM eclipse-temurin:21-jre-jammy

# Optional but handy: curl for healthchecks
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*

# Run as non-root
RUN useradd -m -u 10001 appuser
USER appuser

WORKDIR /app

COPY --from=build /app/target/*.jar /app/app.jar

# Default storage location inside the container
ENV APP_BUNDLE_STORAGE_BASE_PATH=/data/i18n \
    SERVER_PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=10 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]
