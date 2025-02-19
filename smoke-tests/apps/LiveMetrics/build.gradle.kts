plugins {
  id("ai.smoke-test-war")
}

dependencies {
  smokeTestImplementation("org.awaitility:awaitility:4.2.0")
  implementation("log4j:log4j:1.2.17")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:1.22.1")
  implementation("com.azure:azure-json:1.0.0")
  implementation("com.azure:azure-monitor-opentelemetry-autoconfigure:1.0.0-beta.1")
}
