plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.5.12") {
    exclude("spring-boot-starter-tomcat")
  }
  implementation("io.opentelemetry:opentelemetry-api:1.30.0")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:1.30.0")
  implementation("org.springframework.boot:spring-boot-test:2.7.16")
  implementation("io.spring.dependency-management:io.spring.dependency-management.gradle.plugin:1.1.3")

  // spring modules
  smokeTestImplementation("org.springframework.boot:spring-boot-starter-test:2.7.16")
  smokeTestImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
  smokeTestImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
  smokeTestImplementation("org.mock-server:mockserver-netty:5.15.0:shaded")
  smokeTestImplementation("org.awaitility:awaitility:4.2.0")
  smokeTestImplementation("io.opentelemetry.proto:opentelemetry-proto:0.14.0-alpha")
  smokeTestImplementation("org.assertj:assertj-core:3.24.2")
}
