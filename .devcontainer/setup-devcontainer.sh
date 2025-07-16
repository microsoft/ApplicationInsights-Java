#!/bin/bash
set -e

echo "Setting up ApplicationInsights-Java development environment..."

# GRADLE_OPTS is already set via containerEnv in devcontainer.json

# Pre-download Gradle wrapper and dependencies
echo "Pre-downloading Gradle wrapper and dependencies..."
./gradlew --version

# Build project to download dependencies and populate build cache
echo "Building project to download dependencies and populate build cache..."
./gradlew build --quiet || true

echo "Development environment setup complete!"