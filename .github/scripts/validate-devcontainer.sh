#!/bin/bash
# Validation script to verify devcontainer setup is working

set -e

echo "Validating devcontainer setup..."

# Check Java version
echo "Java version:"
java -version

# Check Gradle version
echo -e "\nGradle version:"
./gradlew --version

# Test a simple Gradle command
echo -e "\nTesting Gradle daemon is running..."
./gradlew help --quiet

# Test that buildSrc is compiled
echo -e "\nTesting buildSrc is compiled..."
./gradlew :buildSrc:tasks --quiet > /dev/null

# Test a simple module build
echo -e "\nTesting agent-bootstrap build..."
./gradlew :agent:agent-bootstrap:build -x test --quiet

echo -e "\nDevcontainer setup validation complete!"