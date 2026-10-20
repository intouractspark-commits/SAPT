# ============================================================
# SAPT Backend — Dockerfile
# Multi-stage build: Maven build → slim JRE runtime
# Compatible with Render (port 10000)
# ============================================================

# ---- Stage 1: Build ----
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/sapt-backend-1.0.0-SNAPSHOT.jar app.jar

# Render uses port 10000 by default
EXPOSE 10000

# Start the app — env vars injected by Render at runtime
ENTRYPOINT ["java", "-Xmx400m", "-jar", "app.jar"]
