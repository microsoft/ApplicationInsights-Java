#!/bin/bash
set -e

echo "Setting up ApplicationInsights-Java development environment..."

# Set up Gradle with proper JVM options
export GRADLE_OPTS="-Dorg.gradle.daemon=true -Dorg.gradle.parallel=true -Dorg.gradle.caching=true -XX:MaxMetaspaceSize=512m"

# Pre-download Gradle wrapper and dependencies
echo "Pre-downloading Gradle wrapper and dependencies..."
./gradlew --version

# Build project to download dependencies and populate build cache
echo "Building project to download dependencies and populate build cache..."
./gradlew build --quiet || true

echo "Development environment setup complete!"