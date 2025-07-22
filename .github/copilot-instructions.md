# Application Insights Java Agent - Development Guide

## Architecture Overview

This is a **Java agent** that extends OpenTelemetry Java Agent to provide Azure Application Insights telemetry. The agent is packaged as a single JAR that instruments applications at runtime without code changes.

### Key Components

- **Agent Entry Point**: `Agent.java` - wraps OpenTelemetry Agent with Application Insights-specific initialization
- **Agent Bootstrap**: Minimal classes loaded into bootstrap classloader for early initialization
- **Agent Tooling**: Main Application Insights logic (configuration, exporters, processors) isolated in agent classloader
- **Instrumentation Modules**: C Functions, ASP.NET Core interop, etc.
- **Classic SDK**: Legacy 2.x SDK maintained for compatibility

### Multi-Module Build Structure
ustom instrumentation for Azure
```
agent/
├── agent/              # Final agent JAR assembly (shadow plugin)
├── agent-bootstrap/    # Bootstrap classloader components
├── agent-tooling/      # Core agent logic & Azure exporters
├── instrumentation/    # Custom instrumentation modules
└── runtime-attach/     # Dynamic attach support
```

## Development Workflows

### Building the Agent

```bash
# Build complete agent JAR
./gradlew assemble

# Agent JAR location
# agent/agent/build/libs/applicationinsights-agent-<version>.jar
```

### Running Smoke Tests

Smoke tests use containerized applications with the agent attached.

Generally you shouldn't run all of the smoke tests, as they can take a long time.
Instead, focus on running a single test.

```bash
# Run a specific smoke test
./gradlew :smoke-tests:apps:HttpClients:smokeTest --tests "*HttpClientTest\$Tomcat8Java8Test"
```

### Code Formatting

The project uses Spotless for consistent code formatting. Apply formatting to all code:

```bash
# Apply formatting to all files
./gradlew spotlessApply
```

## Project-Specific Conventions

### Build Conventions (buildSrc/)

- **ai.java-conventions**: Base Java setup with JDK 17 toolchain, targets Java 8
- **ai.javaagent-instrumentation**: Plugin for OpenTelemetry instrumentation modules
- **ai.smoke-test-war**: WAR-based smoke test applications
- **ai.shadow-conventions**: JAR shadowing with relocation rules

### Agent JAR Assembly Process

The agent JAR is built in **3 critical steps** (see `agent/agent/build.gradle.kts`):

1. **Relocate** distro-specific libraries to avoid conflicts
2. **Isolate** classes to `inst/` directory with `.classdata` extensions
3. **Merge** with upstream OpenTelemetry agent, excluding duplicates

### Configuration Pattern

- Main config: `Configuration.java` - comprehensive JSON-based configuration
- Environment variables: `APPLICATIONINSIGHTS_CONNECTION_STRING`, etc.

### Smoke Test Pattern

- **Framework**: `smoke-tests/framework/` - shared test infrastructure
- **Apps**: `smoke-tests/apps/` - containerized test applications
- **Assertions**: `DependencyAssert`, `RequestAssert`, `MetricAssert` for validating telemetry
- **Fake Ingestion**: Mock Application Insights endpoint for testing
- **Environment Matrix**: Tests run across multiple environments (Java 8/11/17/21/23, Tomcat/Wildfly, HotSpot/OpenJ9)
- **Nested Test Classes**: Each abstract test class has nested static classes for different environments:
  ```java
  abstract class HttpClientTest {
    @Environment(TOMCAT_8_JAVA_8)
    static class Tomcat8Java8Test extends HttpClientTest {}

    @Environment(TOMCAT_8_JAVA_11)
    static class Tomcat8Java11Test extends HttpClientTest {}
  }
  ```

## Common Patterns

### Error Handling

Use `FriendlyException` for user-facing errors with actionable messages:

```java
throw new FriendlyException(
    "Connection string is required",
    "Please set APPLICATIONINSIGHTS_CONNECTION_STRING environment variable");
```

### Dependency Management

- All dependencies managed through `dependencyManagement/` module
- Strict version conflict resolution (`failOnVersionConflict()`)
- Dependency locking enabled for reproducible builds

### Testing Patterns

- **Unit Tests**: Standard JUnit 5 with 15-minute timeout
- **Smoke Tests**: Containerized integration tests with fake ingestion
- **Muzzle Tests**: Bytecode compatibility validation for instrumentation

## Key Files for Understanding

- `agent/agent/build.gradle.kts` - Agent assembly process
- `agent/agent-tooling/src/main/java/.../configuration/Configuration.java` - Main configuration
- `smoke-tests/framework/src/main/java/.../smoketest/` - Test infrastructure
- `buildSrc/src/main/kotlin/ai.*.gradle.kts` - Build conventions

## Development Tips

- Agent JAR must be self-contained (no external dependencies)
- Bootstrap classes are loaded early - keep minimal
- Use `hideFromDependabot()` for test-only dependencies
- Smoke tests validate end-to-end functionality in realistic environments
- Check `gradle.lockfile` when dependency issues arise
