# Devcontainer Setup for Application Insights Java

This devcontainer configuration is designed to optimize the development environment for GitHub Copilot agents by pre-installing tools and dependencies to reduce build time from over 5 minutes to under 1 minute.

## What's Included

- **Java 17 (Temurin)**: Pre-installed Java Development Kit
- **Gradle Wrapper**: Uses the project's gradle wrapper (gradlew) for build consistency
- **Docker in Docker**: For containerized smoke tests
- **VS Code Extensions**: Java development extensions

## Setup Process

The `setup-devcontainer.sh` script performs the following optimizations:

1. **Pre-compiles buildSrc**: Avoids recompilation during regular builds
2. **Downloads Gradle wrapper**: Ensures Gradle is immediately available
3. **Caches dependencies**: Pre-downloads common dependencies for faster builds
4. **Optimizes JVM settings**: Configures Gradle with appropriate memory settings

## Usage

This devcontainer is automatically used by GitHub Copilot agents when working on this repository. The configuration is located at `.devcontainer/devcontainer.json` following VS Code devcontainer standards.

## Validation

Run `.devcontainer/validate-devcontainer.sh` to verify the setup is working correctly.