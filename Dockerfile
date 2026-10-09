# syntax=docker/dockerfile:1
#
# Multi-stage production image for the Electrical Inventory & Stock
# Management System (Spring Boot 3.5.7, Java 17, Thymeleaf + MySQL).
#
# Stage 1 compiles and packages the application with the project's own
# Maven Wrapper; Stage 2 copies only the executable JAR into a slim JRE
# image.

# ---------------------------------------------------------------------------
# Stage 1: build
# ---------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy the Maven Wrapper and POM first so Maven's dependency resolution is
# cached across image builds (this layer only changes when the POM or the
# wrapper itself changes).
COPY mvnw ./
COPY .mvn ./.mvn
COPY pom.xml ./

RUN chmod +x ./mvnw && ./mvnw -B dependency:go-offline

# Copy the sources and package the executable JAR.
# Tests are NOT re-run inside the image build: the existing suite is executed
# separately (see ".\mvnw.cmd clean test") so that every deployment stays fast.
COPY src ./src
RUN ./mvnw -B clean package -DskipTests

# ---------------------------------------------------------------------------
# Stage 2: runtime
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Run the application as a non-root user.
RUN groupadd --system --gid 10001 appgroup \
    && useradd --system --uid 10001 --gid appgroup appuser

# The exact JAR produced by the POM (artifactId-version.jar).
COPY --from=builder /app/target/inventory-management-system-1.0.0.jar /app/app.jar

USER appuser

# The container listens on the platform-provided PORT (Render injects PORT).
# application.properties already maps ${PORT:8080} to server.port, so no
# extra wiring is required here. EXPOSE is documentation: Render overrides
# the published port with PORT.
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]