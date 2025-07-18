# Use the same base image as the original devcontainer
FROM mcr.microsoft.com/vscode/devcontainers/base:bullseye

# Install Microsoft OpenJDK 17 and Docker
RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get -y install --no-install-recommends \
        wget \
        ca-certificates \
        curl \
        gnupg \
        lsb-release \
    && wget -q https://packages.microsoft.com/config/debian/11/packages-microsoft-prod.deb -O packages-microsoft-prod.deb \
    && dpkg -i packages-microsoft-prod.deb \
    && curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null \
    && apt-get update \
    && apt-get -y install --no-install-recommends \
        msopenjdk-17 \
        docker-ce \
        docker-ce-cli \
        containerd.io \
    && usermod -aG docker vscode `# Add vscode user to docker group for Docker access` \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && rm packages-microsoft-prod.deb

# Switch to vscode user for the build
USER vscode

# Set working directory for build
WORKDIR /tmp/build

# Copy all project files for the build
COPY --chown=vscode:vscode . ./

# Pre-populate Gradle dependency and build caches by compiling all classes
# Use --no-daemon to avoid leaving daemon processes running
RUN export JAVA_HOME=/usr/lib/jvm/msopenjdk-17-amd64 && \
    export PATH="$JAVA_HOME/bin:$PATH" && \
    ./gradlew classes --no-daemon

# Clean up the build directory but preserve gradle cache
RUN rm -rf /tmp/build

# Set default working directory for development
WORKDIR /workspaces
