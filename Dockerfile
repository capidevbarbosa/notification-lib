FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy Gradle wrapper and config files first (better layer caching)
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./

# Download dependencies (cached layer)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon 2>/dev/null || true

# Copy source code
COPY src/ src/

# Build the library (compile + test + package)
RUN ./gradlew clean build --no-daemon

# Run examples by default
CMD ["java", "-cp", "build/libs/notification-lib-1.0.0.jar", "com.notification.lib.examples.NotificationExamples"]
