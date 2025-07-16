#!/bin/bash

# Script to build and install azure-monitor-opentelemetry-autoconfigure dependency
# from a specified GitHub repository and branch

set -e

echo "Building azure-monitor-opentelemetry-autoconfigure from $AZURE_MONITOR_OPENTELEMETRY_AUTOCONFIGURE"

# Parse repo and branch from format "owner/repo:branch"
REPO_BRANCH="$AZURE_MONITOR_OPENTELEMETRY_AUTOCONFIGURE"
REPO=$(echo "$REPO_BRANCH" | cut -d':' -f1)
BRANCH=$(echo "$REPO_BRANCH" | cut -d':' -f2)

echo "Repository: $REPO"
echo "Branch: $BRANCH"

# Clone the repository
echo "Cloning repository..."
git clone https://github.com/$REPO.git azure-sdk-temp
cd azure-sdk-temp
git checkout $BRANCH

# Build and install the azure-monitor-opentelemetry-autoconfigure module
echo "Building and installing azure-monitor-opentelemetry-autoconfigure..."
mvn clean install -DskipTests -pl sdk/monitor/azure-monitor-opentelemetry-autoconfigure -am

# Get the version that was just built and installed
echo "Determining installed version..."
INSTALLED_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout -pl sdk/monitor/azure-monitor-opentelemetry-autoconfigure)
echo "Installed version: $INSTALLED_VERSION"

# Clean up
echo "Cleaning up..."
cd ..
rm -rf azure-sdk-temp

# Update dependency versions in the current project
echo "Updating dependency versions in project files..."

# Update agent-tooling build.gradle.kts
if [ -f "agent/agent-tooling/build.gradle.kts" ]; then
    sed -i "s/com\.azure:azure-monitor-opentelemetry-autoconfigure:[^\"]\+/com.azure:azure-monitor-opentelemetry-autoconfigure:$INSTALLED_VERSION/g" agent/agent-tooling/build.gradle.kts
    echo "Updated agent/agent-tooling/build.gradle.kts"
fi

# Update smoke-tests framework build.gradle.kts
if [ -f "smoke-tests/framework/build.gradle.kts" ]; then
    sed -i "s/com\.azure:azure-monitor-opentelemetry-autoconfigure:[^\"]\+/com.azure:azure-monitor-opentelemetry-autoconfigure:$INSTALLED_VERSION/g" smoke-tests/framework/build.gradle.kts
    echo "Updated smoke-tests/framework/build.gradle.kts"
fi

./gradlew resolveAndLockAll --write-locks
./gradlew generateLicenseReport --no-build-cache

 # this is needed to make license report pass
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
git config user.name "github-actions[bot]"
git commit -a -m "update azure-monitor-opentelemetry-autoconfigure dependency to $INSTALLED_VERSION"

echo "azure-monitor-opentelemetry-autoconfigure dependency build completed successfully"
echo "All project files updated to use version: $INSTALLED_VERSION"
