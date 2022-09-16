plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.1.7.RELEASE") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }
  implementation("io.opentelemetry:opentelemetry-api:1.0.0")
  // 1.4.0 is when @SpanAttribute annotation was introduced
  implementation("io.opentelemetry:opentelemetry-extension-annotations:1.4.0")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:1.16.0-alpha")
}

// need to use older versions to prevent regression
configurations.all {
  resolutionStrategy.force("io.opentelemetry:opentelemetry-api:1.0.0")
  resolutionStrategy.force("io.opentelemetry:opentelemetry-extension-annotations:1.4.0")
  resolutionStrategy.force("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:1.16.0-alpha")
}
