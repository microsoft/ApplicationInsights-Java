// mostly copied from otel.javaagent-testing.gradle.kts

plugins {
  id("io.opentelemetry.instrumentation.javaagent-testing")

  id("ai.java-conventions")
}

evaluationDependsOn(":agent:agent-for-testing")

dependencies {
  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service")

  testImplementation("org.testcontainers:testcontainers")
}

configurations.configureEach {
  if (name.endsWith("testruntimeclasspath", ignoreCase = true)) {
    // Added by agent, don't let Gradle bring it in when running tests.
    exclude(module = "javaagent-bootstrap")
  }
}
