# Multi-stage build for TradeVisor application

# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-jammy AS builder

# Set the working directory
WORKDIR /app

# Copy gradle wrapper and build files first (for better layer caching)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Make the gradle wrapper executable
RUN chmod +x ./gradlew

# Copy the source code
COPY src src

# Build the application (skip tests for faster builds)
RUN ./gradlew build -x test

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-jammy

# Создаем директорию для логов и назначаем права
RUN mkdir -p /app/logs && \
    chmod 777 /app/logs

# Set the working directory
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
