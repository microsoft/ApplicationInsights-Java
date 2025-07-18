# Use the same base image as the original devcontainer
FROM mcr.microsoft.com/vscode/devcontainers/base:bullseye

# Install Microsoft OpenJDK 17
RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get -y install --no-install-recommends \
        wget \
        ca-certificates \
    && wget -q https://packages.microsoft.com/config/debian/11/packages-microsoft-prod.deb -O packages-microsoft-prod.deb \
    && dpkg -i packages-microsoft-prod.deb \
    && rm packages-microsoft-prod.deb \
    && apt-get update \
    && apt-get -y install --no-install-recommends \
        msopenjdk-17 \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Switch to vscode user for the build
USER vscode

# Set working directory for build
WORKDIR /tmp/build

# Copy all project files for the build
COPY --chown=vscode:vscode . ./

# Pre-populate Gradle dependency and build caches by compiling all classes
# Use --no-daemon to avoid leaving daemon processes running
# Set JAVA_HOME dynamically based on the actual installation
RUN export JAVA_HOME=$(find /usr/lib/jvm -name "msopenjdk-17-*" -type d | head -1) && \
    export PATH="$JAVA_HOME/bin:$PATH" && \
    ./gradlew classes --no-daemon

# Clean up the build directory but preserve gradle cache
RUN rm -rf /tmp/build

# Set default working directory for development
WORKDIR /workspaces
