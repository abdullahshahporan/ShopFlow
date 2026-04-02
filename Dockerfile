# ===================================
# Stage 1: Build Stage
# ===================================
FROM maven:3.9-eclipse-temurin-17 AS builder

LABEL maintainer="Shah Poran <shah.poran@example.com>"
LABEL description="ShopFlow E-Commerce Platform - Build Stage"

WORKDIR /build

# Copy Maven files first for better layer caching
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Download dependencies (cached if pom.xml unchanged)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application (skip tests for faster builds)
RUN mvn clean package -DskipTests -B

# Verify JAR was created
RUN ls -lh /build/target/*.jar

# ===================================
# Stage 2: Production Runtime Stage
# ===================================
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Shah Poran <shah.poran@example.com>"
LABEL description="ShopFlow E-Commerce Platform - Production Runtime"
LABEL version="1.0.0"

# Install necessary tools and create non-root user
RUN apk add --no-cache \
    curl \
    bash \
    tzdata && \
    addgroup -g 1001 -S shopflow && \
    adduser -u 1001 -S shopflow -G shopflow

# Set timezone (change as needed)
ENV TZ=Asia/Dhaka

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder --chown=shopflow:shopflow /build/target/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && chown -R shopflow:shopflow /app

# Switch to non-root user for security
USER shopflow

# Expose application port
EXPOSE 8080

# Health check (checks if app is responding)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM Options for Production
ENV JAVA_OPTS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heap-dump.hprof \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=prod \
    -Dserver.port=8080 \
    "

# Application startup command
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]


