#!/bin/bash
set -e

echo "Setting up ApplicationInsights-Java development environment..."

# Set up Gradle with proper JVM options
export GRADLE_OPTS="-Dorg.gradle.daemon=true -Dorg.gradle.parallel=true -Dorg.gradle.caching=true -XX:MaxMetaspaceSize=512m"

# Pre-download Gradle wrapper and dependencies
echo "Pre-downloading Gradle wrapper and dependencies..."
./gradlew --version

# Pre-compile buildSrc to avoid recompilation during regular builds
echo "Pre-compiling buildSrc..."
./gradlew :buildSrc:build --quiet || true

# Download and cache dependencies for faster builds
echo "Downloading and caching build dependencies..."
./gradlew dependencies --quiet || true

# Pre-download main build dependencies for common modules
echo "Pre-downloading dependencies for key modules..."
./gradlew :agent:agent:dependencies --quiet || true
./gradlew :agent:agent-tooling:dependencies --quiet || true
./gradlew :agent:agent-bootstrap:dependencies --quiet || true

echo "Development environment setup complete!"